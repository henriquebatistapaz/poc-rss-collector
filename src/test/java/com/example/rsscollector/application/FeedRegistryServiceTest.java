package com.example.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FeedRegistryServiceTest {
    private static final String FEED_URL = "https://feeds.example.org/news.xml";

    @Test
    void registersListsAndManuallyRefreshesWithoutDuplicatingLinks() throws Exception {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicReference<String> xml = new AtomicReference<>(feed("""
                <entry><id>story-1</id><title>First</title><link href="https://example.org/story?utm_source=feed"/></entry>
                """));
        FeedRegistryService service = newService(url -> {
            fetchCount.incrementAndGet();
            return xml.get();
        });

        FeedRegistryService.FeedSummary registered = service.register(FEED_URL);
        FeedRegistryService.FeedSummary duplicate = service.register("HTTPS://FEEDS.EXAMPLE.ORG:443/news.xml");
        assertEquals(registered.id(), duplicate.id());
        assertEquals(1, fetchCount.get());
        assertEquals(1, service.listFeeds().size());
        assertEquals(1, service.listLinks(null, null, null).size());

        xml.set(feed("""
                <entry><id>story-1</id><title>Updated title</title><link href="https://example.org/story?utm_source=feed"/></entry>
                <entry><id>story-2</id><title>Second</title><link href="https://example.org/second"/>
                  <updated>2026-11-11T12:00:00Z</updated></entry>
                """));
        FeedRegistryService.RefreshResult refreshed = service.refresh(registered.id());

        assertEquals(2, refreshed.fetchedCount());
        assertEquals(1, refreshed.newCount());
        assertEquals(2, refreshed.totalCount());
        assertEquals(2, service.listLinks(null, null, null).size());
        assertEquals(1, service.listLinks(null, Instant.parse("2026-11-11T00:00:00Z"), null).size());
        assertEquals("Updated title", service.listLinks(null, null, null).getFirst().title());
    }

    @Test
    void rejectsUnsafeOrUnknownFeeds() throws Exception {
        FeedRegistryService service = newService(ignored -> feed(""));

        assertThrows(IllegalArgumentException.class, () -> service.register("http://127.0.0.1/feed"));
        assertThrows(IllegalArgumentException.class, () -> service.refresh(java.util.UUID.randomUUID()));
    }

    private FeedRegistryService newService(FeedDocumentFetcher fetcher) {
        RssFeedLinkValidator linkValidator = new RssFeedLinkValidator();
        RssFeedParser parser = new RssFeedParser(linkValidator);
        return new FeedRegistryService(linkValidator, fetcher, new RssFeedValidator(linkValidator, parser), parser);
    }

    private String feed(String entries) {
        return "<feed xmlns='http://www.w3.org/2005/Atom'><id>urn:test</id><title>Test</title>"
                + "<updated>2026-11-11T10:00:00Z</updated>" + entries + "</feed>";
    }
}
