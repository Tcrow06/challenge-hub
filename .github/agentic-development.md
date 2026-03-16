# Agentic Development Guide

This repository is structured for reusable Copilot workflows based on:

- Custom agents (`.github/agents`)
- Agent skills (`.github/skills`)
- Prompt files (`.github/prompts`)
- Path-specific instructions (`.github/instructions`)

## Directory layout

```
.github/
  copilot-instructions.md
  agentic-development.md
  agents/
    README.md
    delivery-orchestrator.agent.md
    backend.agent.md
    frontend.agent.md
    qa.agent.md
  skills/
    README.md
    challenge-hub-backend-delivery/
      SKILL.md
    challenge-hub-frontend-delivery/
      SKILL.md
    challenge-hub-spec-guard/
      SKILL.md
    challenge-hub-quality-gate/
      SKILL.md
  prompts/
    README.md
    plan-change.prompt.md
    implement-backend.prompt.md
    implement-frontend.prompt.md
    qa-gate.prompt.md
    full-delivery.prompt.md
    create-instruction-file.prompt.md
  instructions/
    README.md
    backend/
      spring-backend.instructions.md
    frontend/
      react-frontend.instructions.md
    contracts/
      api-contract-alignment.instructions.md
    quality/
      testing-validation.instructions.md
    ai/
      agent-assets.instructions.md
```

## Recommended operating model

1. Start with `delivery-orchestrator` or `/plan-change`.
2. Execute implementation via `backend` and/or `frontend` agents.
3. Run validation via `qa` agent or `/qa-gate`.
4. Use `.github/instructions/*.instructions.md` files for auto-applied, path-scoped guardrails.
5. Use spec and quality skills as reusable context layers.

## Why this structure

- Separates persistent personas (agents) from reusable capabilities (skills).
- Keeps one-off task recipes in prompts.
- Reduces repeated prompting and improves consistency.
- Supports handoff-based multi-agent workflow.

## Maintenance rules

- Keep agent names stable once prompt files depend on them.
- Keep skill names lowercase and hyphenated.
- Keep prompts focused on a single workflow intent.
- Keep instruction files narrowly scoped with accurate `applyTo` patterns.
- Update this guide when adding/removing agents, skills, prompts, or instructions.
