package hackathon26.hackathon.googlemaps;

/** Minimal subset of a Place returned by Places API (New) Text Search. */
record GooglePlaceCandidate(
        String id,
        String displayName,
        String formattedAddress,
        double latitude,
        double longitude,
        String googleMapsUri,
        String websiteUri) {
}
