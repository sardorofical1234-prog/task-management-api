# Architecture — Task Management API

## Request lifecycle

~~~mermaid
flowchart TD
    A["HTTP Request"] --> B["Spring Security JWT Filter"]
    B --> C["Controller"]
    C --> D["Service"]
    D --> E["Authorization Check"]
    E --> F["Repository"]
    F --> G[("PostgreSQL")]
    G --> F
    F --> C
    C --> H["HTTP JSON Response"]
~~~

## Authentication

1. POST /api/v1/auth/register validates the request, normalizes email and stores a BCrypt password hash. New users receive USER.
2. POST /api/v1/auth/login delegates credential verification to Spring Security and issues a short-lived HS256 JWT.
3. The resource-server filter validates token signature, issuer and time claims on protected requests.
4. Authorities from the signed role claim are mapped to ROLE_USER or ROLE_ADMIN.

## Project model

- A Project has one owner (User), name, description and creation time.
- ProjectMember links a User to a Project. A unique constraint prevents duplicate membership.
- Only the project owner or ADMIN can update/delete a project and manage its member list.
- Project owner, members and ADMIN can view the project.
- A member or project owner can create tasks inside the project.

## Task model and assignment

- A Task has a creator (owner), and may optionally reference a Project and an assignedTo User.
- Standalone tasks remain supported.
- Task creation inside a project is done through POST /api/v1/projects/{projectId}/tasks.
- Project owner or ADMIN can assign a task. The assignee must be the project owner or a project member.
- TaskResponse exposes projectId and assigneeId, but the caller cannot set task ownership via request data.

## Task access rules

For a USER, a task is visible when at least one condition holds:

- The user created the task.
- The user is assigned to the task.
- The user owns the task's project.
- The user is a member of the task's project.

The same access check is applied to get, update, status update, delete and comment operations. Task list filtering uses database predicates and a membership subquery, so inaccessible tasks are not loaded and filtered in application memory.

ADMIN can access all tasks, including legacy tasks without an owner. An unrelated USER receives 404 rather than 403 to avoid disclosing resource existence.

## Comments

- A TaskComment belongs to exactly one task and one author.
- Comments are append-only in the current API; there are create and list endpoints.
- Only users who can access the task can read or create comments.
- Comments are returned in chronological order.

## Database migrations

- V1 creates tasks.
- V2 creates users.
- V3 adds task owner_id.
- V4 creates projects and project_members, adds optional project_id and assignee_id to tasks, and creates task_comments.

Project deletion cascades membership rows and sets tasks' project_id to NULL to retain task records. Task deletion cascades its comments. The project owner and comment author foreign keys prevent deleting referenced users unexpectedly.

## Security boundaries

- Stateless JWT authentication; sessions, form login, HTTP Basic and CSRF are disabled.
- Passwords are stored as BCrypt hashes.
- JWT signing material comes from JWT_SECRET_BASE64. Production environments must provide a private random key of at least 32 decoded bytes.
- New registrations always receive USER; no public endpoint can grant ADMIN.
- The ownership/role checks live in service methods as well as the authenticated filter boundary.

## Components

- AuthController / AuthService: registration and login.
- CurrentUserService: maps the verified security principal to a persisted User and role.
- ProjectController / ProjectService: project CRUD and membership.
- TaskController / TaskService: task CRUD, access checks, project creation and assignment.
- CommentController / CommentService: task collaboration.
- Repositories: persist users, projects, project memberships, tasks and comments.
- Flyway: applies versioned database migrations.

## Integration testing

Tests run against PostgreSQL 17 using Testcontainers. They verify authentication, owner isolation, project membership, task assignment, comments and ADMIN cross-user access.
