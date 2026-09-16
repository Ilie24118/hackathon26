package hackathon26.hackathon.business;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    public void importBundledSnapshot() {
        if (repository.countRecords() > 0) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("KBO/schoten-kbo-1000-2026-09-07.csv").getInputStream(), StandardCharsets.UTF_8))) {
            Map<String,Integer> headers = header(parseCsv(reader.readLine()));
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = parseCsv(line);
                String number = value(row, headers, "Ondernemingsnr");
                if (number.isBlank()) continue;
                String parent = value(row, headers, "Ondernemingsnr_maatsch_zetel");
                repository.saveRecord(new RegistryRecord(number, parent, parent.isBlank() ? "LEGAL_ENTITY" : "ESTABLISHMENT",
                        value(row,headers,"Maatschappelijke_naam"), value(row,headers,"Commerciele_naam"), value(row,headers,"Rechtstoestand"),
                        value(row,headers,"KBO_Straat"), value(row,headers,"KBO_Huisnr"), value(row,headers,"KBO_Postcode"), value(row,headers,"KBO_Gemeente"),
                        value(row,headers,"AR_straat"), value(row,headers,"AR_huisnr"), value(row,headers,"AR_postcode"), value(row,headers,"Omschrijving_hoofdact_BTW"),
                        value(row,headers,"Startdatum"), value(row,headers,"latitude"), value(row,headers,"longitude"), "", "", ""));
            }
            seedDemoEvidence();
        } catch (Exception e) { throw new IllegalStateException("Unable to import bundled KBO snapshot", e); }
    }

    private void seedDemoEvidence() {
        String id = "2286527055";
        if (repository.find(id).isPresent() && repository.evidence(id).isEmpty()) repository.addEvidence(id, "PUBLIC_LISTING", "https://www.google.com/maps/search/?api=1&query=Trixxo+Schoten", "Curated demo observation: public listing should be rechecked by the reviewing officer.", LocalDate.of(2026, 9, 5), "Demo dataset");
    }
    public List<Map<String,Object>> list(String query, String signal, String decision) {
        return repository.findAll().stream().map(this::summary).filter(r -> matches(r,query,signal,decision)).toList();
    }
    private boolean matches(Map<String,Object> r,String query,String signal,String decision) {
        String hay=(r.get("name")+" "+r.get("address")+" "+r.get("registryNumber")).toLowerCase();
        return (query==null || query.isBlank() || hay.contains(query.toLowerCase())) && (signal==null || signal.isBlank() || signal.equals(r.get("signal"))) && (decision==null || decision.isBlank() || decision.equals(r.get("decisionStatus")));
    }
    public Map<String,Object> detail(String id) {
        RegistryRecord r=repository.find(id).orElseThrow(() -> new IllegalArgumentException("Business record not found"));
        Map<String,Object> result=summary(r); result.put("record",r); result.put("evidence",repository.evidence(id)); result.put("decisions",repository.decisions(id)); result.put("parent",repository.findParent(r.getParentRegistryNumber()).orElse(null)); return result;
    }
    public Map<String,Object> summary(RegistryRecord r) {
        List<Evidence> evidence=repository.evidence(r.getRegistryNumber()); List<ReviewDecision> decisions=repository.decisions(r.getRegistryNumber());
        String signal; String confidence; String reason;
        if (present(r.getCessationDate())) { signal="POSSIBLY_INACTIVE"; confidence="HIGH"; reason="The source record includes a cessation date."; }
        else if (!same(r.getKboStreet(),r.getAddressStreet()) || !same(r.getKboHouseNumber(),r.getAddressHouseNumber())) { signal="ADDRESS_OR_MATCH_ISSUE"; confidence="MEDIUM"; reason="KBO and Address Register address fields differ."; }
        else if (evidence.isEmpty()) { signal="POSSIBLY_INACTIVE"; confidence="LOW"; reason="No dated public or officer evidence has been recorded yet."; }
        else { signal="EVIDENCE_REVIEW"; confidence="MEDIUM"; reason="Evidence exists and still needs an officer decision."; }
        Map<String,Object> m=new LinkedHashMap<>(); m.put("registryNumber",r.getRegistryNumber()); m.put("name",name(r)); m.put("recordType",r.getRecordType()); m.put("address",address(r)); m.put("parentRegistryNumber",r.getParentRegistryNumber()); m.put("signal",signal); m.put("confidence",confidence); m.put("reason",reason); m.put("evidenceCount",evidence.size()); m.put("lastObservedOn",evidence.isEmpty()?null:evidence.getFirst().getObservedOn()); m.put("decisionStatus",decisions.isEmpty()?"PENDING":decisions.getFirst().getStatus()); return m;
    }
    public String name(RegistryRecord r) { return present(r.getTradeName()) ? r.getTradeName() : r.getOfficialName(); }
    public String address(RegistryRecord r) { return String.join(" ", blank(r.getKboStreet()), blank(r.getKboHouseNumber()))+", "+blank(r.getKboPostcode())+" "+blank(r.getMunicipality()); }
    private boolean present(String s) { return s!=null&&!s.isBlank(); } private String blank(String s){return s==null?"":s;} private boolean same(String a,String b){return blank(a).trim().equalsIgnoreCase(blank(b).trim());}
    private Map<String,Integer> header(List<String> row){Map<String,Integer> m=new LinkedHashMap<>();for(int i=0;i<row.size();i++)m.put(row.get(i).replace("\uFEFF",""),i);return m;} private String value(List<String> row,Map<String,Integer> h,String key){Integer i=h.get(key);return i==null||i>=row.size()?"":row.get(i).trim();}
    private List<String> parseCsv(String s) { List<String> out=new ArrayList<>();StringBuilder b=new StringBuilder();boolean quoted=false;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='\"'){if(quoted&&i+1<s.length()&&s.charAt(i+1)=='\"'){b.append(c);i++;}else quoted=!quoted;}else if(c==','&&!quoted){out.add(b.toString());b.setLength(0);}else b.append(c);}out.add(b.toString());return out; }
}
