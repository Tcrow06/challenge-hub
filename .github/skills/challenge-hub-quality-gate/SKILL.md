---
name: challenge-hub-quality-gate
description: Pre-merge quality gate for Challenge Hub. Use for risk-based validation, regression checks, and concise PASS/FAIL reporting.
license: Proprietary - internal repository use only
---

Use this skill near the end of implementation, before merge or handoff.

## Validation sequence

1. Identify impacted scope (backend, frontend, or full-stack).
2. Run focused checks around changed files first.
3. Run broader checks only after focused checks pass.
4. Re-check critical user flow impacted by the change.

## Local environment hints

- Infra services can be started with the workspace task `infra:up`.
- Backend dev run task: `be:run`.
- Frontend dev run task: `fe:run`.
- Combined dev task: `dev:run`.

## Expected output

- Final verdict: `PASS` or `FAIL`.
- Blocking findings first with concrete reproduction steps.
- Non-blocking findings separately.
- List what was validated and what was not validated.

## Scope guardrail

- Avoid broad refactors during validation.
- Do not resolve unrelated failures unless explicitly requested.
