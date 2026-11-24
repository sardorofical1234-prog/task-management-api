# Task Management API

A portfolio REST API built with Java 21 and Spring Boot 3.5.16.

## Features

- User registration and JWT authentication
- BCrypt password hashing; plain-text passwords are never stored
- Stateless Spring Security and Bearer-token validation
- USER and ADMIN role authorities carried by JWTs
- Task CRUD, pagination, sorting, status filtering and title search
- PostgreSQL, Flyway migrations, OpenAPI / Swagger UI
- Unit and PostgreSQL integration tests with Testcontainers
- Docker Compose and GitHub Actions CI

> This milestone authenticates requests. Task ownership / tenant isolation and role-based endpoint restrictions are next. Currently, any authenticated user can access the shared task collection.

## Tech stack

- Java 21, Spring Boot 3.5.16, Maven
- Spring Web, Validation, Spring Data JPA
- Spring Security, OAuth2 Resource Server JWT
- PostgreSQL 17, Flyway
- springdoc-openapi, JUnit 5, Mockito, Testcontainers, Docker

## Architecture

`HTTP Request -> Spring Security JWT Filter -> Controller -> Service -> Repository -> PostgreSQL`

The security filter validates a Bearer JWT before protected controller methods run. Controllers handle HTTP concerns, services contain business operations, repositories access PostgreSQL, and DTOs define the API contract. JPA entities are not exposed directly.

See [docs/architecture.md](docs/architecture.md).

## Requirements

- Java 21
- Docker Desktop or Docker Engine with Compose
- Maven 3.6.3+ (only needed when running Maven directly)
- OpenSSL (optional, to generate a local JWT signing key)

Check Java: 

~~~bash
java -version
~~~

## Configure JWT signing key

The checked-in Base64 fallback is for local development only. Set a private key before deploying or sharing an environment. Generate 48 random bytes:

~~~bash
export JWT_SECRET_BASE64="$(openssl rand -base64 48)"
~~~

Keep this value private. Do not commit it or expose it in logs. All application instances that need to validate the same access tokens must use the same key.

Optional token lifetime:

~~~bash
export JWT_ACCESS_TOKEN_TTL="PT15M"
~~~

## Run the API and PostgreSQL

~~~bash
docker compose up --build
~~~

API base URL: http://localhost:8080

Swagger UI: http://localhost:8080/swagger-ui.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

Stop containers:

~~~bash
docker compose down
~~~

Remove containers and the local database volume (deletes local database data):

~~~bash
docker compose down -v
~~~

## Run locally (database in Docker)

~~~bash
docker compose up -d postgres
export JWT_SECRET_BASE64="$(openssl rand -base64 48)"
mvn spring-boot:run
~~~

## Authentication API

### Register

~~~bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \\
  -H 'Content-Type: application/json' \\
  -d '{"email":"developer@example.com","password":"Password123!"}'
~~~

A successful registration returns 201 Created, the profile and the default USER role. The password is stored as a BCrypt hash.

Password rules: 8 to 72 characters; BCrypt's 72-byte input limit is also enforced. Email addresses are trimmed and normalized to lowercase.

### Login

~~~bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \\
  -H 'Content-Type: application/json' \\
  -d '{"email":"developer@example.com","password":"Password123!"}'
~~~

A successful login returns:

~~~json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
~~~

Access tokens expire after 15 minutes by default. Invalid credentials return 401 Unauthorized; duplicate registration returns 409 Conflict.

### Call protected task endpoints

~~~bash
curl http://localhost:8080/api/v1/tasks \\
  -H "Authorization: Bearer <JWT>"
~~~

All /api/v1/tasks/** endpoints require a valid token. Registration, login, Swagger UI and OpenAPI docs are public.

## Task API endpoints

Base URL: http://localhost:8080/api/v1/tasks

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | /api/v1/tasks | Create a task | 201 Created |
| GET | /api/v1/tasks | List tasks with pagination and filters | 200 OK |
| GET | /api/v1/tasks/{id} | Get one task | 200 OK |
| PUT | /api/v1/tasks/{id} | Update title, description and status | 200 OK |
| PATCH | /api/v1/tasks/{id}/status | Update only the status | 200 OK |
| DELETE | /api/v1/tasks/{id} | Delete a task | 204 No Content |

All task requests require Authorization: Bearer <JWT>.

### Create a task

~~~bash
curl -i -X POST http://localhost:8080/api/v1/tasks \\
  -H 'Authorization: Bearer <JWT>' \\
  -H 'Content-Type: application/json' \\
  -d '{"title":"Learn Spring Boot","description":"Build the first API endpoint"}'
~~~

### List, paginate, sort, filter and search

~~~http
GET /api/v1/tasks?page=0&size=10&sortBy=createdAt&direction=desc&status=TODO&search=spring
Authorization: Bearer <JWT>
~~~

Query parameters:

| Parameter | Default | Rules |
|---|---|---|
| page | 0 | Zero-based page index; 0 or greater |
| size | 10 | Between 1 and 100 |
| sortBy | createdAt | id, title, status or createdAt |
| direction | desc | asc or desc |
| status | omitted | TODO, IN_PROGRESS or DONE |
| search | omitted | Case-insensitive substring match in task title |

The paginated response contains content, page, size, totalElements, totalPages, first and last.

## Validation and error handling

- Invalid request payload, pagination value or sort option: 400 Bad Request.
- Invalid login credentials or missing / invalid JWT: 401 Unauthorized.
- Duplicate email: 409 Conflict.
- Unknown task ID: 404 Not Found.
- Successful deletion: 204 No Content.

Application validation errors use Spring ProblemDetail. Spring Security handles authentication failures.

## Tests

Run unit and integration tests:

~~~bash
mvn clean verify
~~~

The integration test starts a disposable PostgreSQL 17 container through Testcontainers. Docker must be running locally. Tests cover registration, duplicate email, BCrypt password hashing, login, rejected credentials, JWT-protected routes and the task API.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| SERVER_PORT | 8080 | HTTP port |
| DB_URL | jdbc:postgresql://localhost:5432/task_management | JDBC URL |
| DB_USERNAME | task_user | Database username |
| DB_PASSWORD | task_password | Database password |
| JWT_SECRET_BASE64 | Development-only key | Base64 HMAC signing key; replace outside local development |
| JWT_ACCESS_TOKEN_TTL | PT15M | Access-token lifetime (ISO-8601 duration) |

Database credentials and JWT fallback are local-development defaults only. Do not use them in production.

## Planned milestones

1. CRUD API, pagination, search, OpenAPI and PostgreSQL integration tests — complete.
2. User persistence, registration, login, BCrypt and JWT authentication — implemented; CI verification pending.
3. Task ownership, user data isolation and role-based endpoint authorization — next.
4. Projects, task assignment and comments.
5. Deployment, monitoring and observability.
