# Task Management API

[![Java CI](https://github.com/sardorofical1234-prog/task-management-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/sardorofical1234-prog/task-management-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.5.16](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-blue)

A backend portfolio REST API built with Java 21 and Spring Boot 3.5.16. It demonstrates authentication, authorization, relational data modeling, project collaboration, automated testing and containerized execution.

## Features

- User registration and JWT authentication
- BCrypt password hashing and stateless Spring Security
- USER and ADMIN roles
- Task CRUD, pagination, sorting, status filtering and title search
- Task ownership and project membership authorization
- Projects with owner-controlled membership
- Task creation inside projects and assignment to project members
- Task comments with access checks
- PostgreSQL, Flyway, OpenAPI / Swagger UI (disabled in production)
- Actuator liveness/readiness probes with database connectivity checks
- Unit and PostgreSQL integration tests with Testcontainers
- Docker Compose runtime and GitHub Actions CI with end-to-end container smoke checks
- Newman executes the Postman API collection in CI against the running API
- Ready-to-import Postman collection for a complete local API demo

## Tech stack

- Java 21, Spring Boot 3.5.16, Maven
- Spring Web, Validation, Spring Data JPA
- Spring Security, OAuth2 Resource Server JWT, Spring Boot Actuator
- PostgreSQL 17, Flyway
- springdoc-openapi, JUnit 5, Mockito, Testcontainers, Docker

## Architecture

~~~mermaid
flowchart TD
    A["Client / Postman"] --> B["Spring Security"]
    B -->|"Valid JWT"| C["REST Controllers"]
    B -->|"Missing or invalid JWT"| X["401 Unauthorized"]
    C --> D["Application Services"]
    D --> E["Ownership and membership checks"]
    E --> F["Spring Data JPA Repositories"]
    F --> G[("PostgreSQL 17")]
    H["Flyway migrations"] --> G
~~~

A USER can access tasks they created, tasks in projects where they are a member, and tasks assigned to them. Project owners manage project membership and assignment. ADMIN can access resources across users.

See [the detailed architecture and ER diagram](docs/architecture.md).

## Requirements

- Java 21
- Docker Desktop or Docker Engine with Compose
- Maven 3.6.3+ (only needed when running Maven directly)
- OpenSSL (optional, to generate a local JWT signing key)

~~~bash
java -version
~~~

## Configure JWT signing key

The checked-in Base64 fallback is for local development only. Set a private key before deploying or sharing an environment:

~~~bash
export JWT_SECRET_BASE64="$(openssl rand -base64 48)"
~~~

Keep this value private. Do not commit it or expose it in logs. All application instances that need to validate the same access tokens must use the same key.

Optional token lifetime:

~~~bash
export JWT_ACCESS_TOKEN_TTL="PT15M"
~~~

## Free local portfolio demo

No paid hosting, domain, cloud account or external database is required. Docker Compose starts the Spring Boot API and PostgreSQL on your computer.

~~~bash
docker compose up --build
~~~

The first build downloads dependencies and creates the containers. When both services are healthy:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Liveness: http://localhost:8080/actuator/health/liveness
- Readiness: http://localhost:8080/actuator/health/readiness

### Run the ready-made Postman demo

1. Import `postman/Task Management API.postman_collection.json` into Postman.
2. Import and select `postman/Local.postman_environment.json`.
3. Run the collection requests from top to bottom.

The collection registers a fresh demo user, logs in, stores the JWT automatically, creates a task and project, creates a project task, and adds/lists a comment. Each request has response assertions and the full collection also runs in GitHub Actions through Newman. The demo password is for local testing only. Do not reuse it for a real account.

Stop the containers:

~~~bash
docker compose down
~~~

Reset the local database completely (this deletes the local demo data):

~~~bash
docker compose down -v
~~~

The database is stored in a Docker volume on your computer. It is not published to the internet.

## Run locally (database in Docker)

~~~bash
docker compose up -d postgres
export JWT_SECRET_BASE64="$(openssl rand -base64 48)"
mvn spring-boot:run
~~~

## Continuous integration

Every push to `main` and pull request targeting `main` runs the GitHub Actions workflow:

1. Run `mvn clean verify`, including the PostgreSQL integration tests with Testcontainers.
2. Validate the Postman collection and environment JSON.
3. Build the API container and start the API plus PostgreSQL using Docker Compose.
4. Wait for `/actuator/health/readiness` to report `UP`, which includes a database connectivity check.
5. Run the Postman API collection with Newman against the live API, covering registration, JWT login, task creation/listing/status updates, project creation, project tasks and comments.
6. Confirm Swagger UI is reachable and an unauthenticated request to `/api/v1/tasks` returns `401 Unauthorized`.
7. Print container logs on failure and always stop the containers and remove the temporary CI database volume.

Latest verified run: [GitHub Actions](https://github.com/sardorofical1234-prog/task-management-api/actions/workflows/ci.yml).

The pipeline currently verifies 18 Java unit/integration tests, 10 Newman API requests with 10 assertions, Docker Compose startup, PostgreSQL readiness, Swagger availability and unauthenticated access rejection. This is a runtime and API integration test, not a public deployment. It runs entirely on GitHub-hosted CI and requires no paid hosting account.

## Authentication API

### Register

~~~bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"developer@example.com","password":"Password123!"}'
~~~

A successful registration returns 201 Created and the default USER role. The password is stored as a BCrypt hash. Clients cannot choose their own role during registration.

### Login

~~~bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"developer@example.com","password":"Password123!"}'
~~~

The response contains an accessToken, tokenType (Bearer) and expiresIn (seconds). Access tokens expire after 15 minutes by default.

Use the token in the Authorization header for all project and task endpoints:

~~~bash
curl http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <JWT>"
~~~

## Projects API

All project endpoints require a valid Bearer token.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | /api/v1/projects | Create a project | Authenticated user |
| GET | /api/v1/projects | List accessible projects | Owner/member; ADMIN: all |
| GET | /api/v1/projects/{projectId} | Get a project | Owner/member; ADMIN |
| PUT | /api/v1/projects/{projectId} | Update project | Owner; ADMIN |
| DELETE | /api/v1/projects/{projectId} | Delete project | Owner; ADMIN |
| POST | /api/v1/projects/{projectId}/members | Add a member | Owner; ADMIN |
| GET | /api/v1/projects/{projectId}/members | List members | Owner/member; ADMIN |
| DELETE | /api/v1/projects/{projectId}/members/{userId} | Remove a member | Owner; ADMIN |
| POST | /api/v1/projects/{projectId}/tasks | Create a task in a project | Owner/member; ADMIN |

Create a project:

~~~bash
curl -i -X POST http://localhost:8080/api/v1/projects \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Payments API","description":"Payment service development"}'
~~~

Add an existing user to the project (use their database user ID):

~~~bash
curl -i -X POST http://localhost:8080/api/v1/projects/1/members \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"userId":2}'
~~~

Create a task inside a project:

~~~bash
curl -i -X POST http://localhost:8080/api/v1/projects/1/tasks \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Implement payment endpoint","description":"Add payment API"}'
~~~

## Task API

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/v1/tasks | Create a standalone task |
| GET | /api/v1/tasks | List tasks visible to the current user |
| GET | /api/v1/tasks/{id} | Get an accessible task |
| PUT | /api/v1/tasks/{id} | Update an accessible task |
| PATCH | /api/v1/tasks/{id}/status | Update task status |
| PATCH | /api/v1/tasks/{id}/assignment | Assign a task to a project member |
| DELETE | /api/v1/tasks/{id} | Delete an accessible task |

Task assignment (the caller must be the project owner or ADMIN; assignee must be the project owner or a project member):

~~~bash
curl -i -X PATCH http://localhost:8080/api/v1/tasks/1/assignment \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"projectId":1,"assigneeId":2}'
~~~

List and filter tasks:

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

Task responses include projectId and assigneeId when the task belongs to a project or has an assignee.

## Task comments API

Only users who can access a task can read or add its comments.

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/v1/tasks/{taskId}/comments | Add a comment |
| GET | /api/v1/tasks/{taskId}/comments | List comments in chronological order |

Add a comment:

~~~bash
curl -i -X POST http://localhost:8080/api/v1/tasks/1/comments \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"content":"I have started the implementation."}'
~~~

Comments are limited to 2,000 characters and include author identity and creation time.

## Authorization rules

- USER can create standalone tasks and access tasks they created, tasks in projects they own or belong to, and tasks assigned to them.
- Project owners can update/delete their projects, add/remove members and assign tasks to project members.
- Project members can view the project, list its members, create tasks inside it and collaborate through task comments.
- ADMIN can access and manage projects and tasks across users.
- Registration always creates USER accounts. There is no public role-escalation endpoint.
- A task belonging to another unrelated user returns 404 to avoid disclosing resource existence.
- Existing tasks without an owner (from before ownership was introduced) remain visible only to ADMIN until explicitly reconciled.

## Validation and errors

- Invalid request payload, pagination value or sort option: 400 Bad Request.
- Invalid login or missing/invalid JWT: 401 Unauthorized.
- Duplicate email or duplicate project membership: 409 Conflict.
- Unknown or inaccessible task/project: 404 Not Found.
- Successful deletion: 204 No Content.

Application validation errors use Spring ProblemDetail. Spring Security handles authentication failures.

## Production profile and health checks

The `prod` Spring profile is designed for hosted environments:

- Requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET_BASE64`.
- Does not fall back to the development JWT key. The application fails to start if the production secret is missing.
- Disables Swagger UI and OpenAPI document endpoints.
- Uses graceful shutdown, forwarded headers and conservative production error responses.
- Exposes only health status, not component or database details.

Health endpoints:

| Endpoint | Purpose |
|---|---|
| `/actuator/health/liveness` | Confirms the process is alive |
| `/actuator/health/readiness` | Confirms the app is ready and PostgreSQL is reachable |

The readiness endpoint is public for hosting-platform probes, but it returns status only. Other Actuator endpoints are not exposed.

## Hosting is optional

This repository is intentionally prepared to be demonstrated locally at no hosting cost. The production profile and health probes are included as backend engineering practice; you do not need a hosted service to run the project, execute its tests or present the API. A public demo URL can be added later if it becomes necessary.

## Tests

Run unit and integration tests:

~~~bash
mvn clean verify
~~~

The integration test uses a disposable PostgreSQL 17 container through Testcontainers. Docker must be running locally. It covers registration, BCrypt, JWT, task ownership, project membership, task assignment, comments and ADMIN access.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| SERVER_PORT | 8080 | HTTP port |
| DB_URL | jdbc:postgresql://localhost:5432/task_management | JDBC URL override |
| DB_USERNAME | task_user | Database username |
| DB_PASSWORD | task_password | Database password |
| JWT_SECRET_BASE64 | Development-only key | Base64 HMAC signing key; replace outside local development |
| JWT_ACCESS_TOKEN_TTL | PT15M | Access-token lifetime (ISO-8601 duration) |

Database credentials and JWT fallback are local-development defaults only. Do not use them in production.

## Planned milestones

1. CRUD API, pagination, search, OpenAPI and PostgreSQL integration tests — complete.
2. Registration, BCrypt, JWT authentication and USER / ADMIN authorization — complete.
3. Task ownership — complete.
4. Projects, project membership, task assignment and comments — complete (Java 21 CI passed; 18 tests).
5. Local portfolio demo, production profile, health checks, Docker image CI and Postman collection — configured; no paid hosting required.
