---
name: implement-frontend
description: Execute frontend implementation tasks in Challenge Hub with typed API integration and feature-based structure.
agent: frontend
argument-hint: "Describe frontend task, target feature, and acceptance criteria"
---

Implement the frontend task below:

${input:task:Describe frontend change}

Required behavior:

- Keep changes in existing feature structure.
- Use feature API modules and shared API client for all requests.
- Keep strict typing and avoid `any`.
- Align runtime behavior and types with documented API contract.

When done, provide:

1. Changed file summary
2. Validation commands run and outcomes
3. Assumptions and unresolved risks
