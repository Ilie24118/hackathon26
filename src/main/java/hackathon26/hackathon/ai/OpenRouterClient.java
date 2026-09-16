package hackathon26.hackathon.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenRouterClient {
    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public OpenRouterClient(@Value("${OPENROUTER_API_KEY:}") String apiKey,
                            @Value("${openrouter.model:inclusionai/ling-3.0-flash-sante:free}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder().requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public Completion complete(String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                "reasoning", Map.of("enabled", true));
        try {
            String json = client.post().uri("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(String.class);
            JsonNode message = mapper.readTree(json).path("choices").path(0).path("message");
            String content = message.path("content").asString("");
            if (content.isBlank()) throw new IllegalStateException("OpenRouter returned an empty answer");
            return new Completion(content, message.path("reasoning").asString(""));
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OpenRouter request failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("Unable to reach OpenRouter: " + e.getMessage(), e);
        }
    }

    public String model() { return model; }

    public record Completion(String answer, String reasoning) {}
}
