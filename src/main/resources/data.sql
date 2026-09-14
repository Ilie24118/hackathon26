-- Demo seed: runs after Hibernate has created the schema (spring.jpa.defer-datasource-initialization),
-- but inserts only when the notes table is completely empty, so real data is never
-- overwritten or duplicated. To reseed the demo rows: DELETE FROM notes (or drop the
-- table) and restart/deploy.

INSERT INTO notes (title, content)
SELECT seed.title, seed.content
FROM (VALUES
    ('Welcome to the hackathon', 'You are seeing this row because the notes table was empty at startup. It will not come back after you delete it.'),
    ('How to add a new entity', 'Copy the note package: Lombok model + JdbcTemplate repository (RowMapper, keyholder insert) + REST controller with 404s.'),
    ('Endpoints to try', 'GET /api/notes, GET /api/notes/{id}, POST /api/notes, PUT /api/notes/{id}, DELETE /api/notes/{id}.'),
    ('Database cheat sheet', 'dev: Docker Postgres on port 5050. test: docker-compose.test.yml on 5435. prod: Neon via PG* env vars.'),
    ('Deploy checklist', 'Push to main, Vercel builds Dockerfile.vercel, then check GET /api/health shows database:up.')
) AS seed(title, content)
WHERE NOT EXISTS (SELECT 1 FROM notes);
