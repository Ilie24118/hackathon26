package hackathon26.hackathon.business;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {
    private final BusinessRepository repository;
    public BusinessService(BusinessRepository repository) { this.repository = repository; }
    public List<Map<String,Object>> list(String query, String signal, String decision) {
        Map<String,List<Evidence>> evidence = repository.evidenceByRecord();
        Map<String,List<ReviewDecision>> decisions = repository.decisionsByRecord();
        return repository.findAll().stream().map(r -> summary(r, evidence.getOrDefault(r.getRegistryNumber(), List.of()), decisions.getOrDefault(r.getRegistryNumber(), List.of())))
                .filter(r -> matches(r,query,signal,decision)).toList();
    }

    public Map<String,Object> meta() {
        Map<String,List<Evidence>> evidence = repository.evidenceByRecord();
        Map<String,List<ReviewDecision>> decisions = repository.decisionsByRecord();
        List<Map<String,Object>> summaries = repository.findAll().stream().map(r -> summary(r, evidence.getOrDefault(r.getRegistryNumber(), List.of()), decisions.getOrDefault(r.getRegistryNumber(), List.of()))).toList();
        long pending = summaries.stream().filter(s -> "PENDING".equals(s.get("decisionStatus"))).count();
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("municipality", "Schoten");
        m.put("records", repository.countRecords());
        m.put("establishments", summaries.stream().filter(s -> "ESTABLISHMENT".equals(s.get("recordType"))).count());
        m.put("legalEntities", summaries.stream().filter(s -> "LEGAL_ENTITY".equals(s.get("recordType"))).count());
        m.put("needsReview", pending);
        m.put("possiblyInactive", summaries.stream().filter(s -> "POSSIBLY_INACTIVE".equals(s.get("signal"))).count());
        m.put("addressChecks", summaries.stream().filter(s -> "ADDRESS_CHECK".equals(s.get("signal"))).count());
        m.put("newRegistrations", summaries.stream().filter(s -> "NEW_REGISTRATION".equals(s.get("signal"))).count());
        m.put("evidenceItems", repository.countEvidence());
        m.put("verified", repository.verified().size());
        m.put("snapshotRetrievedOn", "2026-09-07");
        m.put("sourceAttribution", "publieke KBO gegevens, verrijkt met adressen uit het Vlaamse Adressenregister.");
        m.put("partialSample", true);
        m.put("nextPageUrl", "https://geo.api.vlaanderen.be/VKBO/ogc/features/v1/collections/Vkbo/items?f=application%2Fjson&limit=1000&filter=KBO_Gemeente%3D%27Schoten%27&filter-lang=cql2-text&startIndex=1000");
        return m;
    }

    public Map<String,Object> detail(String id) {
        RegistryRecord r = repository.find(id).orElseThrow(() -> new IllegalArgumentException("Business record not found"));
        List<Evidence> evidence = repository.evidence(id);
        List<ReviewDecision> decisions = repository.decisions(id);
        Map<String,Object> result = summary(r, evidence, decisions);
        result.put("record", r);
        result.put("evidence", evidence);
        result.put("decisions", decisions);
        result.put("parent", repository.findParent(r.getParentRegistryNumber()).orElse(null));
        result.put("establishments", repository.findChildren(id).stream().map(c -> mini(c)).toList());
        result.put("atSameAddress", repository.findAtAddress(r.getKboStreet(), r.getKboHouseNumber(), id).stream().map(this::mini).toList());
        return result;
    }

    private Map<String,Object> mini(RegistryRecord r) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("registryNumber", r.getRegistryNumber()); m.put("name", name(r)); m.put("recordType", r.getRecordType()); m.put("address", address(r));
        return m;
    }

    public Map<String,Object> summary(RegistryRecord r, List<Evidence> evidence, List<ReviewDecision> decisions) {
        ReviewDecision latest = decisions.isEmpty() ? null : decisions.getFirst();
        String decisionStatus = latest == null ? "PENDING" : latest.getStatus();
        String signal; String confidence; String reason; String proposal;
        boolean inactive = present(r.getCessationDate()) || (present(r.getLegalStatus()) && !"Normale toestand".equalsIgnoreCase(r.getLegalStatus().trim()));
        boolean recent = present(r.getStartDate()) && r.getStartDate().compareTo("2025-01-01") >= 0;
        String addressReason = addressIssueReason(r);
        if (inactive) { signal="POSSIBLY_INACTIVE"; confidence="HIGH"; reason=inactivityReason(r); proposal="Check: possibly no longer active"; }
        else if (addressReason != null) { signal="ADDRESS_CHECK"; confidence="MEDIUM"; reason=addressReason; proposal="Check address match"; }
        else if (evidence.isEmpty()) {
            if (recent) { signal="NEW_REGISTRATION"; confidence="LOW"; reason="Recently registered — no evidence recorded yet."; proposal="New business — verify and welcome"; }
            else { signal="UNVERIFIED"; confidence="LOW"; reason="No dated evidence recorded yet."; proposal="Gather evidence of activity"; }
        }
        else { signal="EVIDENCE_REVIEW"; confidence="MEDIUM"; reason="Evidence recorded, awaiting an officer decision."; proposal="Review evidence and decide"; }
        if ("CONFIRMED".equals(decisionStatus)) proposal="No action — verified";
        else if ("REJECTED".equals(decisionStatus)) proposal="Rejected — no change published";
        else if ("NEEDS_FOLLOW_UP".equals(decisionStatus)) proposal="Follow up";
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("registryNumber", r.getRegistryNumber()); m.put("name", name(r)); m.put("recordType", r.getRecordType());
        m.put("address", address(r)); m.put("street", blank(r.getKboStreet())); m.put("houseNumber", blank(r.getKboHouseNumber()));
        m.put("legalStatus", blank(r.getLegalStatus()));
        m.put("parentRegistryNumber", blank(r.getParentRegistryNumber()));
        m.put("signal", signal); m.put("confidence", confidence); m.put("reason", reason); m.put("proposal", proposal);
        m.put("evidenceCount", evidence.size());
        m.put("lastObservedOn", evidence.isEmpty() ? null : evidence.getFirst().getObservedOn());
        Evidence mapsEvidence = evidence.stream().filter(e -> "GOOGLE_MAPS".equals(e.getSourceType())).findFirst().orElse(null);
        m.put("googleMapsLocation", mapsEvidence == null ? null : evidenceValue(mapsEvidence.getObservation(), "address"));
        m.put("googleMapsWebsiteDomain", mapsEvidence == null ? null : evidenceValue(mapsEvidence.getObservation(), "websiteDomain"));
        m.put("decisionStatus", decisionStatus);
        return m;
    }

    private String evidenceValue(String observation, String key) {
        if (observation == null) return null;
        String prefix = key + "=";
        int start = observation.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int end = observation.indexOf(';', start);
        String value = observation.substring(start, end < 0 ? observation.length() : end).trim();
        return value.isBlank() ? null : value;
    }

    private String inactivityReason(RegistryRecord r) {
        if (present(r.getCessationDate())) return "The register records a cessation date (" + r.getCessationDate() + ").";
        String s = blank(r.getLegalStatus()).toLowerCase();
        if (s.contains("faillissement")) return "Register status: bankruptcy (" + r.getLegalStatus().trim() + ").";
        return "Register status: in liquidation or dissolution (" + r.getLegalStatus().trim() + ").";
    }
    private String addressIssueReason(RegistryRecord r) {
        if (!present(r.getAddressStreet())) return "No Address Register match for the registered address.";
        if (!same(r.getKboStreet(), r.getAddressStreet()) || !same(r.getKboHouseNumber(), r.getAddressHouseNumber())) return "KBO and Address Register address fields differ.";
        if (outsideMunicipality(r.getLatitude(), r.getLongitude())) return "Registered coordinates fall outside the municipality.";
        return null;
    }
    private boolean outsideMunicipality(String lat, String lon) {
        try {
            double la = Double.parseDouble(lat), lo = Double.parseDouble(lon);
            return la < 51.15 || la > 51.32 || lo < 4.40 || lo > 4.60;
        } catch (Exception e) { return false; }
    }

    private boolean matches(Map<String,Object> r,String query,String signal,String decision) {
        String hay=(r.get("name")+" "+r.get("address")+" "+r.get("registryNumber")+" "+r.get("parentRegistryNumber")).toLowerCase();
        return (query==null || query.isBlank() || hay.contains(query.toLowerCase())) && (signal==null || signal.isBlank() || signal.equals(r.get("signal"))) && (decision==null || decision.isBlank() || decision.equals(r.get("decisionStatus")));
    }
    public String name(RegistryRecord r) { return present(r.getTradeName()) ? r.getTradeName() : present(r.getOfficialName()) ? r.getOfficialName() : present(r.getSearchName()) ? r.getSearchName() : r.getRegistryNumber(); }
    public String address(RegistryRecord r) { return String.join(" ", blank(r.getKboStreet()), blank(r.getKboHouseNumber()))+", "+blank(r.getKboPostcode())+" "+blank(r.getMunicipality()); }
    private boolean present(String s) { return s!=null&&!s.isBlank(); } private String blank(String s){return s==null?"":s;} private boolean same(String a,String b){return blank(a).trim().equalsIgnoreCase(blank(b).trim());}
    private String text(JsonNode p, String field) { JsonNode v = p.get(field); return v == null || v.isNull() ? "" : v.asString("").trim(); }
    private String firstNonBlank(String a, String b) { return present(a) ? a : b; }
    private String realDate(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String d = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        return d.startsWith("1900") || d.startsWith("9999") ? "" : d;
    }
}
