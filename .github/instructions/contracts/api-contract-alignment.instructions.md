---
name: API Contract Alignment
description: Guard contract consistency across backend DTO/controllers and frontend API/type layers.
applyTo: "challenge-hub-backend/src/main/java/com/challengehub/dto/**/*.java,challenge-hub-backend/src/main/java/com/challengehub/controller/**/*.java,challenge-hub-frontend/src/features/**/api/**/*.ts,challenge-hub-frontend/src/types/**/*.ts"
---

# Contract guardrails

- Do not invent endpoints, payload fields, or error codes not present in the specs.
- Keep response and request shapes aligned with documented DTO contracts.
- Keep error handling aligned with `ApiResponse` wrapper and existing error catalog entries.
- Ensure frontend API functions and TS types match backend payload names and nullability.
- When contract changes are required, update docs first and then implementation.

# Mandatory references

- [api-spec.md](../../../instructions/api-spec.md)
- [requirements.md](../../../instructions/requirements.md)
- [workflows.md](../../../instructions/workflows.md)
- [db-schema.md](../../../instructions/db-schema.md)
