# Task Management API

A portfolio REST API for managing tasks, built with Java 21 and Spring Boot. Milestone 1 focuses on a clean layered architecture, PostgreSQL persistence, validation, and complete CRUD operations. Authentication and role-based authorization will follow in later milestones.

## Tech stack

- Java 21
- Spring Boot 3.5.16
- Maven
- Spring Web and Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- JUnit 5, Mockito and AssertJ
- Docker and Docker Compose
- GitHub Actions

## Features

- Create, list, retrieve, update and delete tasks
- Validate request data
- Persist tasks in PostgreSQL
- Manage schema changes with Flyway
- Return structured 400 and 404 errors
- Run the API and database with Docker Compose
- Run automated tests in GitHub Actions

## Architecture

`HTTP Request -> TaskController -> TaskService -> TaskRepository -> PostgreSQL`

The controller handles HTTP concerns, the service contains business operations, the repository handles persistence, and DTOs define the API contract. JPA entities are not returned directly from the API. See [docs/architecture.md](docs/architecture.md).

## Requirements

- Java 21
- Docker Desktop or Docker Engine with the Compose plugin
- Maven 3.6.3+ (only needed when running Maven directly)

Check Java: `java -version`

## Run everything with Docker Compose

From the repository root:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080` and PostgreSQL at `localhost:5432`.

Stop containers: `docker compose down`

Remove containers and the local database volume (deletes local data): `docker compose down -v`

## Run locally (database in Docker)

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the application using Java 21 and Maven:

```bash
mvn spring-boot:run
```

## API endpoints

Base URL: `http://localhost:8080/api/v1/tasks`

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/v1/tasks` | Create a task | 201 Created |
| GET | `/api/v1/tasks` | List all tasks | 200 OK |
| GET | `/api/v1/tasks/{id}` | Get one task | 200 OK |
| PUT | `/api/v1/tasks/{id}` | Update a task | 200 OK |
| DELETE | `/api/v1/tasks/{id}` | Delete a task | 204 No Content |

### Create a task

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks \\
  -H 'Content-Type: application/json' \\
  -d '{"title":"Learn Spring Boot","description":"Build the first API endpoint"}'
```

Example response shape (ID and timestamp are generated):

```json
{
  "id": 1,
  "title": "Learn Spring Boot",
  "description": "Build the first API endpoint",
  "status": "TODO",
  "createdAt": "2026-11-24T12:00:00Z"
}
```

### Update a task

```bash
curl -i -X PUT http://localhost:8080/api/v1/tasks/1 \\
  -H 'Content-Type: application/json' \\
  -d '{"title":"Learn Spring Boot","description":"Complete CRUD","status":"IN_PROGRESS"}'
```

Valid statuses: `TODO`, `IN_PROGRESS`, `DONE`.

## Validation and errors

- Blank title or title longer than 120 characters: `400 Bad Request`.
- Description longer than 2000 characters: `400 Bad Request`.
- Task ID does not exist: `404 Not Found`.
- Successful deletion: `204 No Content`.

## Run tests

```bash
mvn test
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/task_management` | JDBC URL |
| `DB_USERNAME` | `task_user` | Database username |
| `DB_PASSWORD` | `task_password` | Database password |

These credentials are local-development defaults only. Do not use them in production.

## Planned milestones

1. CRUD API, PostgreSQL, migrations, validation and unit tests (current).
2. Pagination, filtering and integration tests with Testcontainers.
3. Spring Security, JWT and role-based access control.
4. Projects, task assignment, comments and authorization rules.
5. Deployment and observability.
