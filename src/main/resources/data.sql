-- Demo data reset: runs on EVERY boot (after Hibernate, via
-- spring.jpa.defer-datasource-initialization) and wipes the notes table back
-- to these 5 rows. On Vercel that includes every deploy AND every cold start
-- (scale-to-zero) — user-entered notes are disposable by design.

TRUNCATE TABLE notes RESTART IDENTITY;

INSERT INTO notes (title, content) VALUES
    ('Welcome to the hackathon', 'The notes table resets to these demo rows on every restart. Anything you add is disposable demo data.'),
    ('How to add a new entity', 'Copy the note package: Lombok @Entity model + JdbcTemplate repository (RowMapper, keyholder insert) + REST controller with 404s.'),
    ('Endpoints to try', 'GET /api/notes, GET /api/notes/{id}, POST /api/notes, PUT /api/notes/{id}, DELETE /api/notes/{id}.'),
    ('Database cheat sheet', 'dev: Docker Postgres on port 5050. test: docker-compose.test.yml on 5435. prod: Neon via PG* env vars.'),
    ('Deploy checklist', 'Push to main, Vercel builds Dockerfile.vercel, then check GET /api/health shows database:up.');
