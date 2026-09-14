package hackathon26.hackathon.note;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUpTable() {
		jdbcTemplate.execute("DROP TABLE IF EXISTS notes");
		jdbcTemplate.execute("""
				CREATE TABLE notes (
					id      BIGSERIAL PRIMARY KEY,
					title   VARCHAR(200) NOT NULL,
					content TEXT NOT NULL
				)
				""");
	}

	@Test
	void fullCrudRoundTrip() throws Exception {
		mockMvc.perform(get("/api/notes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		mockMvc.perform(post("/api/notes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "First", "content": "Hello hackathon"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.title").value("First"))
				.andExpect(jsonPath("$.content").value("Hello hackathon"));

		long id = jdbcTemplate.queryForObject("SELECT id FROM notes", Long.class);

		mockMvc.perform(get("/api/notes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(put("/api/notes/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Updated", "content": "Edited"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.title").value("Updated"));

		mockMvc.perform(get("/api/notes/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Edited"));

		mockMvc.perform(delete("/api/notes/" + id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/notes/" + id))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/notes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void healthEndpointReportsDatabaseUp() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"))
				.andExpect(jsonPath("$.database").value("up"));
	}

}
