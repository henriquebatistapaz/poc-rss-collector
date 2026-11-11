package com.example.rsscollector.application;

import com.example.rsscollector.domain.FeedItem;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** In-memory feed registry and manual refresh service for the initial API slice. */
@Service
public class FeedRegistryService {
    private static final int MAX_FEEDS = 100;
    private static final int MAX_ITEMS_PER_FEED = 1_000;

    private final RssFeedLinkValidator linkValidator;
    private final FeedDocumentFetcher fetcher;
    private final RssFeedValidator validator;
    private final RssFeedParser parser;
    private final Map<UUID, FeedState> feeds = new ConcurrentHashMap<>();
    private final Map<String, UUID> feedIdsByUrl = new ConcurrentHashMap<>();

    public FeedRegistryService(RssFeedLinkValidator linkValidator, FeedDocumentFetcher fetcher,
                               RssFeedValidator validator, RssFeedParser parser) {
        this.linkValidator = linkValidator;
        this.fetcher = fetcher;
        this.validator = validator;
        this.parser = parser;
    }

    public FeedSummary register(String feedUrl) throws IOException, InterruptedException {
        URI normalizedUrl = normalizeFeedUrl(feedUrl);
        String urlKey = normalizedUrl.toString();
        UUID existingId = feedIdsByUrl.get(urlKey);
        if (existingId != null) return summary(feeds.get(existingId));
        synchronized (this) {
            existingId = feedIdsByUrl.get(urlKey);
            if (existingId != null) return summary(feeds.get(existingId));
            if (feeds.size() >= MAX_FEEDS) throw new IllegalStateException("Feed registry is full");
        }

        List<FeedItem> initialItems = fetchItems(urlKey);
        synchronized (this) {
            existingId = feedIdsByUrl.get(urlKey);
            if (existingId != null) return summary(feeds.get(existingId));
            if (feeds.size() >= MAX_FEEDS) throw new IllegalStateException("Feed registry is full");
            Instant now = Instant.now();
            FeedState state = new FeedState(UUID.randomUUID(), normalizedUrl, now);
            initialItems.forEach(state::upsert);
            state.lastRefreshedAt = now;
            feeds.put(state.id, state);
            feedIdsByUrl.put(urlKey, state.id);
            return summary(state);
        }
    }

    public List<FeedSummary> listFeeds() {
        return feeds.values().stream().map(FeedRegistryService::summary)
                .sorted((left, right) -> left.createdAt().compareTo(right.createdAt())).toList();
    }

    public RefreshResult refresh(UUID feedId) throws IOException, InterruptedException {
        FeedState state = feeds.get(feedId);
        if (state == null) throw new IllegalArgumentException("Feed was not found");
        synchronized (state) {
            List<FeedItem> fetchedItems = fetchItems(state.url.toString());
            int added = 0;
            for (FeedItem item : fetchedItems) {
                if (!state.items.containsKey(identity(item))) added++;
                state.upsert(item);
            }
            state.lastRefreshedAt = Instant.now();
            return new RefreshResult(state.id, fetchedItems.size(), added, state.items.size(), state.lastRefreshedAt);
        }
    }

    public List<FeedItem> listLinks(UUID feedId, Instant from, Instant to) {
        List<FeedState> selected = feedId == null
                ? new ArrayList<>(feeds.values())
                : feeds.containsKey(feedId) ? List.of(feeds.get(feedId)) : List.of();
        return selected.stream().flatMap(state -> {
            synchronized (state) {
                return List.copyOf(state.items.values()).stream();
            }
        }).filter(item -> from == null || (item.publishedAt() != null && !item.publishedAt().isBefore(from)))
                .filter(item -> to == null || (item.publishedAt() != null && !item.publishedAt().isAfter(to)))
                .toList();
    }

    private List<FeedItem> fetchItems(String feedUrl) throws IOException, InterruptedException {
        String xml = fetcher.fetch(feedUrl);
        if (!validator.isValidRssFeed(feedUrl, xml)) {
            throw new IOException("Feed response is not valid RSS 2.0 or Atom 1.0");
        }
        return parser.parse(feedUrl, xml);
    }

    private URI normalizeFeedUrl(String feedUrl) {
        if (!linkValidator.isValidFeedUrl(feedUrl)) {
            throw new IllegalArgumentException("feedUrl must be an absolute public HTTP(S) URL");
        }
        try {
            URI source = new URI(feedUrl.trim());
            String scheme = source.getScheme().toLowerCase(Locale.ROOT);
            String host = source.getHost().toLowerCase(Locale.ROOT);
            int port = source.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) port = -1;
            String authority = host.contains(":") ? "[" + host + "]" : host;
            return new URI(scheme + "://" + authority + (port < 0 ? "" : ":" + port)
                    + (source.getRawPath() == null ? "" : source.getRawPath())
                    + (source.getRawQuery() == null ? "" : "?" + source.getRawQuery())).normalize();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("feedUrl is invalid", exception);
        }
    }

    private static FeedSummary summary(FeedState state) {
        if (state == null) throw new IllegalStateException("Feed registry entry is unavailable");
        synchronized (state) {
            return new FeedSummary(state.id, state.url, state.createdAt, state.lastRefreshedAt, state.items.size());
        }
    }

    private static ItemIdentity identity(FeedItem item) {
        String id = item.id() == null || item.id().isBlank() ? item.link().toString() : item.id().trim();
        return new ItemIdentity(item.sourceFeed(), id);
    }

    public record FeedSummary(UUID id, URI feedUrl, Instant createdAt, Instant lastRefreshedAt, int itemCount) {}

    public record RefreshResult(UUID feedId, int fetchedCount, int newCount, int totalCount, Instant refreshedAt) {}

    private record ItemIdentity(String sourceFeed, String itemId) {}

    private static final class FeedState {
        private final UUID id;
        private final URI url;
        private final Instant createdAt;
        private final LinkedHashMap<ItemIdentity, FeedItem> items = new LinkedHashMap<>();
        private Instant lastRefreshedAt;

        private FeedState(UUID id, URI url, Instant createdAt) {
            this.id = id;
            this.url = url;
            this.createdAt = createdAt;
        }

        private void upsert(FeedItem item) {
            items.put(identity(item), item);
            while (items.size() > MAX_ITEMS_PER_FEED) items.remove(items.keySet().iterator().next());
        }
    }
}
