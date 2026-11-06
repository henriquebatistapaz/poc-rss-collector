package com.example.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RssFeedLinkValidatorTest {
    private final RssFeedLinkValidator validator = new RssFeedLinkValidator();

    @Test
    void acceptsAbsolutePublicHttpAndHttpsUrls() {
        assertTrue(validator.isValidFeedUrl("https://feeds.example.org/news.xml"));
        assertTrue(validator.isValidFeedUrl("http://example.org/rss"));
    }

    @Test
    void rejectsMissingMalformedOrUnsupportedUrls() {
        assertFalse(validator.isValidFeedUrl(null));
        assertFalse(validator.isValidFeedUrl("  "));
        assertFalse(validator.isValidFeedUrl("not a url"));
        assertFalse(validator.isValidFeedUrl("ftp://example.org/feed.xml"));
        assertFalse(validator.isValidFeedUrl("https:///feed.xml"));
    }

    @Test
    void rejectsCredentialsFragmentsAndNonPublicHosts() {
        assertFalse(validator.isValidFeedUrl("https://user:secret@example.org/feed.xml"));
        assertFalse(validator.isValidFeedUrl("https://example.org/feed.xml#latest"));
        assertFalse(validator.isValidFeedUrl("http://localhost/feed.xml"));
        assertFalse(validator.isValidFeedUrl("http://127.0.0.1/feed.xml"));
        assertFalse(validator.isValidFeedUrl("http://192.168.1.20/feed.xml"));
        assertFalse(validator.isValidFeedUrl("http://[::1]/feed.xml"));
        assertFalse(validator.isValidFeedUrl("http://192.0.2.10/feed.xml"));
        assertFalse(validator.isValidFeedUrl("http://[2001:db8::1]/feed.xml"));
    }
}
