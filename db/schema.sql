-- Reference DDL, applied manually (no schema tooling by choice).
-- Dev DB:
--   docker compose exec -T postgres_hackathon_db psql -U spring -d hackathon < db/schema.sql
-- Test DB: not needed - tests create their own tables (see NoteApiTests.setUpTable).

CREATE TABLE IF NOT EXISTS notes (
    id      BIGSERIAL PRIMARY KEY,
    title   VARCHAR(200) NOT NULL,
    content TEXT NOT NULL
);
