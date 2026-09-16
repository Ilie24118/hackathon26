package hackathon26.hackathon.googlemaps;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration test endpoint for the Google Maps presence service.
 *
 * <p>POST /api/google-maps/verify
 * with {"companyName":"Example Company","latitude":51.2642,"longitude":4.5257}</p>
 */
@RestController
@RequestMapping("/api/google-maps")
public class GoogleMapsVerificationController {
    private final GoogleMapsCompanyPresenceService presenceService;

    public GoogleMapsVerificationController(GoogleMapsCompanyPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/verify")
    public ResponseEntity<CompanyMapsVerificationResult> verify(@RequestBody CompanyMapsVerificationRequest request) {
        CompanyMapsVerificationResult result = presenceService.verify(request);
        HttpStatus status = switch (result.status()) {
            case PRESENT, NOT_FOUND -> HttpStatus.OK;
            case NOT_CONFIGURED -> HttpStatus.SERVICE_UNAVAILABLE;
            case REQUEST_FAILED -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(result);
    }

}
