# Day 4 Status (Supply Chain + DoD)

## Supply-chain inventory
- Command run: `./mvnw dependency:tree` in `fx-app-spring`.
- Output captured in `fx-app-spring/target/dependency-tree.txt`.
- Approximate shipped dependency surface from the tree output: 93 dependency lines (excluding project root line).
- Key point: many runtime behaviors come from transitive dependencies, not only direct `pom.xml` entries.

## Two transitive traces (who pulled whom)
- `org.apache.tomcat.embed:tomcat-embed-el:10.1.30` <- pulled by `org.springframework.boot:spring-boot-starter-validation:3.3.4`.
- `com.github.docker-java:docker-java-transport-zerodep:3.4.2` <- pulled by `org.testcontainers:testcontainers:1.21.4`.

## Manual advisory lookup
- Lookup target: `org.yaml:snakeyaml:2.2`.
- Source checked: GitHub Advisory Database search (`snakeyaml 2.2`).
- Result at lookup time: 0 advisory results matched this exact query.
- Note: "no result" is time-bound; this must be rechecked continuously (automated by Dependabot + CI scan below).

## CI guards and limits
- Guarded now:
  - `build` job runs `mvn -B verify` for unit + web slice + integration tests.
  - `lint` job runs `mvn -B validate` in parallel.
  - `dependency-check` job runs OWASP dependency scan with `continue-on-error: true` to surface findings without blocking all PRs.
- Not fully guarded in CI:
  - No full browser/user end-to-end flow.
  - No runtime docker-compose smoke test in CI.

## Dependabot rule
- Dependabot enabled/configured for Maven in `/fx-app-spring` (daily checks).
- Team rule: security/dependency PRs are reviewed within 1 working day; if CI is green and change is low risk, merge same day.

## Honesty checks (fast tier vs full tier)
- MySQL-off honesty check:
  - `./mvnw test` baseline remained green: 29 run, 0 failures, 1 skipped.
  - Interpretation: unit/slice tier is not secretly coupled to a local MySQL service.
- Docker-off silent-skip check:
  - To complete this check exactly, fully quit Docker Desktop and run `./mvnw verify`.
  - Expected trap signal: `RateRepositoryIT` shows skipped tests while overall build can remain green.
  - In this session, Docker remained reachable, so ITs executed (`Skipped: 0`).

## Definition of Done (Day 4)
A change is done only when all of the following hold:

1. From `fx-app-spring/`, `./mvnw verify` is green with unit, slice, and integration tests, and test summaries are read for wrong skips; plus `docker compose up` serves `/api/rates` with expected data.
2. New behavior has tests at the correct altitude:
   - calculation changes -> unit test,
   - API contract/validation -> web slice test,
   - SQL/query/schema change -> integration (`*IT`) test.
3. A teammate reviews the PR and runs `./mvnw verify` on the branch before approval.
4. Changes merge to `main` only through PR (no direct push).
5. `main` stays green after merge, and a fresh clone can build and verify successfully.

Not done: failed tests, wrongly skipped critical tests, unreviewed merge, or "works only on one laptop" outcomes.
