---
name: qa-gate
description: Run a risk-based QA gate and report PASS/FAIL with actionable findings.
agent: qa
argument-hint: "Provide change summary or PR context for validation"
---

Run a QA gate for the following scope:

${input:scope:Describe what changed and expected behavior}

Required output:

1. Verdict: PASS or FAIL
2. Blocking issues first (with repro and impact)
3. Non-blocking issues
4. Executed vs not-executed checks

Keep findings concise, reproducible, and scoped to changed behavior.
