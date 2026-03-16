---
name: challenge-hub-frontend-delivery
description: Frontend delivery playbook for Challenge Hub. Use for React, TypeScript, hooks, API integration, and state-management updates.
license: Proprietary - internal repository use only
---

Use this skill when a task touches frontend code in `challenge-hub-frontend`.

## Mandatory references

- `.github/copilot-instructions.md`
- `instructions/api-spec.md`
- `instructions/requirements.md`
- `instructions/workflows.md`
- `instructions/frontend-state.md`
- `instructions/project-structure.md`
- `instructions/websocket-spec.md` for realtime paths

## Frontend delivery checklist

1. Locate target feature folder under `src/features/*` and keep changes local.
2. Keep API calls in feature API modules and `src/api/client.ts` usage.
3. Use TanStack Query for server state and Zustand only for global app state.
4. Keep strict TypeScript typing and avoid `any`.
5. Align request/response contracts with backend DTO definitions from `api-spec.md`.
6. Handle loading, success, empty, and error states explicitly.
7. Reuse shared components/hooks before creating new patterns.

## Validation guidance

- Run focused checks first (types/lint/tests around changed scope).
- Validate UI behavior and query invalidation paths where relevant.
- Do not fix unrelated failures unless explicitly asked.

## Done criteria

- Contract-compatible payload and response handling.
- Feature-based structure preserved.
- State management follows project rules.
- Validation outcomes recorded.
