package hackathon26.hackathon.googlemaps;

/** The outcome and the selected Google Maps listing, when one was found. */
public record CompanyMapsVerificationResult(
        MapsListingStatus status,
        String requestedCompanyName,
        double expectedLatitude,
        double expectedLongitude,
        String matchedPlaceName,
        String formattedAddress,
        Double matchedLatitude,
        Double matchedLongitude,
        Double distanceMeters,
        String googleMapsUri,
        String websiteUri,
        String websiteDomain,
        String message) {

    public boolean isPresent() {
        return status == MapsListingStatus.PRESENT;
    }
}
