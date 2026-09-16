package hackathon26.hackathon.googlemaps;

/**
 * PRESENT means a Google Maps place matched both the company name and the configured
 * coordinate tolerance. It is a public-listing signal, not a legal verification.
 */
public enum MapsListingStatus {
    PRESENT,
    NOT_FOUND,
    NOT_CONFIGURED,
    REQUEST_FAILED
}
