# cloud-itonami-isco-9212

Open Occupation Blueprint for **ISCO-08 9212**: Livestock Farm Labourers.

This repository designs a forkable OSS business for a livestock-farm
scheduling and logistics coordination practice: a farm scheduling and
supply-coordination robot manages crew/task records under a governor-gated
actor, so a livestock-farm crew keeps its own operating records instead of
renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/livestockfarm/` implements the
`LivestockFarmActor` as a `langgraph.graph/state-graph`
(`livestockfarm.actor`) wired to a `Livestock Farm Labourer Advisor`
(`livestockfarm.advisor`) and an independent `LivestockFarmGovernor`
(`livestockfarm.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt) +->
:hold (:hard?)`. 24 tests / 52 assertions green (`clojure -M:test`). HARD
invariants (always hold, never overridable): worker provenance, farm
provenance, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist (`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any proposal
that would directly finalize a livestock-handling-execution decision
(e.g. approving a specific handling or restraint procedure) *or* a
farm-safety-clearance decision (e.g. declaring a farm cleared for
safety), or that would override a farm safety supervisor's judgment.
Always-escalate paths (human sign-off regardless of confidence, mapping
this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above the
registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a farm scheduling/logistics
coordination robot performs crew scheduling, feeding/care-log/progress-
record logging and feed/farm-supplies procurement coordination for a
livestock-farm crew, under an actor that proposes actions and an
independent **LivestockFarmGovernor** that gates them. The governor never
dispatches hardware itself, never handles livestock on the farm floor
itself, and never finalizes a livestock-handling-execution decision or a
farm-safety-clearance decision, and never overrides a farm safety
supervisor's judgment; `:high`/`:safety-critical` actions (such as a
flagged animal-behavior-hazard/outdoor-exposure/farm-equipment concern, or
an above-threshold supply order) require human sign-off. **This actor
coordinates FARM SCHEDULING/LOGISTICS ONLY — it never handles livestock or
performs farm labour work itself and never makes a farm-safety-clearance
decision itself.**

## Core Contract

```text
worker roster + farm registration + safety-reporting policy
        |
        v
Livestock Farm Labourer Advisor -> LivestockFarmGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize a livestock-handling-execution decision, finalize a farm-
safety-clearance decision (e.g. declaring a farm cleared for safety),
override a farm safety supervisor's judgment, suppress an operating
record, or disclose sensitive data without governor approval and audit
evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `9212`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
