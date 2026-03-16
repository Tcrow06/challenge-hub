# Path-specific Instructions

This directory contains scoped instruction files for Copilot.

Each file uses `.instructions.md` format with `applyTo` glob patterns so rules are auto-applied when matching files are in scope.

## Included scopes

- `backend/spring-backend.instructions.md`
- `frontend/react-frontend.instructions.md`
- `contracts/api-contract-alignment.instructions.md`
- `quality/testing-validation.instructions.md`
- `ai/agent-assets.instructions.md`

## Authoring conventions

- Keep instructions short and concrete.
- Prefer one responsibility per file.
- Use workspace-relative glob patterns in `applyTo`.
- Update this index when adding/removing instruction files.
