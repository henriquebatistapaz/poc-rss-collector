package com.example.rsslinkcollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class FeedUrlValidationServiceTest {
    private static final String ATOM = """
            <feed xmlns="http://www.w3.org/2005/Atom"><id>urn:feed:test</id><title>sample</title>
              <updated>2026-11-05T10:00:00Z</updated>
              <entry><id>item-1</id><title>One</title><link href="https://example.org/one"/></entry>
            </feed>
            """;

    @Test
    void fetchesUrlAndReportsValidAtomAndItemCount() throws Exception {
        RssFeedLinkValidator links = new RssFeedLinkValidator();
        RssFeedParser parser = new RssFeedParser(links);
        FeedUrlValidationService service = new FeedUrlValidationService(links, ignored -> ATOM,
                new RssFeedValidator(links, parser), parser);

        var result = service.validate("https://feeds.example.org/atom.xml");

        assertEquals(true, result.valid());
        assertEquals(1, result.itemCount());
    }

    @Test
    void rejectsUnsafeUrlBeforeCallingFetcher() {
        AtomicBoolean fetched = new AtomicBoolean();
        RssFeedLinkValidator links = new RssFeedLinkValidator();
        RssFeedParser parser = new RssFeedParser(links);
        FeedUrlValidationService service = new FeedUrlValidationService(links, ignored -> {
            fetched.set(true);
            return ATOM;
        }, new RssFeedValidator(links, parser), parser);

        assertThrows(IllegalArgumentException.class, () -> service.validate("http://127.0.0.1/feed"));
        assertEquals(false, fetched.get());
    }
}
