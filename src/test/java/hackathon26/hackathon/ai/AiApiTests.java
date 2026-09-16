package hackathon26.hackathon.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiApiTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private WebsiteScraper scraper;

	@MockitoBean
	private OpenRouterClient openRouter;

	@BeforeEach
	void setUpTables() {
		jdbc.execute("DROP TABLE IF EXISTS registry_records");
		jdbc.execute("DROP TABLE IF EXISTS business_evidence");
		jdbc.execute("DROP TABLE IF EXISTS review_decisions");
		jdbc.execute("DROP TABLE IF EXISTS verified_businesses");
		jdbc.execute("""
				CREATE TABLE registry_records (
					registry_number VARCHAR(20) PRIMARY KEY,
					parent_registry_number VARCHAR(20),
					record_type VARCHAR(20),
					official_name VARCHAR(300),
					trade_name VARCHAR(300),
					search_name VARCHAR(300),
					legal_status VARCHAR(100),
					legal_form VARCHAR(100),
					kbo_street VARCHAR(200),
					kbo_house_number VARCHAR(20),
					kbo_bus_number VARCHAR(20),
					kbo_postcode VARCHAR(10),
					municipality VARCHAR(100),
					address_street VARCHAR(200),
					address_house_number VARCHAR(20),
					address_bus_number VARCHAR(20),
					address_postcode VARCHAR(10),
					activity_description VARCHAR(300),
					registration_date VARCHAR(10),
					start_date VARCHAR(10),
					cessation_date VARCHAR(10),
					latitude VARCHAR(30),
					longitude VARCHAR(30),
					phone VARCHAR(50),
					email VARCHAR(200),
					annual_account_url VARCHAR(500)
				)
				""");
		jdbc.execute("""
				CREATE TABLE business_evidence (
					id BIGSERIAL PRIMARY KEY,
					registry_number VARCHAR(20) NOT NULL,
					source_type VARCHAR(40) NOT NULL,
					source_url VARCHAR(500),
					observation TEXT NOT NULL,
					observed_on DATE NOT NULL,
					captured_at TIMESTAMPTZ NOT NULL,
					officer VARCHAR(200)
				)
				""");
		jdbc.execute("""
				CREATE TABLE review_decisions (
					id BIGSERIAL PRIMARY KEY,
					registry_number VARCHAR(20) NOT NULL,
					status VARCHAR(30) NOT NULL,
					note TEXT NOT NULL,
					officer VARCHAR(200),
					decided_at TIMESTAMPTZ NOT NULL
				)
				""");
		jdbc.execute("""
				CREATE TABLE verified_businesses (
					registry_number VARCHAR(20) PRIMARY KEY,
					verified_status VARCHAR(30),
					note TEXT,
					officer VARCHAR(200),
					verified_at TIMESTAMPTZ
				)
				""");
		jdbc.update("INSERT INTO registry_records (registry_number,record_type,official_name,legal_status,kbo_street,kbo_house_number,kbo_postcode,municipality,address_street,address_house_number,address_postcode,start_date,latitude,longitude) VALUES ('2372012066','LEGAL_ENTITY','Data 2 Strategy','Normale toestand','Brechtsebaan','30','2900','Schoten','Brechtsebaan','30','2900','2026-01-15','51.26','4.50')");
		when(scraper.scrape(anyString())).thenReturn("Welcome to Data 2 Strategy. Opening hours: Mon-Fri 9-17. Brechtsebaan 30, 2900 Schoten.");
		when(openRouter.isConfigured()).thenReturn(true);
		when(openRouter.model()).thenReturn("inclusionai/ling-3.0-flash-sante:free");
		when(openRouter.complete(anyString())).thenReturn(new OpenRouterClient.Completion("Yes, the site shows an active business.", "The site lists opening hours and an address."));
	}

	@Test
	void analyzeScrapesSiteSavesAiAnswerAsEvidence() throws Exception {
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"https://www.data2strategy.be","registryNumber":"2372012066"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://www.data2strategy.be"))
				.andExpect(jsonPath("$.answer").value("Yes, the site shows an active business."))
				.andExpect(jsonPath("$.reasoning").value("The site lists opening hours and an address."))
				.andExpect(jsonPath("$.model").value("inclusionai/ling-3.0-flash-sante:free"))
				.andExpect(jsonPath("$.evidence.sourceType").value("AI_CHECK"))
				.andExpect(jsonPath("$.evidence.officer").value("AI Assistant"));

		mockMvc.perform(get("/api/businesses/2372012066"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.evidence[0].sourceType").value("AI_CHECK"))
				.andExpect(jsonPath("$.evidence[0].sourceUrl").value("https://www.data2strategy.be"));

		mockMvc.perform(get("/api/businesses").param("signal", "EVIDENCE_REVIEW"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].registryNumber").value("2372012066"));
	}

	@Test
	void analyzeSupportsCustomQuestionWithoutRegistryNumber() throws Exception {
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"https://www.data2strategy.be","question":"What services does this business offer?"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.question").value("What services does this business offer?"))
				.andExpect(jsonPath("$.evidence").value((Object) null));
	}

	@Test
	void analyzeRequiresValidHttpUrl() throws Exception {
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"ftp://example.com"}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"question":"hi"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void analyzeReturns404ForUnknownRegistryNumber() throws Exception {
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"https://www.data2strategy.be","registryNumber":"0123456789"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void analyzeMapsScrapeAndAiFailuresTo502() throws Exception {
		doThrow(new IllegalStateException("Unable to fetch website: connection timed out")).when(scraper).scrape(anyString());
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"https://down.example.com"}
								"""))
				.andExpect(status().isBadGateway());

		doReturn("some text").when(scraper).scrape(anyString());
		doThrow(new IllegalStateException("OpenRouter request failed: 429")).when(openRouter).complete(anyString());
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"https://www.data2strategy.be"}
								"""))
				.andExpect(status().isBadGateway());
	}

	@Test
	void analyzeReturns503WhenApiKeyMissing() throws Exception {
		when(openRouter.isConfigured()).thenReturn(false);
		mockMvc.perform(post("/api/ai/analyze")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"url":"https://www.data2strategy.be"}
								"""))
				.andExpect(status().isServiceUnavailable());
	}
}
