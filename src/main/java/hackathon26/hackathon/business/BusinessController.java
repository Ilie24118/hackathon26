package hackathon26.hackathon.business;

import hackathon26.hackathon.googlemaps.CompanyMapsVerificationRequest;
import hackathon26.hackathon.googlemaps.CompanyMapsVerificationResult;
import hackathon26.hackathon.googlemaps.GoogleMapsCompanyPresenceService;
import hackathon26.hackathon.googlemaps.MapsListingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {
    private final BusinessService service;
    private final BusinessRepository repository;
    private final GoogleMapsCompanyPresenceService googleMaps;
    public BusinessController(BusinessService service, BusinessRepository repository, GoogleMapsCompanyPresenceService googleMaps) { this.service=service; this.repository=repository; this.googleMaps=googleMaps; }

    @GetMapping
    public List<Map<String,Object>> list(@RequestParam(required=false) String query, @RequestParam(required=false) String signal, @RequestParam(required=false) String decision) { return service.list(query,signal,decision); }
    @GetMapping("/meta")
    public Map<String,Object> meta() { return service.meta(); }
    @GetMapping("/{registryNumber}")
    public Map<String,Object> detail(@PathVariable String registryNumber) { try { return service.detail(registryNumber); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,e.getMessage()); } }
    @PostMapping("/{registryNumber}/evidence")
    public ResponseEntity<Evidence> evidence(@PathVariable String registryNumber,@RequestBody EvidenceInput input) {
        requireRecord(registryNumber); if (blank(input.sourceType())||blank(input.observation())||input.observedOn()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"sourceType, observation and observedOn are required");
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.addEvidence(registryNumber,input.sourceType(),input.sourceUrl(),input.observation(),input.observedOn(),blank(input.officer())?"Officer":input.officer()));
    }
    @PostMapping("/{registryNumber}/google-maps/verify")
    public CompanyMapsVerificationResult verifyGoogleMaps(@PathVariable String registryNumber, @RequestBody(required=false) GoogleMapsInput input) {
        RegistryRecord record = repository.find(registryNumber).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Business record not found"));
        final double latitude;
        final double longitude;
        try { latitude=Double.parseDouble(record.getLatitude()); longitude=Double.parseDouble(record.getLongitude()); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This record has no valid coordinates for a Google Maps check"); }
        CompanyMapsVerificationResult result = googleMaps.verify(new CompanyMapsVerificationRequest(service.name(record),latitude,longitude));
        if (result.status()==MapsListingStatus.PRESENT) {
            String officer=input==null||blank(input.officer())?"Officer":input.officer();
            repository.addEvidence(registryNumber,"GOOGLE_MAPS",result.googleMapsUri(),googleMapsObservation(result),LocalDate.now(),officer);
        }
        return result;
    }
    @PostMapping("/{registryNumber}/decisions")
    public ResponseEntity<ReviewDecision> decision(@PathVariable String registryNumber,@RequestBody DecisionInput input) {
        requireRecord(registryNumber); if (!("CONFIRMED".equals(input.status())||"REJECTED".equals(input.status())||"NEEDS_FOLLOW_UP".equals(input.status()))||blank(input.note())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A supported status and decision note are required");
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.addDecision(registryNumber,input.status(),input.note(),blank(input.officer())?"Officer":input.officer()));
    }
    @GetMapping("/verified/list") public List<VerifiedBusiness> verified() { return repository.verified(); }
    @GetMapping(value="/verified/export",produces="text/csv") public ResponseEntity<String> export() {
        StringBuilder csv=new StringBuilder("registry_number,verified_status,officer,verified_at,note\n");
        for(VerifiedBusiness v:repository.verified()) csv.append(escape(v.getRegistryNumber())).append(',').append(escape(v.getVerifiedStatus())).append(',').append(escape(v.getOfficer())).append(',').append(escape(String.valueOf(v.getVerifiedAt()))).append(',').append(escape(v.getNote())).append('\n');
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=schoten-verified-businesses.csv").contentType(MediaType.valueOf("text/csv")).body(csv.toString());
    }
    private void requireRecord(String id){if(repository.find(id).isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Business record not found");}
    private String googleMapsObservation(CompanyMapsVerificationResult result) {
        return "googleMapsChecked=true; matchedPlaceName="+safe(result.matchedPlaceName())+"; address="+safe(result.formattedAddress())+"; latitude="+safe(result.matchedLatitude())+"; longitude="+safe(result.matchedLongitude())+"; distanceMeters="+safe(result.distanceMeters())+"; websiteUri="+safe(result.websiteUri())+"; websiteDomain="+safe(result.websiteDomain())+"; businessStatus="+safe(result.businessStatus())+"; openNow="+safe(result.openNow())+"; openingHours="+safe(result.openingHours()).replace("\n"," | ");
    }
    private String safe(Object value){return value==null?"":String.valueOf(value).replace(";",",");}
    private boolean blank(String s){return s==null||s.isBlank();} private String escape(String s){return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";}
    public record EvidenceInput(String sourceType,String sourceUrl,String observation,LocalDate observedOn,String officer) {}
    public record GoogleMapsInput(String officer) {}
    public record DecisionInput(String status,String note,String officer) {}
}
