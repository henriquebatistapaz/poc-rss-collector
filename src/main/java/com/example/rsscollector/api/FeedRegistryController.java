package com.example.rsscollector.api;

import com.example.rsscollector.application.FeedRegistryService;
import com.example.rsscollector.application.FeedRegistryService.FeedSummary;
import com.example.rsscollector.application.FeedRegistryService.RefreshResult;
import com.example.rsscollector.domain.FeedItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1")
@Tag(name = "Feed registry", description = "Register public RSS and Atom feeds and refresh links manually")
public class FeedRegistryController {
    private final FeedRegistryService service;

    public FeedRegistryController(FeedRegistryService service) {
        this.service = service;
    }

    @PostMapping("/feeds")
    @Operation(summary = "Register and fetch a public RSS or Atom feed")
    public ResponseEntity<FeedSummary> register(@RequestBody FeedRegistrationRequest request) {
        if (request == null || request.feedUrl() == null || request.feedUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "feedUrl is required");
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request.feedUrl()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        } catch (IOException | InterruptedException exception) {
            restoreInterrupt(exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch the feed URL", exception);
        }
    }

    @GetMapping("/feeds")
    @Operation(summary = "List registered feeds and their latest refresh status")
    public List<FeedSummary> listFeeds() {
        return service.listFeeds();
    }

    @PostMapping("/feeds/{id}/refresh")
    @Operation(summary = "Fetch a registered feed now and add new links")
    public RefreshResult refresh(@PathVariable UUID id) {
        try {
            return service.refresh(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IOException | InterruptedException exception) {
            restoreInterrupt(exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to refresh the feed", exception);
        }
    }

    @GetMapping("/links")
    @Operation(summary = "List collected links with optional feed and publication-time filters")
    public List<FeedItem> listLinks(@RequestParam(required = false) UUID feedId,
                                    @RequestParam(required = false) Instant from,
                                    @RequestParam(required = false) Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be earlier than or equal to to");
        }
        return service.listLinks(feedId, from, to);
    }

    private void restoreInterrupt(Exception exception) {
        if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
    }

    public record FeedRegistrationRequest(String feedUrl) {}
}
