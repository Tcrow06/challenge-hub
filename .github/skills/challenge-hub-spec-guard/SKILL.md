---
name: challenge-hub-spec-guard
description: Cross-document consistency guard for API, schema, workflows, and error codes. Use before finalizing implementation.
license: Proprietary - internal repository use only
---

Use this skill whenever a change may impact API contracts, state flows, or business rules.

## Inputs to compare

- `instructions/api-spec.md`
- `instructions/db-schema.md`
- `instructions/workflows.md`
- `instructions/requirements.md`
- Changed files in backend and frontend

## Consistency checks

1. API route and payloads match documented contract names and shapes.
2. Database assumptions match schema constraints and indexes.
3. Workflow side effects match documented state transitions.
4. Error handling maps to existing error catalog entries.
5. Frontend API types and runtime assumptions align with backend responses.

## Reporting format

- `Aligned`: confirmed consistent.
- `Mismatch`: include file paths, expected contract, actual implementation.
- `Unknown`: information missing; list what must be clarified.

## Guardrail

- Never invent undocumented contract fields or error codes.
