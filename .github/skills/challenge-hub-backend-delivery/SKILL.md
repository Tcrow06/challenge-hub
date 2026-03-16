---
name: challenge-hub-backend-delivery
description: Backend delivery playbook for Challenge Hub. Use for Spring Boot API, DTO, service, repository, auth, workflow, or storage-related changes.
license: Proprietary - internal repository use only
---

Use this skill when a task touches backend code in `challenge-hub-backend`.

## Mandatory references

- `.github/copilot-instructions.md`
- `instructions/api-spec.md`
- `instructions/db-schema.md`
- `instructions/workflows.md`
- `instructions/requirements.md` (error catalog in section 8)
- `instructions/infrastructure.md`
- `instructions/media-storage.md` when media behavior is involved

## Backend delivery checklist

1. Map requested behavior to existing API spec and workflow.
2. Implement in strict layers: Controller -> Service -> Repository -> Entity/DTO.
3. Keep controller thin and ensure `@Valid` and `ApiResponse<T>` usage.
4. Keep DTO boundaries strict (never expose entities in response payloads).
5. Reuse existing utilities/services before adding new abstractions.
6. Enforce business constraints and canonical error codes from requirements.
7. Ensure auth/ownership checks for user-owned resources.
8. Use UTC-compatible timestamp behavior.

## Validation guidance

- Run focused checks first (target module/test), then broader checks.
- Do not fix unrelated failures unless explicitly asked.
- Report exact failing command/output and proposed next fix.

## Done criteria

- Contract alignment confirmed against `api-spec.md`.
- Schema assumptions verified in `db-schema.md`.
- Error codes mapped to existing catalog.
- Validation outcomes recorded.
