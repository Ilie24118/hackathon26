package hackathon26.hackathon.business;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Loads the bundled Schoten KBO snapshot when a fresh local database has no records. */
@Component
public class KboSnapshotImporter implements ApplicationRunner {
    private static final String SNAPSHOT = "KBO/schoten-kbo-1000-2026-09-07.csv";
    private final BusinessRepository repository;

    public KboSnapshotImporter(BusinessRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.countRecords() > 0) return;
        List<RegistryRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(SNAPSHOT).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // Header
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = csvFields(line);
                if (row.size() < 32 || value(row, 2).isBlank()) continue;
                String type = value(row, 7).equalsIgnoreCase("Rechtspersoon") ? "LEGAL_ENTITY" : "ESTABLISHMENT";
                records.add(new RegistryRecord(
                        value(row, 2), value(row, 6), type, value(row, 3), value(row, 4), first(value(row, 4), value(row, 3)),
                        value(row, 9), value(row, 8), value(row, 10), value(row, 11), value(row, 12), value(row, 13), value(row, 14),
                        value(row, 15), value(row, 16), value(row, 17), value(row, 18), value(row, 21), date(value(row, 28)), date(value(row, 29)), "",
                        value(row, 31), value(row, 30), "", "", ""));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the bundled KBO snapshot", e);
        }
        repository.saveRecords(records);
    }

    private List<String> csvFields(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"' && quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                value.append(character); index++;
            } else if (character == '"') quoted = !quoted;
            else if (character == ',' && !quoted) { values.add(value.toString().trim()); value.setLength(0); }
            else value.append(character);
        }
        values.add(value.toString().trim());
        return values;
    }

    private String value(List<String> row, int index) { return index < row.size() ? row.get(index).trim() : ""; }
    private String first(String preferred, String fallback) { return preferred.isBlank() ? fallback : preferred; }
    private String date(String raw) { return raw.length() >= 10 ? raw.substring(0, 10) : raw; }
}
