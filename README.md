# Task Management API

A portfolio REST API for managing tasks, built with Java 21 and Spring Boot 3.5.16.

## Current features

- Create, list, retrieve, update and delete tasks
- Pagination with page and size
- Sorting by an allow-listed task field
- Filter by task status
- Case-insensitive search in task titles
- Update a task status independently with PATCH
- Request validation and structured HTTP errors
- PostgreSQL persistence and Flyway schema migrations
- OpenAPI documentation and Swagger UI
- Unit and PostgreSQL integration tests with Testcontainers
- Docker Compose and GitHub Actions CI
- User persistence model and PostgreSQL migration (authentication implementation in progress)

## Tech stack

- Java 21
- Spring Boot 3.5.16
- Spring Web, Bean Validation and Spring Data JPA
- PostgreSQL 17 and Flyway
- springdoc-openapi 2.10.1
- JUnit 5, Mockito and Testcontainers
- Maven and Docker

## Architecture

`HTTP Request -> TaskController -> TaskService -> TaskRepository -> PostgreSQL`

The controller handles HTTP concerns, the service validates query options and contains business operations, the repository queries PostgreSQL, and DTOs define the API contract. JPA entities are not exposed directly.

See [docs/architecture.md](docs/architecture.md).

## Requirements

- Java 21
- Docker Desktop or Docker Engine with Docker Compose
- Maven 3.6.3+ (only needed when running Maven directly)

Check your Java version:

```bash
java -version
```

## Run the API and PostgreSQL

```bash
docker compose up --build
```

API base URL: `http://localhost:8080`

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Stop containers:

```bash
docker compose down
```

Remove containers and the local database volume (this deletes local database data):

```bash
docker compose down -v
```

## Run locally (database in Docker)

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Start Spring Boot with Java 21:

```bash
mvn spring-boot:run
```

## API endpoints

Base URL: `http://localhost:8080/api/v1/tasks`

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/v1/tasks` | Create a task | 201 Created |
| GET | `/api/v1/tasks` | List tasks with pagination and filters | 200 OK |
| GET | `/api/v1/tasks/{id}` | Get one task | 200 OK |
| PUT | `/api/v1/tasks/{id}` | Update title, description and status | 200 OK |
| PATCH | `/api/v1/tasks/{id}/status` | Update only the status | 200 OK |
| DELETE | `/api/v1/tasks/{id}` | Delete a task | 204 No Content |

### Create a task

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Learn Spring Boot","description":"Build the first API endpoint"}'
```

### List, paginate, sort, filter and search

```http
GET /api/v1/tasks?page=0&size=10&sortBy=createdAt&direction=desc&status=TODO&search=spring
```

Query parameters:

| Parameter | Default | Rules |
|---|---|---|
| `page` | `0` | Zero-based page index; 0 or greater |
| `size` | `10` | Between 1 and 100 |
| `sortBy` | `createdAt` | `id`, `title`, `status` or `createdAt` |
| `direction` | `desc` | `asc` or `desc` |
| `status` | omitted | `TODO`, `IN_PROGRESS` or `DONE` |
| `search` | omitted | Case-insensitive substring match in task title |

Example:

```bash
curl "http://localhost:8080/api/v1/tasks?page=0&size=5&sortBy=title&direction=asc&status=TODO&search=java"
```

The paginated response contains `content`, `page`, `size`, `totalElements`, `totalPages`, `first` and `last`.

### Update only task status

```bash
curl -i -X PATCH http://localhost:8080/api/v1/tasks/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
```

Valid statuses: `TODO`, `IN_PROGRESS`, `DONE`.

### Update a task

```bash
curl -i -X PUT http://localhost:8080/api/v1/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Learn Spring Boot","description":"Complete API CRUD","status":"IN_PROGRESS"}'
```

## Validation and error handling

- Blank title or title longer than 120 characters: `400 Bad Request`.
- Description longer than 2000 characters: `400 Bad Request`.
- Invalid pagination values, sort fields or sort directions: `400 Bad Request`.
- Unknown task ID: `404 Not Found`.
- Successful deletion: `204 No Content`.

Error responses use Spring's `ProblemDetail` format.

## Tests

Run unit and integration tests:

```bash
mvn clean verify
```

The integration test starts a real PostgreSQL 17 container through Testcontainers. Docker must be running locally to execute these integration tests.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/task_management` | JDBC URL |
| `DB_USERNAME` | `task_user` | Database username |
| `DB_PASSWORD` | `task_password` | Database password |

The credentials above are local-development defaults only. Do not use them in production.

## Planned milestones

1. CRUD API, pagination, filtering, search, OpenAPI and PostgreSQL integration tests.
2. User registration, Spring Security, JWT and role-based access control (in progress: user entity and database migration).
3. Projects, task assignment, comments and authorization rules.
4. Deployment, monitoring and observability.
