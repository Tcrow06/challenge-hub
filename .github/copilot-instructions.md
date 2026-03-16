<!-- .github/copilot-instructions.md -->

# Copilot Instructions

## Project Overview

This repository contains a full-stack application.

Backend:

- Spring Boot (Java 17)
- PostgreSQL 15
- MongoDB 6.0
- Redis 7

Frontend:

- React + TypeScript + Vite
- Feature-based folder structure
- TanStack Query for API calls
- Tailwind CSS

The system is a challenge platform where users complete tasks, submit media, gain scores, and appear on leaderboards.

Architecture documentation is located in the `/instructions` directory.

Copilot MUST read those documents before generating code related to:

- API endpoints → `api-spec.md`
- Database entities → `db-schema.md`
- Business workflows → `workflows.md`
- Business rules & error codes → `requirements.md`
- Infrastructure & scheduled jobs → `infrastructure.md`
- Media storage → `media-storage.md`
- Project structure → `project-structure.md`

---

# General Coding Principles

1. Prefer readable and maintainable code over clever code.
2. Follow the existing project structure strictly.
3. Do not introduce new architectural patterns unless necessary.
4. Avoid duplication and reuse existing services/utilities.
5. Never expose database entities directly to API responses.
6. Use error codes from `requirements.md` section 8 — never invent new ones without updating the catalog.
7. All timestamps use UTC.

---

# Backend Rules (Spring Boot)

## Architecture Layers

Use the following layers:

Controller → Service → Repository → Entity + DTO

Never skip layers.

---

## Controller Guidelines

Controllers should:

- Be thin
- Contain no business logic
- Only orchestrate requests
- Use `@Valid` for request validation

All controllers must return a standard response wrapper:

`ApiResponse<T>`

Success: `ApiResponse.success(data)`
Success with message: `ApiResponse.success(data, "message")`
Error: `ApiResponse.error(errorCode, message)`
Error with field errors: `ApiResponse.validationError(errors)`

---

## DTO Rules

Never return JPA entities directly.

Always create:

- Request DTO (in `dto/request/`)
- Response DTO (in `dto/response/`)

Example naming:

- `CreateChallengeRequest`
- `ChallengeResponse`
- `UpdateSubmissionStatusRequest`
- `SubmissionResponse`

Use Bean Validation annotations: `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern`.

---

## Entity Rules

Entities must:

- Use JPA annotations
- Use UUID as primary key (`@GeneratedValue(strategy = GenerationType.UUID)`)
- Include `createdAt` and `updatedAt` with `@CreatedDate` and `@LastModifiedDate`
- Avoid business logic inside entities
- Only contain domain fields

---

## Service Rules

Services contain business logic.

Responsibilities:

- validation (business rules)
- orchestration of workflows
- calling repositories
- publishing events if necessary
- calling audit log service for state-changing operations

Services should remain stateless.

Use `@Transactional` for operations spanning multiple repository calls.
Use `@Async` for non-critical side effects (MongoDB logging, notifications, emails).

---

## Repository Rules

Repositories must use Spring Data JPA (Postgres) or Spring Data MongoDB.

Avoid raw SQL unless performance requires it.

Prefer method-based queries:

- `findById`
- `findByUserId`
- `findByChallengeIdAndStatus`
- `existsByUserIdAndChallengeId`

---

## Security

Use Spring Security with JWT authentication.

Rules:

- All endpoints require authentication unless explicitly marked Public in `api-spec.md`
- Use role-based access control
- Check ownership before allowing PUT/PATCH/DELETE on user-owned resources
- Validate AT against Redis blacklist on every request

Roles (canonical list — do not use abbreviations):

- USER
- CREATOR
- MODERATOR
- ADMIN

JWT claims: `sub` (user_id), `role`, `jti` (unique token ID), `iat`, `exp`.

---

## Error Handling

Use a global exception handler (`@RestControllerAdvice`).

All errors must return structured `ApiResponse.error()` with:

- `errorCode` from the Error Code Catalog (`requirements.md` section 8)
- `message` (human-readable)
- `errors[]` (field-level, only for validation errors)

Never expose stack traces in API responses.

---

# Database Guidelines

PostgreSQL is the primary relational database.

MongoDB is used for:

- activity feed
- comments
- reactions
- audit logs
- notifications

Redis is used for:

- Refresh Token Family storage (RT rotation, replay detection, family blocking)
- Leaderboard (ZSET)
- Access Token blacklist
- Optional caching

Do not mix relational logic with MongoDB documents.

Refer to `db-schema.md` for exact table/collection definitions and indexes.

---

# Media Upload

Media storage must use the abstraction defined in:

`instructions/media-storage.md`

Never directly call a storage provider (MinIO, Cloudinary, R2).

Always use the `MediaStorageService` interface.

Upload flow: Signed URL → Client direct upload → Confirm endpoint.

---

# Frontend Rules (React + TypeScript)

## Folder Structure

Use feature-based structure:

```
features/
  auth/
  challenges/
  submissions/
  social/
  notifications/
```

Each feature contains:

- components
- pages
- hooks
- api

---

## Component Rules

Components must be:

- small
- reusable
- typed with TypeScript interfaces

Prefer functional components with hooks.

---

## API Layer

All API calls must go through:

`/api/client` (Axios instance with interceptors for auth + refresh)

Never call fetch or axios directly inside components.

Use TanStack Query hooks for data fetching and mutation.

---

## State Management

Use Zustand for global state (auth, notifications count).

Use TanStack Query cache for server state.

---

# Naming Conventions

Backend:

- Service classes end with `Service`
- Controllers end with `Controller`
- Repositories end with `Repository`
- DTOs: `{Action}{Resource}Request` / `{Resource}Response`

Frontend:

- Components use PascalCase
- Hooks start with `use`
- API functions: `{verb}{Resource}` (e.g., `fetchChallenges`, `createSubmission`)
- Types: match backend ResponseDTO names

---

# Before Generating Code

Copilot MUST check:

1. Existing implementations in the codebase
2. API specification in `/instructions/api-spec.md` (request/response bodies)
3. Database schema in `/instructions/db-schema.md` (tables, constraints, indexes)
4. Business rules in `/instructions/requirements.md` (state machines, error codes)
5. Workflows in `/instructions/workflows.md` (side effects, async operations)

Do not invent new endpoints, database fields, or error codes unless required and documented.

---

# Testing

Backend tests:

- JUnit 5 + SpringBootTest
- Use `@DataJpaTest` for repository tests
- Use `@WebMvcTest` for controller tests
- Integration tests with Testcontainers (Postgres, MongoDB, Redis)

Frontend tests:

- React Testing Library + Vitest

Write tests for critical workflows: auth, submission, scoring, leaderboard.

---

# Output Expectations

Generated code should be:

- production-ready
- consistent with project architecture
- readable and maintainable
- fully typed (no `any` in TypeScript)
- using error codes from the catalog
