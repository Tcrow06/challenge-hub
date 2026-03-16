---
name: full-delivery
description: Orchestrate end-to-end implementation from planning to QA gate using custom agents.
agent: delivery-orchestrator
tools: ["read", "search"]
argument-hint: "Describe end-to-end requirement and success criteria"
---

Drive a full delivery workflow for this task:

${input:task:Describe full-stack feature or fix}

Execution sequence:

1. Produce an implementation plan and acceptance criteria.
2. Recommend backend/frontend execution order.
3. Provide explicit handoff prompt text for each implementation agent.
4. Provide QA handoff prompt text for final validation.

Constraints:

- Keep scope minimal and aligned with repository standards.
- Ensure contracts and error handling are tied to existing docs.
