package hackathon26.hackathon.googlemaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Low-level adapter for Google Places API (New) Text Search.
 * Keep this adapter separate so it can be replaced or mocked without changing
 * the verification rules.
 */
@Component
class GooglePlacesTextSearchClient {
    private static final URI TEXT_SEARCH_URI = URI.create("https://places.googleapis.com/v1/places:searchText");
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,places.googleMapsUri,places.websiteUri";
    private static final String STRING_VALUE_PATTERN = "\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"";
    private static final String NUMBER_VALUE_PATTERN = "\"%s\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)";

    private final GoogleMapsProperties properties;
    private final HttpClient httpClient;

    @Autowired
    GooglePlacesTextSearchClient(GoogleMapsProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    GooglePlacesTextSearchClient(GoogleMapsProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    List<GooglePlaceCandidate> search(String companyName, double latitude, double longitude) {
        try {
            HttpRequest request = HttpRequest.newBuilder(TEXT_SEARCH_URI)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody(companyName, latitude, longitude)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GoogleMapsRequestException("Google Places Text Search returned HTTP " + response.statusCode());
            }
            return candidates(response.body());
        } catch (IOException e) {
            throw new GoogleMapsRequestException("Could not read the Google Places response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GoogleMapsRequestException("Google Places request was interrupted", e);
        }
    }

    private String requestBody(String companyName, double latitude, double longitude) {
        return String.format(Locale.ROOT,
                "{\"textQuery\":\"%s\",\"locationBias\":{\"circle\":{\"center\":{\"latitude\":%.8f,\"longitude\":%.8f},\"radius\":%.1f}},\"maxResultCount\":20}",
                escapeJson(companyName), latitude, longitude, properties.searchBiasRadiusMeters());
    }

    private List<GooglePlaceCandidate> candidates(String body) {
        List<GooglePlaceCandidate> result = new ArrayList<>();
        for (String place : objectsInPlacesArray(body)) {
            String location = objectValue(place, "location");
            String displayName = objectValue(place, "displayName");
            Double latitude = numberValue(location, "latitude");
            Double longitude = numberValue(location, "longitude");
            if (latitude == null || longitude == null) continue;
            result.add(new GooglePlaceCandidate(
                    stringValue(place, "id"), stringValue(displayName, "text"), stringValue(place, "formattedAddress"),
                    latitude, longitude, stringValue(place, "googleMapsUri"), stringValue(place, "websiteUri")));
        }
        return result;
    }

    private List<String> objectsInPlacesArray(String json) {
        int arrayStart = json.indexOf("\"places\"");
        if (arrayStart < 0) return List.of();
        arrayStart = json.indexOf('[', arrayStart);
        if (arrayStart < 0) return List.of();
        List<String> objects = new ArrayList<>();
        int depth = 0, objectStart = -1;
        boolean quoted = false, escaped = false;
        for (int index = arrayStart + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (quoted) {
                if (!escaped && character == '\\') escaped = true;
                else { if (!escaped && character == '\"') quoted = false; escaped = false; }
                continue;
            }
            if (character == '\"') quoted = true;
            else if (character == '{') { if (depth++ == 0) objectStart = index; }
            else if (character == '}' && --depth == 0 && objectStart >= 0) objects.add(json.substring(objectStart, index + 1));
            else if (character == ']' && depth == 0) break;
        }
        return objects;
    }

    private String objectValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) return "";
        int start = json.indexOf('{', keyIndex);
        if (start < 0) return "";
        int depth = 0;
        for (int index = start; index < json.length(); index++) {
            if (json.charAt(index) == '{') depth++;
            else if (json.charAt(index) == '}' && --depth == 0) return json.substring(start, index + 1);
        }
        return "";
    }

    private String stringValue(String json, String key) {
        Matcher matcher = Pattern.compile(STRING_VALUE_PATTERN.formatted(Pattern.quote(key))).matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : "";
    }

    private Double numberValue(String json, String key) {
        Matcher matcher = Pattern.compile(NUMBER_VALUE_PATTERN.formatted(Pattern.quote(key))).matcher(json);
        return matcher.find() ? Double.valueOf(matcher.group(1)) : null;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
