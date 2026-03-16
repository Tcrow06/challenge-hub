---
name: Agent Asset Authoring
description: Keep custom agents, prompts, skills, and instruction files consistent with Copilot conventions.
applyTo: ".github/agents/**/*.md,.github/prompts/**/*.prompt.md,.github/skills/**/SKILL.md,.github/instructions/**/*.instructions.md"
---

# Agentic asset conventions

- Keep frontmatter valid and minimal (`name`, `description`, `tools`, `agent`, `applyTo`, `handoffs` as needed).
- Keep names stable for reusable assets referenced by other files.
- Use least-privilege tools whenever possible.
- Keep one responsibility per file (persona, capability, or workflow intent).
- Avoid duplicating global guidance from `.github/copilot-instructions.md` unless scoping is required.

# File-specific rules

- Agents: use `.agent.md` and define focused personas.
- Prompts: use `.prompt.md` and optimize for one reusable slash-command workflow.
- Skills: keep `SKILL.md` in lowercase-hyphenated folder names.
- Instructions: keep `.instructions.md` files scoped by clear `applyTo` patterns.
