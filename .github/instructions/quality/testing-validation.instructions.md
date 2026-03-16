---
name: Testing Validation Rules
description: Enforce focused, risk-based validation and concise pass/fail reporting for test-related changes.
applyTo: "challenge-hub-backend/src/test/**/*.java,challenge-hub-frontend/src/**/*.test.ts,challenge-hub-frontend/src/**/*.test.tsx,challenge-hub-frontend/src/**/*.spec.ts,challenge-hub-frontend/src/**/*.spec.tsx"
---

# Testing rules

- Prefer targeted tests for changed scope before broad/full-suite execution.
- Keep tests deterministic and isolated from unrelated external state.
- Match existing test style and naming conventions in each module.
- Avoid rewriting production code solely to satisfy unrelated failing tests.
- Report validation outcome with clear PASS/FAIL and blocking findings first.

# Validation references

- [requirements.md](../../../instructions/requirements.md)
- [workflows.md](../../../instructions/workflows.md)
- [api-spec.md](../../../instructions/api-spec.md)
