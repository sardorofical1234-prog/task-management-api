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
    H --> I["Task Repository"]
    I --> J[("PostgreSQL")]
    J --> I
    I --> H
    H --> F
    F --> K["HTTP JSON Response"]
~~~

## Authentication lifecycle

1. POST /api/v1/auth/register validates the request, normalizes the email and stores a BCrypt password hash. New accounts receive the USER role.
2. POST /api/v1/auth/login delegates credential verification to Spring Security's AuthenticationManager and DaoAuthenticationProvider.
3. On successful authentication, JwtService issues an HS256 JWT with issuer, subject (email), issue time, expiration and role claims.
4. The resource-server filter validates the token signature, issuer and time claims on every protected request.
5. The JWT authentication converter maps the role claim into a Spring Security authority such as ROLE_USER.

## Security boundaries

- The API is stateless; HTTP sessions, form login, HTTP Basic and CSRF are disabled.
- Register and login endpoints are public. Task endpoints require a valid Bearer access token.
- Passwords are stored only as BCrypt hashes using work factor 12.
- JWT signing material is read from JWT_SECRET_BASE64. The repository contains only a development fallback; deployment environments must provide a private random key of at least 32 decoded bytes.
- Access tokens last 15 minutes by default.
- This milestone authenticates a user but does not yet scope tasks by owner or restrict endpoints by role. All authenticated users can currently access the shared task collection.

## Components

- AuthController: exposes registration and login endpoints.
- AuthService: normalizes email, validates password byte length, hashes new passwords and authenticates login requests.
- CustomUserDetailsService: loads persisted users and maps their roles to Spring Security authorities.
- JwtService: creates short-lived access tokens.
- JwtConfig: loads the HMAC secret and configures JWT encoding and decoding.
- SecurityConfig: configures stateless authentication, password encoding, public routes and protected routes.
- TaskController / TaskService: expose and implement task operations after authentication.
- Repositories: persist users and tasks with Spring Data JPA.
- Flyway: applies versioned database migrations.

## Database and integration testing

Flyway creates the task and user tables. JPA validates entity mappings against the migrated schema. Integration tests use a disposable PostgreSQL 17 container through Testcontainers and exercise the HTTP API, including the authentication boundary.

## Next security milestone

Add a task-to-user relationship, derive the current user from the verified JWT principal, and scope every task query/update/delete to that owner. Then add explicit USER/ADMIN authorization rules and cross-user access-denial tests.
