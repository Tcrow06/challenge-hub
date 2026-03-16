---
name: create-instruction-file
description: Generate a scoped .instructions.md file under .github/instructions with valid applyTo patterns.
agent: agent
tools: ["read", "search", "edit", "problems"]
argument-hint: "topic=..., folder=..., file=..., applyTo=..."
---

Create one new instruction file for this workspace.

Inputs:

- topic: ${input:topic:Describe the convention or guardrail to enforce}
- folder: ${input:folder:backend|frontend|contracts|quality|ai}
- file: ${input:file:lowercase-hyphen-name}
- applyTo: ${input:applyTo:workspace-relative glob patterns}

Required actions:

1. Create `.github/instructions/<folder>/<file>.instructions.md`.
2. Use YAML frontmatter with `name`, `description`, and `applyTo`.
3. Write concise, actionable rules in markdown body.
4. Keep rules scoped to the target files only.
5. If the new file introduces a new scope or file not listed, update `.github/instructions/README.md`.
6. Update `.github/agentic-development.md` only if directory layout examples become outdated.

Content quality requirements:

- Avoid duplicating global instructions from `.github/copilot-instructions.md` unless scope-specific.
- Prefer clear do/don't bullets and required references.
- Keep naming stable and consistent with existing repository conventions.

Final output:

- List changed files.
- Show the chosen `applyTo` pattern.
- Mention any assumptions.
