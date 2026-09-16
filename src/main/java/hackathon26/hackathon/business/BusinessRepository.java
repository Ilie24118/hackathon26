package hackathon26.hackathon.business;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessRepository {
    private static final String RECORD_COLUMNS = "registry_number,parent_registry_number,record_type,official_name,trade_name,search_name,legal_status,legal_form,kbo_street,kbo_house_number,kbo_bus_number,kbo_postcode,municipality,address_street,address_house_number,address_bus_number,address_postcode,activity_description,registration_date,start_date,cessation_date,latitude,longitude,phone,email,annual_account_url";
    private static final RowMapper<RegistryRecord> RECORD = (rs, n) -> new RegistryRecord(
            rs.getString("registry_number"), rs.getString("parent_registry_number"), rs.getString("record_type"),
            rs.getString("official_name"), rs.getString("trade_name"), rs.getString("search_name"),
            rs.getString("legal_status"), rs.getString("legal_form"),
            rs.getString("kbo_street"), rs.getString("kbo_house_number"), rs.getString("kbo_bus_number"),
            rs.getString("kbo_postcode"), rs.getString("municipality"),
            rs.getString("address_street"), rs.getString("address_house_number"), rs.getString("address_bus_number"),
            rs.getString("address_postcode"), rs.getString("activity_description"),
            rs.getString("registration_date"), rs.getString("start_date"), rs.getString("cessation_date"),
            rs.getString("latitude"), rs.getString("longitude"), rs.getString("phone"), rs.getString("email"),
            rs.getString("annual_account_url"));
    private final JdbcTemplate jdbc;
    public BusinessRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public int countRecords() { return jdbc.queryForObject("SELECT count(*) FROM registry_records", Integer.class); }
    public int countEvidence() { return jdbc.queryForObject("SELECT count(*) FROM business_evidence", Integer.class); }
    public int countDecided() { return jdbc.queryForObject("SELECT count(DISTINCT registry_number) FROM review_decisions", Integer.class); }

    public void saveRecords(List<RegistryRecord> records) {
        jdbc.batchUpdate("INSERT INTO registry_records (" + RECORD_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (registry_number) DO UPDATE SET parent_registry_number=excluded.parent_registry_number,record_type=excluded.record_type,"
                + "official_name=excluded.official_name,trade_name=excluded.trade_name,search_name=excluded.search_name,legal_status=excluded.legal_status,"
                + "legal_form=excluded.legal_form,kbo_street=excluded.kbo_street,kbo_house_number=excluded.kbo_house_number,kbo_bus_number=excluded.kbo_bus_number,"
                + "kbo_postcode=excluded.kbo_postcode,municipality=excluded.municipality,address_street=excluded.address_street,address_house_number=excluded.address_house_number,"
                + "address_bus_number=excluded.address_bus_number,address_postcode=excluded.address_postcode,activity_description=excluded.activity_description,"
                + "registration_date=excluded.registration_date,start_date=excluded.start_date,cessation_date=excluded.cessation_date,latitude=excluded.latitude,"
                + "longitude=excluded.longitude,phone=excluded.phone,email=excluded.email,annual_account_url=excluded.annual_account_url",
                records.stream().map(r -> new Object[] { r.getRegistryNumber(), r.getParentRegistryNumber(), r.getRecordType(),
                        r.getOfficialName(), r.getTradeName(), r.getSearchName(), r.getLegalStatus(), r.getLegalForm(),
                        r.getKboStreet(), r.getKboHouseNumber(), r.getKboBusNumber(), r.getKboPostcode(), r.getMunicipality(),
                        r.getAddressStreet(), r.getAddressHouseNumber(), r.getAddressBusNumber(), r.getAddressPostcode(),
                        r.getActivityDescription(), r.getRegistrationDate(), r.getStartDate(), r.getCessationDate(),
                        r.getLatitude(), r.getLongitude(), r.getPhone(), r.getEmail(), r.getAnnualAccountUrl() }).toList());
    }

    public List<RegistryRecord> findAll() { return jdbc.query("SELECT " + RECORD_COLUMNS + " FROM registry_records ORDER BY kbo_street, kbo_house_number, registry_number", RECORD); }
    public Optional<RegistryRecord> find(String id) { return jdbc.query("SELECT " + RECORD_COLUMNS + " FROM registry_records WHERE registry_number=?", RECORD, id).stream().findFirst(); }
    public Optional<RegistryRecord> findParent(String parent) { return parent == null || parent.isBlank() ? Optional.empty() : find(parent); }
    public List<RegistryRecord> findChildren(String parentId) { return jdbc.query("SELECT " + RECORD_COLUMNS + " FROM registry_records WHERE parent_registry_number=? ORDER BY registry_number", RECORD, parentId); }
    public List<RegistryRecord> findAtAddress(String street, String houseNumber, String excludeId) {
        List<RegistryRecord> rows = jdbc.query("SELECT " + RECORD_COLUMNS + " FROM registry_records WHERE lower(kbo_street)=lower(?) AND lower(kbo_house_number)=lower(?) ORDER BY registry_number", RECORD, street, houseNumber);
        return rows.stream().filter(r -> !r.getRegistryNumber().equals(excludeId)).toList();
    }

    public Map<String, List<Evidence>> evidenceByRecord() {
        Map<String, List<Evidence>> grouped = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM business_evidence ORDER BY observed_on DESC, id DESC", (rs, n) -> {
            Evidence e = new Evidence(rs.getLong("id"), rs.getString("registry_number"), rs.getString("source_type"), rs.getString("source_url"), rs.getString("observation"), rs.getObject("observed_on", LocalDate.class), rs.getObject("captured_at", OffsetDateTime.class), rs.getString("officer"));
            grouped.computeIfAbsent(e.getRegistryNumber(), k -> new ArrayList<>()).add(e); return e;
        });
        return grouped;
    }
    public List<Evidence> evidence(String id) { return jdbc.query("SELECT * FROM business_evidence WHERE registry_number=? ORDER BY observed_on DESC, id DESC", (rs,n) -> new Evidence(rs.getLong("id"),rs.getString("registry_number"),rs.getString("source_type"),rs.getString("source_url"),rs.getString("observation"),rs.getObject("observed_on",LocalDate.class),rs.getObject("captured_at",OffsetDateTime.class),rs.getString("officer")), id); }
    public Evidence addEvidence(String id, String type, String url, String observation, LocalDate observedOn, String officer) {
        KeyHolder keys = new GeneratedKeyHolder(); OffsetDateTime now = OffsetDateTime.now();
        jdbc.update(c -> { PreparedStatement ps=c.prepareStatement("INSERT INTO business_evidence (registry_number,source_type,source_url,observation,observed_on,captured_at,officer) VALUES (?,?,?,?,?,?,?)",new String[]{"id"}); ps.setString(1,id);ps.setString(2,type);ps.setString(3,url);ps.setString(4,observation);ps.setObject(5,observedOn);ps.setObject(6,now);ps.setString(7,officer);return ps;},keys);
        return new Evidence(keys.getKey().longValue(),id,type,url,observation,observedOn,now,officer);
    }
    public Map<String, List<ReviewDecision>> decisionsByRecord() {
        Map<String, List<ReviewDecision>> grouped = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM review_decisions ORDER BY decided_at DESC, id DESC", (rs, n) -> {
            ReviewDecision d = new ReviewDecision(rs.getLong("id"), rs.getString("registry_number"), rs.getString("status"), rs.getString("note"), rs.getString("officer"), rs.getObject("decided_at", OffsetDateTime.class));
            grouped.computeIfAbsent(d.getRegistryNumber(), k -> new ArrayList<>()).add(d); return d;
        });
        return grouped;
    }
    public List<ReviewDecision> decisions(String id) { return jdbc.query("SELECT * FROM review_decisions WHERE registry_number=? ORDER BY decided_at DESC,id DESC", (rs,n)->new ReviewDecision(rs.getLong("id"),rs.getString("registry_number"),rs.getString("status"),rs.getString("note"),rs.getString("officer"),rs.getObject("decided_at",OffsetDateTime.class)),id); }
    public ReviewDecision addDecision(String id,String status,String note,String officer) { OffsetDateTime now=OffsetDateTime.now(); KeyHolder keys=new GeneratedKeyHolder(); jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO review_decisions (registry_number,status,note,officer,decided_at) VALUES (?,?,?,?,?)",new String[]{"id"});ps.setString(1,id);ps.setString(2,status);ps.setString(3,note);ps.setString(4,officer);ps.setObject(5,now);return ps;},keys); if ("CONFIRMED".equals(status)) jdbc.update("INSERT INTO verified_businesses (registry_number,verified_status,note,officer,verified_at) VALUES (?,?,?,?,?) ON CONFLICT (registry_number) DO UPDATE SET verified_status=excluded.verified_status,note=excluded.note,officer=excluded.officer,verified_at=excluded.verified_at",id,"VERIFIED",note,officer,now); else if ("REJECTED".equals(status)) jdbc.update("DELETE FROM verified_businesses WHERE registry_number=?",id); return new ReviewDecision(keys.getKey().longValue(),id,status,note,officer,now); }
    public List<VerifiedBusiness> verified() { return jdbc.query("SELECT * FROM verified_businesses ORDER BY verified_at DESC",(rs,n)->new VerifiedBusiness(rs.getString("registry_number"),rs.getString("verified_status"),rs.getString("note"),rs.getString("officer"),rs.getObject("verified_at",OffsetDateTime.class))); }
}
