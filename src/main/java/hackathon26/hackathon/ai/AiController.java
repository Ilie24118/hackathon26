package hackathon26.hackathon.ai;

import hackathon26.hackathon.business.BusinessRepository;
import hackathon26.hackathon.business.Evidence;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final String DEFAULT_QUESTION =
            "Does this website show a currently active business? Summarize what the site says about its activity, address, contact details and opening hours.";

    private final WebsiteScraper scraper;
    private final OpenRouterClient openRouter;
    private final BusinessRepository repository;
    public AiController(WebsiteScraper scraper, OpenRouterClient openRouter, BusinessRepository repository) { this.scraper=scraper; this.openRouter=openRouter; this.repository=repository; }

    @PostMapping("/analyze")
    public Map<String,Object> analyze(@RequestBody AnalyzeRequest input) {
        String url = input.url() == null ? "" : input.url().trim();
        if (url.isBlank() || !(url.startsWith("http://") || url.startsWith("https://")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid http(s) url is required");
        if (!openRouter.isConfigured())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"OpenRouter API key is not configured (set OPENROUTER_API_KEY)");
        String question = input.question() == null || input.question().isBlank() ? DEFAULT_QUESTION : input.question().trim();
        if (input.registryNumber() != null && !input.registryNumber().isBlank()) {
            String id = input.registryNumber().trim();
            if (repository.find(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Business record not found");
        }
        String pageText;
        try { pageText = scraper.scrape(url); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,e.getMessage()); }
        OpenRouterClient.Completion completion;
        try { completion = openRouter.complete(prompt(url, pageText, question)); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,e.getMessage()); }

        Evidence evidence = null;
        if (input.registryNumber() != null && !input.registryNumber().isBlank()) {
            evidence = repository.addEvidence(input.registryNumber().trim(), "AI_CHECK", url, completion.answer(), LocalDate.now(), "AI Assistant");
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("url", url); result.put("question", question);
        result.put("answer", completion.answer()); result.put("reasoning", completion.reasoning());
        result.put("model", openRouter.model()); result.put("analyzedAt", OffsetDateTime.now());
        result.put("evidence", evidence);
        return result;
    }

    private String prompt(String url, String pageText, String question) {
        return "You are helping a municipal officer verify whether a business is active. "
                + "A website was scraped and its text is below. Answer the question using only this website content; "
                + "if the content does not contain the answer, say so clearly.\n\n"
                + "Website URL: " + url + "\n\n"
                + "Website content:\n" + pageText + "\n\n"
                + "Question: " + question;
    }

    public record AnalyzeRequest(String url, String question, String registryNumber) {}
}
