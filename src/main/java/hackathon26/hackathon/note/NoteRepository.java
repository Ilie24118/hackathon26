package hackathon26.hackathon.note;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

	private static final RowMapper<Note> NOTE_ROW_MAPPER = (rs, rowNum) -> new Note(
			rs.getLong("id"),
			rs.getString("title"),
			rs.getString("content"));

	private final JdbcTemplate jdbcTemplate;

	public NoteRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Note> findAll() {
		return jdbcTemplate.query("SELECT id, title, content FROM notes ORDER BY id", NOTE_ROW_MAPPER);
	}

	public Optional<Note> findById(Long id) {
		return jdbcTemplate.query("SELECT id, title, content FROM notes WHERE id = ?", NOTE_ROW_MAPPER, id)
				.stream()
				.findFirst();
	}

	public Note save(Note note) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO notes (title, content) VALUES (?, ?)",
					new String[] { "id" });
			ps.setString(1, note.getTitle());
			ps.setString(2, note.getContent());
			return ps;
		}, keyHolder);
		Number key = keyHolder.getKey();
		if (key == null) {
			throw new IllegalStateException("Insert failed to return a generated id");
		}
		note.setId(key.longValue());
		return note;
	}

	public boolean update(Long id, Note note) {
		return jdbcTemplate.update("UPDATE notes SET title = ?, content = ? WHERE id = ?",
				note.getTitle(), note.getContent(), id) > 0;
	}

	public boolean deleteById(Long id) {
		return jdbcTemplate.update("DELETE FROM notes WHERE id = ?", id) > 0;
	}
}
