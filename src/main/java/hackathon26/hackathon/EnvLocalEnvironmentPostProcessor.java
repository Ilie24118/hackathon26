package hackathon26.hackathon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads the API keys needed by local integrations from .env.local or .env when they are not
 * already present in the environment. Database credentials are deliberately not exposed.
 */
public class EnvLocalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    static final String PROPERTY_SOURCE_NAME = "env.local";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) return;
        Map<String, String> local = readEnvLocal(Path.of(".env.local"));
        Map<String, String> env = readEnvLocal(Path.of(".env"));
        Map<String, Object> properties = new HashMap<>();
        addIfMissing(environment, properties, "OPENROUTER_API_KEY", local, env);
        addIfMissing(environment, properties, "GOOGLE_MAPS_API_KEY", local, env);
        if (!properties.isEmpty()) environment.getPropertySources().addLast(
                new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }

    Map<String, String> readEnvLocal(Path file) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(file)) return values;
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.trim();
                if (line.startsWith("export ")) line = line.substring(7).trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                int eq = line.indexOf('=');
                String key = line.substring(0, eq).trim();
                String value = unquote(line.substring(eq + 1).trim());
                if (!key.isBlank() && !value.isBlank()) values.put(key, value);
            }
        } catch (IOException ignored) { }
        return values;
    }

    private void addIfMissing(ConfigurableEnvironment environment, Map<String, Object> properties, String key,
                              Map<String, String> local, Map<String, String> env) {
        if (environment.getProperty(key) != null) return;
        String value = local.getOrDefault(key, env.get(key));
        if (value != null && !value.isBlank()) properties.put(key, value);
    }

    private String unquote(String value) {
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
