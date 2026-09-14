# AGENTS.md

## Project
Single-module Spring Boot 4.1.1 web app (Java 21, Lombok, plain JDBC — no JPA starter).
Always use the Gradle wrapper: `./gradlew`. No lint/format/typecheck tooling; verification is compile + tests.
Deployed to Vercel as a Docker project (`Dockerfile.vercel`), which supplies `PORT` and the `PG*` env vars (Neon Postgres).

Databases by environment:
- dev (`bootRun`): Docker Postgres from `docker-compose.yml` (host port 5050, spring/spring/hackathon).
  Boot's docker-compose support auto-starts it and the service connection OVERRIDES datasource properties.
- test: Docker Postgres from `docker-compose.test.yml` (host port 5435, springtest/springtest/hackathon-test),
  wired via the `testAndDevelopmentOnly` dependency + `src/test/resources/application-test.yaml` (profile `test`).
- prod (Vercel): Neon Postgres via `PGHOST`/`PGDATABASE`/`PGUSER`/`PGPASSWORD` — see `application-prod.yaml`.

## Commands
- `./gradlew bootRun` — dev server at http://localhost:8080. Requires Docker running. No env vars needed.
- `./gradlew test` — requires Docker running; no env vars needed (tests use the test compose DB).
- Single test: `./gradlew test --tests 'hackathon26.hackathon.note.NoteApiTests'`

## Gotchas
- Boot 4 starter names differ from Boot 3: `spring-boot-starter-webmvc` (not `-web`), plus
  `-webmvc-test`, `-jdbc-test`, `-cache-test`. Don't rename them.
- Boot 4 moved test annotations: `@AutoConfigureMockMvc` lives in
  `org.springframework.boot.webmvc.test.autoconfigure` (NOT the Boot 3 package
  `org.springframework.boot.test.autoconfigure.web.servlet`).
- Postgres + `GeneratedKeyHolder`: request the key column explicitly —
  `prepareStatement(sql, new String[] { "id" })`. With `Statement.RETURN_GENERATED_KEYS` PgJDBC
  returns ALL columns and `KeyHolder.getKey()` fails with "multiple keys".
- Profiles: base `application.yaml` has no active profile; tests use `test` via `@ActiveProfiles`;
  the Vercel image sets `SPRING_PROFILES_ACTIVE=prod`. Don't hardcode `spring.profiles.active` again.
- Schema: `src/main/resources/schema.sql` is auto-applied on every boot (`spring.sql.init.mode=always`)
  against whichever DB is active (dev Docker, test Docker, Neon on Vercel). Keep its statements
  idempotent (`CREATE TABLE IF NOT EXISTS`); add new tables there — no Flyway/Liquibase by choice.
- Integration tests create their own tables anyway (see `NoteApiTests.setUpTable`); copy that
  pattern for new entities so tests stay isolated from schema/seed state.
- `.env.local` holds live Neon credentials and is gitignored — never commit, echo, or paste it.
- JDK 21 must be installed locally — `settings.gradle` has no toolchain auto-provision resolver.
- `Dockerfile.vercel` builds with the wrapper (Gradle 9.7.1), matching local builds.

## Patterns
- `note` package = copy-me CRUD example: Lombok model + `JdbcTemplate` repository (RowMapper,
  keyholder insert) + REST controller with 404s via `ResponseStatusException`.
- `GET /api/health` returns `{"status":"ok","database":"up|down"}` — use as deploy smoke URL.

## Entry point
`src/main/java/hackathon26/hackathon/HackathonApplication.java`
