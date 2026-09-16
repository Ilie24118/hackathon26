package hackathon26.hackathon.ai;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class WebsiteScraper {
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_TEXT_CHARS = 12_000;

    private final RestClient client;

    public WebsiteScraper() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    public String scrape(String url) {
        try {
            String html = client.get().uri(url)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .retrieve().body(String.class);
            if (html == null || html.isBlank()) throw new IllegalArgumentException("Empty response body");
            if (html.length() > MAX_BODY_BYTES) html = html.substring(0, MAX_BODY_BYTES);
            return toText(html);
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new IllegalStateException("Unable to fetch website: " + e.getMessage(), e);
        }
    }

    String toText(String html) {
        String text = html.replaceAll("(?is)<(script|style|noscript|svg|head)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<!--.*?-->", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr|section|article)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s*", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
    }
}
