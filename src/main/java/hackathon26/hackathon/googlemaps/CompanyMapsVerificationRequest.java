package hackathon26.hackathon.googlemaps;

/** Input supplied by the registry record that is being checked. */
public record CompanyMapsVerificationRequest(String companyName, double latitude, double longitude) {
    public CompanyMapsVerificationRequest {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("companyName is required");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("latitude or longitude is outside its valid range");
        }
    }
}
