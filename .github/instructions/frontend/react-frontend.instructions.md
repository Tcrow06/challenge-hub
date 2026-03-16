---
name: Frontend React Rules
description: Apply feature-based React and typed API integration conventions for frontend changes.
applyTo: "challenge-hub-frontend/src/**/*.{ts,tsx},challenge-hub-frontend/src/assets/**/*.css"
---

# Frontend delivery rules

- Keep code in feature-based folders under `src/features/*`.
- Route HTTP calls through `src/api/client.ts` and feature API modules only.
- Use TanStack Query for server state and Zustand only for global app state.
- Keep TypeScript strict and avoid `any`.
- Ensure loading, success, error, and empty states are explicit in data-driven UI.
- Reuse existing components/hooks before introducing new patterns.

# Contract alignment references

- [api-spec.md](../../../instructions/api-spec.md)
- [frontend-state.md](../../../instructions/frontend-state.md)
- [workflows.md](../../../instructions/workflows.md)
- [requirements.md](../../../instructions/requirements.md)
- [websocket-spec.md](../../../instructions/websocket-spec.md) for realtime behavior
