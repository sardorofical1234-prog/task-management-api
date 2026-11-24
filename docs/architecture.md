# Architecture — milestone 1

## Request lifecycle

`Client -> TaskController -> TaskService -> TaskRepository -> PostgreSQL`

The controller validates the request DTO and delegates to the service. The service performs the use case and calls the repository. The repository uses Spring Data JPA and Hibernate to communicate with PostgreSQL. The service maps the entity to a response DTO before returning it.

## Current modules

- `task`: entity, status enum, repository, service, controller and DTOs.
- `common.exception`: centralized REST error handling.
- `resources/db/migration`: versioned schema migrations managed by Flyway.

## Design decisions

1. Controllers do not contain business logic.
2. DTOs define the API contract; JPA entities are not exposed.
3. `TaskService` is transactional for write operations and read-only by default.
4. Hibernate schema generation is set to `validate`; Flyway owns schema changes.
5. Task status is persisted as a string so database values remain readable.

## Scope boundary

This milestone deliberately has no authentication or user ownership yet. JWT, users and role-based authorization will be introduced after the CRUD and persistence flow is understood and tested.
