package hackathon26.hackathon.health;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping
	public Map<String, String> health() {
		String database;
		try {
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			database = "up";
		}
		catch (Exception ex) {
			database = "down";
		}
		return Map.of("status", "ok", "database", database);
	}
}
