package hackathon26.hackathon.googlemaps;

import java.text.Normalizer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Verifies whether Google Maps has a listing whose name and pin both agree with a
 * company record. This must be used as evidence for review, never as sole proof of
 * a company's legal or operational status.
 */
@Service
public class GoogleMapsCompanyPresenceService {
    private final GoogleMapsProperties properties;
    private final GooglePlacesTextSearchClient placesClient;

    public GoogleMapsCompanyPresenceService(GoogleMapsProperties properties, GooglePlacesTextSearchClient placesClient) {
        this.properties = properties;
        this.placesClient = placesClient;
    }

    public CompanyMapsVerificationResult verify(CompanyMapsVerificationRequest request) {
        if (!properties.isConfigured()) {
            return result(MapsListingStatus.NOT_CONFIGURED, request, null, null,
                    "Google Maps is not configured; set GOOGLE_MAPS_API_KEY first.");
        }
        try {
            List<GooglePlaceCandidate> nameMatches = placesClient.search(request.companyName(), request.latitude(), request.longitude()).stream()
                    .filter(place -> namesMatch(request.companyName(), place.displayName()))
                    .toList();
            GooglePlaceCandidate nearest = nameMatches.stream()
                    .min(Comparator.comparingDouble(place -> distanceMeters(request.latitude(), request.longitude(), place.latitude(), place.longitude())))
                    .orElse(null);
            if (nearest == null) {
                return result(MapsListingStatus.NOT_FOUND, request, null, null,
                        "No Google Maps result matched the company name.");
            }
            double distance = distanceMeters(request.latitude(), request.longitude(), nearest.latitude(), nearest.longitude());
            if (distance > properties.coordinateToleranceMeters()) {
                return result(MapsListingStatus.NOT_FOUND, request, nearest, distance,
                        "A name match was found, but its pin is outside the " + properties.coordinateToleranceMeters() + " m tolerance.");
            }
            return result(MapsListingStatus.PRESENT, request, nearest, distance,
                    "A Google Maps listing matched the name and coordinate tolerance.");
        } catch (GoogleMapsRequestException e) {
            return result(MapsListingStatus.REQUEST_FAILED, request, null, null, e.getMessage());
        }
    }

    private boolean namesMatch(String expected, String actual) {
        String normalizedExpected = normalize(expected);
        String normalizedActual = normalize(actual);
        return !normalizedExpected.isBlank() && !normalizedActual.isBlank()
                && (normalizedExpected.equals(normalizedActual)
                || normalizedExpected.contains(normalizedActual)
                || normalizedActual.contains(normalizedExpected));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }

    private double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private CompanyMapsVerificationResult result(MapsListingStatus status, CompanyMapsVerificationRequest request,
                                                   GooglePlaceCandidate place, Double distanceMeters, String message) {
        return new CompanyMapsVerificationResult(status, request.companyName(), request.latitude(), request.longitude(),
                place == null ? null : place.displayName(), place == null ? null : place.formattedAddress(),
                place == null ? null : place.latitude(), place == null ? null : place.longitude(), distanceMeters,
                place == null ? null : place.googleMapsUri(), place == null ? null : place.websiteUri(),
                place == null ? null : websiteDomain(place.websiteUri()), place == null ? null : place.businessStatus(),
                place == null ? null : place.openNow(), place == null ? null : place.openingHours(), message);
    }

    private String websiteDomain(String websiteUri) {
        if (websiteUri == null || websiteUri.isBlank()) return null;
        try {
            String host = new URI(websiteUri).getHost();
            return host == null ? null : host.toLowerCase().replaceFirst("^www\\.", "");
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
