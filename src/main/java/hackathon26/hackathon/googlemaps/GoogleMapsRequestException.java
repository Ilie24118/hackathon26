package hackathon26.hackathon.googlemaps;

class GoogleMapsRequestException extends RuntimeException {
    GoogleMapsRequestException(String message) { super(message); }
    GoogleMapsRequestException(String message, Throwable cause) { super(message, cause); }
}
