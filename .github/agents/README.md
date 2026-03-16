# Custom Agents

This directory defines workspace-level custom agents for GitHub Copilot.

## Agents

- `delivery-orchestrator.agent.md`
  - Planning and routing agent.
  - Uses read-only tools and proposes handoffs.
- `backend.agent.md`
  - Backend implementation specialist (Spring Boot/Postgres/Mongo/Redis).
- `frontend.agent.md`
  - Frontend implementation specialist (React/TypeScript/TanStack Query/Zustand).
- `qa.agent.md`
  - Risk-focused validation specialist.

## Recommended workflow

1. Start with `delivery-orchestrator` for scope classification and plan.
2. Handoff to `backend` and/or `frontend` for implementation.
3. Handoff to `qa` for validation gate before merge.

## Naming conventions

- File names follow `<name>.agent.md`.
- Keep names stable because prompt files can reference agent names.

## Design principles

- Keep each agent focused on one responsibility.
- Use least-privilege tools where practical.
- Reuse repository standards from `.github/copilot-instructions.md`.
