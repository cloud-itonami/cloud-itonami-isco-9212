# Operator Guide

## First Deployment

1. Define the operator's farm coverage and crew intake process.
2. Define consent and purpose categories for worker/farm records.
3. Run synthetic operating cases (work-log entry, crew-operation
   scheduling, supply coordination, safety-concern flagging).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions
   (all flagged safety concerns, above-threshold supply orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (animal-behavior hazard,
  outdoor-exposure hazard, farm-equipment hazard)
- provenance for all operating records (worker and farm both
  independently registered)
- human review for high-risk cases
- audit export for all gated actions
- a hard, unconditional block on any attempt to route a livestock-
  handling-execution decision, a farm-safety-clearance decision (e.g.
  declaring a farm cleared for safety), or a farm-safety-supervisor
  override decision, through this actor — those decisions stay a farm
  safety supervisor's exclusive authority end to end

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks escalate to
humans, and that no deployment configuration can route a livestock-
handling-execution decision, a farm-safety-clearance decision, or a
farm-safety-supervisor judgment override through this actor.
