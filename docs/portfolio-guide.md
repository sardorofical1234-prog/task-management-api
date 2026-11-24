# Portfolio Presentation Guide

This document contains a CV-ready summary and an interview walkthrough for the Task Management API. Treat the CV text as a draft: include only the technical work you understand well enough to explain and demonstrate.

## Project at a glance

**Task Management API** is a backend portfolio project for managing personal and collaborative tasks.

**Technology stack:** Java 21, Spring Boot 3.5.16, Spring Web, Spring Data JPA, Spring Security, OAuth2 Resource Server JWT, PostgreSQL 17, Flyway, Maven, JUnit 5, Mockito, Testcontainers, Docker Compose, GitHub Actions and Newman.

**Main capabilities:**

- User registration and login with BCrypt password hashing and short-lived JWT access tokens.
- USER and ADMIN roles with task ownership and resource-level access checks.
- Task CRUD, pagination, sorting, status filtering and title search.
- Projects, owner-controlled membership, task assignment and comments.
- Versioned PostgreSQL schema migrations with Flyway.
- Swagger / OpenAPI documentation and Actuator readiness checks.

## CV description — English

**Task Management API | Java Backend Portfolio Project**

*Java 21, Spring Boot, Spring Security, JWT, PostgreSQL, Flyway, Docker, JUnit 5, Testcontainers, GitHub Actions, Newman*

- Developed a REST API for task and project collaboration, including task lifecycle management, project membership, assignments and comments.
- Implemented stateless JWT authentication, BCrypt password hashing, USER / ADMIN authorization and ownership-based resource access.
- Designed PostgreSQL persistence and versioned Flyway migrations for users, tasks, projects, memberships and comments.
- Added paginated, sortable and filterable task queries with title search.
- Automated verification with Java unit and PostgreSQL integration tests, Docker Compose runtime checks, health probes and a 10-request Newman API collection in GitHub Actions.

## CV tavsifi — O‘zbekcha

**Task Management API | Java Backend Portfolio loyihasi**

*Java 21, Spring Boot, Spring Security, JWT, PostgreSQL, Flyway, Docker, JUnit 5, Testcontainers, GitHub Actions, Newman*

- Task va project collaboration uchun REST API: task lifecycle, project membership, assignment va comments funksiyalarini ishlab chiqish.
- BCrypt password hashing, stateless JWT authentication, USER / ADMIN rollari va resource-level authorization mexanizmlarini joriy qilish.
- User, task, project, membership va comment ma’lumotlari uchun PostgreSQL modeli hamda Flyway migratsiyalarini tashkil etish.
- Task ro‘yxati uchun pagination, sorting, status filtering va title search imkoniyatlarini qo‘shish.
- GitHub Actions orqali Java testlari, Docker Compose runtime tekshiruvi, health check va Newman API testlarini avtomatlashtirish.

## 60-second interview pitch — English

"I built this Task Management API as a Java backend portfolio project using Java 21 and Spring Boot. It supports user registration and JWT-based login, task management, and collaborative projects. Security is enforced at the resource level: users can access tasks they own, tasks assigned to them, or tasks in projects where they participate, while administrators have broader access. PostgreSQL stores the data and Flyway manages schema changes. I also added pagination and search, integration tests with Testcontainers, and a GitHub Actions pipeline that starts the API and database in Docker Compose and runs the Postman collection through Newman."

## 60 soniyalik taqdimot — O‘zbekcha

"Bu Task Management API — Java 21 va Spring Boot asosida tayyorlangan backend portfolio loyihasi. Unda user registration, JWT login, task boshqaruvi va jamoaviy project funksiyalari mavjud. Xavfsizlik resource darajasida tekshiriladi: oddiy user faqat o‘zi yaratgan, unga biriktirilgan yoki a’zosi bo‘lgan project tasklarini ko‘ra oladi; ADMIN esa kengroq huquqlarga ega. Ma’lumotlar PostgreSQL’da saqlanadi, schema o‘zgarishlarini Flyway boshqaradi. Pagination va search, Testcontainers bilan integration testlar, Docker Compose va Newman ishlatadigan GitHub Actions CI ham mavjud."

## Suggested live demo

1. Open the repository and point out the Java / Spring Boot stack and CI badge.
2. Start the local environment with `docker compose up --build`.
3. Open Swagger UI at `http://localhost:8080/swagger-ui.html`.
4. Import `postman/Task Management API.postman_collection.json` and `postman/Local.postman_environment.json`.
5. Run the collection from top to bottom. It registers a demo user, logs in, saves the JWT, creates tasks and a project, adds a comment and checks readiness.
6. Show the latest successful GitHub Actions run and explain that CI repeats the API checks automatically.

Stop the demo with `docker compose down`.

## Technical topics to be ready to explain

### Authentication and JWT

Explain the difference between authentication (who is making the request) and authorization (what that user can access). Describe how the login flow verifies a BCrypt hash and issues a signed JWT, and how Spring Security validates the token on later requests.

### Task ownership and project access

Explain why a task's owner is not accepted from the request body: the application derives the owner from the authenticated principal. A USER can access a task if they created it, are assigned to it, own its project or belong to its project. ADMIN has broader access. An unrelated user receives 404 to avoid revealing that a protected task exists.

### Database migrations

Explain why Flyway migrations are version-controlled and why Hibernate uses schema validation rather than automatically changing the database schema.

### Testing strategy

Explain the roles of unit tests, PostgreSQL integration tests with Testcontainers and HTTP-level API tests with Newman. Unit tests isolate service logic; Testcontainers verifies behavior against PostgreSQL; Newman runs the exported Postman requests against a live application.

### CI pipeline

Walk through the pipeline in order: Java 21 setup, Maven verification, Postman JSON validation, Docker Compose startup, readiness check, Newman collection, Swagger and unauthenticated-access checks, and container cleanup.

## Current scope and known limitations

Be clear about what is and is not implemented:

- There is no public hosted demo URL; the project is intended to run locally with Docker Compose.
- Comments are append-only: create and list operations are implemented, but edit/delete endpoints are not.
- Pagination is implemented for the task list; project, member and comment lists do not yet have pagination.
- There is no frontend, production monitoring stack or automated database backup setup.

These are explicit future improvements, not features to claim as complete.
