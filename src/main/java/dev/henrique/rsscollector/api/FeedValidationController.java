package dev.henrique.rsscollector.api;

import dev.henrique.rsscollector.application.FeedUrlValidationService;
import dev.henrique.rsscollector.application.FeedUrlValidationService.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/feeds")
@Tag(name = "Feed validation", description = "Validate public RSS 2.0 and Atom 1.0 feed URLs")
public class FeedValidationController {
    private final FeedUrlValidationService service;

    public FeedValidationController(FeedUrlValidationService service) {
        this.service = service;
    }

    @PostMapping("/validate")
    @Operation(summary = "Fetch a public feed URL and check whether its document is valid RSS or Atom")
    public ResponseEntity<ValidationResult> validate(@RequestBody FeedValidationRequest request) {
        if (request == null || request.feedUrl() == null || request.feedUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "feedUrl is required");
        }
        try {
            return ResponseEntity.ok(service.validate(request.feedUrl()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch the feed URL", exception);
        }
    }

    public record FeedValidationRequest(String feedUrl) {}
}
