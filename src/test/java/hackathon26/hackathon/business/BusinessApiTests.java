package hackathon26.hackathon.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessApiTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

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
		insert("0403750127", "", "LEGAL_ENTITY", "Drukkerij L. Van Mechelen", "Vervroegde ontbinding - Vereffening (vrijwillig ontbinding)", "Constant Neutjensstraat", "45-47", "Constant Neutjensstraat", "45");
		insert("2286527055", "0403750127", "ESTABLISHMENT", "TRIXXO I", "", "Paalstraat", "73", "Paalstraat", "73");
		insert("2372012066", "", "LEGAL_ENTITY", "Data 2 Strategy", "Normale toestand", "Brechtsebaan", "30", "Brechtsebaan", "30");
	}

	private void insert(String number, String parent, String type, String name, String status, String street, String house, String arStreet, String arHouse) {
		jdbc.update("INSERT INTO registry_records (registry_number,parent_registry_number,record_type,official_name,legal_status,kbo_street,kbo_house_number,kbo_postcode,municipality,address_street,address_house_number,address_postcode,start_date,latitude,longitude) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				number, parent, type, name, status, street, house, "2900", "Schoten", arStreet, arHouse, "2900", "2026-01-15", "51.26", "4.50");
	}

	@Test
	void listShowsSignalsAndProposals() throws Exception {
		mockMvc.perform(get("/api/businesses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[?(@.registryNumber=='0403750127')].signal").value("POSSIBLY_INACTIVE"))
				.andExpect(jsonPath("$[?(@.registryNumber=='0403750127')].confidence").value("HIGH"))
				.andExpect(jsonPath("$[?(@.registryNumber=='2286527055')].recordType").value("ESTABLISHMENT"))
				.andExpect(jsonPath("$[?(@.registryNumber=='2286527055')].parentRegistryNumber").value("0403750127"))
				.andExpect(jsonPath("$[?(@.registryNumber=='2372012066')].signal").value("NEW_REGISTRATION"))
				.andExpect(jsonPath("$[?(@.registryNumber=='2372012066')].decisionStatus").value("PENDING"));
	}

	@Test
	void listFiltersBySignalAndDecision() throws Exception {
		mockMvc.perform(get("/api/businesses").param("signal", "POSSIBLY_INACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].registryNumber").value("0403750127"));
	}

	@Test
	void detailIncludesParentAndChildren() throws Exception {
		mockMvc.perform(get("/api/businesses/2286527055"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.record.officialName").value("TRIXXO I"))
				.andExpect(jsonPath("$.parent.registryNumber").value("0403750127"))
				.andExpect(jsonPath("$.parent.officialName").value("Drukkerij L. Van Mechelen"));

		mockMvc.perform(get("/api/businesses/0403750127"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.establishments[0].registryNumber").value("2286527055"));
	}

	@Test
	void detailReturns404ForUnknownRecord() throws Exception {
		mockMvc.perform(get("/api/businesses/0123456789"))
				.andExpect(status().isNotFound());
	}

	@Test
	void evidenceFlowDrivesSignalToReview() throws Exception {
		mockMvc.perform(post("/api/businesses/2372012066/evidence")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sourceType":"WEBSITE","sourceUrl":"https://example.com","observation":"Active website with Schoten address","observedOn":"2026-09-10","officer":"Marleen"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sourceType").value("WEBSITE"))
				.andExpect(jsonPath("$.officer").value("Marleen"));

		mockMvc.perform(get("/api/businesses").param("signal", "EVIDENCE_REVIEW"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].registryNumber").value("2372012066"))
				.andExpect(jsonPath("$[0].lastObservedOn").value("2026-09-10"))
				.andExpect(jsonPath("$[0].proposal").value("Review evidence and decide"));

		mockMvc.perform(post("/api/businesses/2372012066/evidence")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sourceType":"WEBSITE","observation":"","observedOn":"2026-09-10"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void confirmedDecisionPublishesToVerifiedLayerAndExport() throws Exception {
		postDecision("2372012066", "CONFIRMED", "Checked website and listing; record is accurate.");

		mockMvc.perform(get("/api/businesses").param("decision", "CONFIRMED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].proposal").value("No action — verified"));

		mockMvc.perform(get("/api/businesses/verified/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].registryNumber").value("2372012066"))
				.andExpect(jsonPath("$[0].officer").value("Tom"));

		String csv = mockMvc.perform(get("/api/businesses/verified/export"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		org.junit.jupiter.api.Assertions.assertTrue(csv.contains("2372012066"));

		postDecision("2372012066", "REJECTED", "On second thought, reject the proposal.");
		mockMvc.perform(get("/api/businesses/verified/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void decisionsRequireSupportedStatusAndNote() throws Exception {
		mockMvc.perform(post("/api/businesses/2372012066/decisions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"MAYBE","note":"x"}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/businesses/2372012066/decisions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"CONFIRMED","note":""}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void metaReportsWorkspaceCounts() throws Exception {
		mockMvc.perform(get("/api/businesses/meta"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.municipality").value("Schoten"))
				.andExpect(jsonPath("$.records").value(3))
				.andExpect(jsonPath("$.establishments").value(1))
				.andExpect(jsonPath("$.legalEntities").value(2))
				.andExpect(jsonPath("$.needsReview").value(3))
				.andExpect(jsonPath("$.possiblyInactive").value(1))
				.andExpect(jsonPath("$.sourceAttribution").isNotEmpty());
	}

	private void postDecision(String id, String status, String note) throws Exception {
		mockMvc.perform(post("/api/businesses/" + id + "/decisions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"" + status + "\",\"note\":\"" + note + "\",\"officer\":\"Tom\"}"))
				.andExpect(status().isCreated());
	}
}
