---
name: plan-change
description: Build an implementation plan grounded in Challenge Hub specs and architecture constraints.
agent: delivery-orchestrator
tools: ["read", "search"]
argument-hint: "Describe requested behavior, scope, and constraints"
---

Create an execution-ready implementation plan for the following task:

${input:task:Implement feature X with clear scope}

Requirements:

- Read and apply repository standards from `.github/copilot-instructions.md`.
- Map behavior to `instructions/api-spec.md`, `instructions/workflows.md`, and `instructions/requirements.md`.
- Keep plan minimal and dependency-aware.

Return:

1. Scope classification (backend/frontend/full-stack/qa-only)
2. Ordered implementation steps
3. Validation checklist
4. Suggested next handoff agent
