# Security Policy

This project handles livestock-farm operating workflows. Treat
vulnerabilities as potentially high impact even when the demo data is
synthetic — this domain's failure modes include physical worker-safety
risk (animal-handling hazards: kick/trample/bite injury) and outdoor
exposure/farm-equipment risk (weather, uneven terrain, equipment
condition).

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real worker, farm or operator data exposure
- authorization bypass
- LivestockFarmGovernor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach a livestock-handling-execution
  decision, a farm-safety-clearance decision (e.g. declaring a farm
  cleared for safety), or a farm-safety-supervisor override decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on worker/farm data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real worker/farm/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
