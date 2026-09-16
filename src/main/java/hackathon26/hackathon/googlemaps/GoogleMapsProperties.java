package hackathon26.hackathon.googlemaps;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration deliberately lives outside application.yaml so this integration can
 * be added without changing current application configuration. Set GOOGLE_MAPS_API_KEY
 * in the deployment environment before calling the service.
 */
@Component
public class GoogleMapsProperties {
    private final String apiKey;
    private final double coordinateToleranceMeters;
    private final double searchBiasRadiusMeters;

    public GoogleMapsProperties(
            @Value("${google.maps.api-key:${GOOGLE_MAPS_API_KEY:}}") String apiKey,
            @Value("${google.maps.coordinate-tolerance-meters:150}") double coordinateToleranceMeters,
            @Value("${google.maps.search-bias-radius-meters:5000}") double searchBiasRadiusMeters) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.coordinateToleranceMeters = coordinateToleranceMeters;
        this.searchBiasRadiusMeters = searchBiasRadiusMeters;
    }

    boolean isConfigured() { return !apiKey.isBlank(); }
    String apiKey() { return apiKey; }
    double coordinateToleranceMeters() { return coordinateToleranceMeters; }
    double searchBiasRadiusMeters() { return searchBiasRadiusMeters; }
}
