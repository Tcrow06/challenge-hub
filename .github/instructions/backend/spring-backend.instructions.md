---
name: Backend Spring Rules
description: Apply Spring Boot architecture, DTO boundary, and security rules when editing backend code.
applyTo: "challenge-hub-backend/src/main/java/**/*.java,challenge-hub-backend/src/main/resources/**/*.yml,challenge-hub-backend/src/main/resources/**/*.yaml,challenge-hub-backend/src/main/resources/**/*.properties"
---

# Backend delivery rules

- Follow strict layering: Controller -> Service -> Repository -> Entity/DTO.
- Keep controllers thin, validate input with `@Valid`, and return `ApiResponse<T>`.
- Never expose JPA entities directly in response payloads.
- Reuse existing services and repositories before introducing new abstractions.
- Enforce JWT + role checks and ownership checks for mutable resources.
- Use canonical roles only: `USER`, `CREATOR`, `MODERATOR`, `ADMIN`.
- Use UTC semantics for timestamps and scheduled logic.

# Required references before behavioral changes

- [api-spec.md](../../../instructions/api-spec.md)
- [db-schema.md](../../../instructions/db-schema.md)
- [workflows.md](../../../instructions/workflows.md)
- [requirements.md](../../../instructions/requirements.md)
- [media-storage.md](../../../instructions/media-storage.md) for media-related paths
