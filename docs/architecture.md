# Architecture — Task Management API

## Request lifecycle

~~~mermaid
flowchart TD
    A["HTTP Request"] --> B["Spring Security Filter Chain"]
    B --> C{"Public endpoint?"}
    C -->|Yes| D["Auth Controller"]
    C -->|No| E["JWT Decoder / Validator"]
    E -->|Valid token| F["Task Controller"]
    E -->|Invalid or missing token| G["401 Unauthorized"]
    F --> H["Task Service"]
    H --> I["Current JWT Principal"]
    I --> J{"Role is ADMIN?"}
    J -->|Yes| K["All tasks"]
    J -->|No| L["Tasks for current user only"]
    K --> M["Task Repository"]
    L --> M
    M --> N[("PostgreSQL")]
    N --> M
    M --> F
~~~

## Authentication lifecycle

1. POST /api/v1/auth/register validates the request, normalizes the email and stores a BCrypt password hash. New accounts receive USER.
2. POST /api/v1/auth/login delegates credential verification to Spring Security's AuthenticationManager and DaoAuthenticationProvider.
3. On successful authentication, JwtService issues an HS256 JWT with issuer, subject (email), issue time, expiration and role claims.
4. The resource-server filter validates the token signature, issuer and time claims on every protected request.
5. The JWT authentication converter maps the role claim into a Spring Security authority such as ROLE_USER or ROLE_ADMIN.

## Task ownership and authorization

- Every newly created task receives its owner from the authenticated user's identity in the Spring Security context. A client cannot choose the owner via JSON.
- USER list queries include an owner ID predicate in the database query. The same owner check is applied to get, update, status update and delete operations.
- A task owned by another USER returns 404, not 403, so the API does not disclose whether another account's resource exists.
- ADMIN can list and manage tasks across accounts.
- Registration always assigns USER. There is no public API for assigning ADMIN; this role must be provisioned by a trusted operator.

## Legacy task migration

V3 adds a nullable owner_id foreign key. Existing task rows are deliberately left unassigned instead of being attributed to an arbitrary user. These rows are accessible only to ADMIN. Newly created tasks always have a non-null owner in application code. Before enforcing a database NOT NULL constraint in a later migration, an operator must explicitly reconcile legacy rows.

## Security boundaries

- The API is stateless; HTTP sessions, form login, HTTP Basic and CSRF are disabled.
- Register and login endpoints are public. Task endpoints require a valid Bearer access token.
- Passwords are stored only as BCrypt hashes using work factor 12.
- JWT signing material is read from JWT_SECRET_BASE64. The repository contains only a development fallback; deployment environments must provide a private random key of at least 32 decoded bytes.
- Access tokens last 15 minutes by default.
- Authorities are derived from the signed JWT and populated from the database during login.

## Components

- AuthController: exposes registration and login endpoints.
- AuthService: normalizes email, validates password byte length, hashes new passwords and authenticates login requests.
- CustomUserDetailsService: loads persisted users and maps their roles to Spring Security authorities.
- JwtService: creates short-lived access tokens.
- JwtConfig: loads the HMAC secret and configures JWT encoding and decoding.
- SecurityConfig: configures stateless authentication, password encoding, public routes and protected routes.
- TaskService: enforces ownership scoping and ADMIN cross-user privileges.
- Repositories: persist users and tasks with Spring Data JPA.
- Flyway: applies versioned database migrations.

## Database and integration testing

Flyway creates the task and user tables and adds task ownership. JPA validates entity mappings against the migrated schema. Integration tests use a disposable PostgreSQL 17 container through Testcontainers and exercise the authentication boundary, cross-user isolation and ADMIN privileges.

## Next milestones

Add projects, task assignment and comments. Then deploy the API and add monitoring and observability.
