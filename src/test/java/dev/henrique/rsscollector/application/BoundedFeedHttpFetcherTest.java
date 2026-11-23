package dev.henrique.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class BoundedFeedHttpFetcherTest {
    private static final String FEED_URL = "https://feeds.example.org/rss.xml";

    @Test
    void followsUpToThreeRedirectsAndReturnsBoundedResponse() throws Exception {
        RssFeedLinkValidator validator = safeValidator();
        Queue<FakeConnection> responses = new ArrayDeque<>();
        responses.add(new FakeConnection(302, "../one", ""));
        responses.add(new FakeConnection(301, "/two", ""));
        responses.add(new FakeConnection(307, "https://feeds.example.org/final", ""));
        responses.add(new FakeConnection(200, null, "<rss/>"));

        BoundedFeedHttpFetcher fetcher = new BoundedFeedHttpFetcher(validator, ignored -> responses.remove());

        assertEquals("<rss/>", fetcher.fetch(FEED_URL));
    }

    @Test
    void rejectsTheFourthRedirect() {
        RssFeedLinkValidator validator = safeValidator();
        Queue<FakeConnection> responses = new ArrayDeque<>();
        for (int index = 0; index < 4; index++) responses.add(new FakeConnection(302, "/next", ""));
        BoundedFeedHttpFetcher fetcher = new BoundedFeedHttpFetcher(validator, ignored -> responses.remove());

        assertThrows(IOException.class, () -> fetcher.fetch(FEED_URL));
    }

    @Test
    void rejectsRedirectAfterDestinationResolvesToPrivateAddress() {
        RssFeedLinkValidator validator = mock(RssFeedLinkValidator.class);
        given(validator.isValidFeedUrl(FEED_URL)).willReturn(true);
        given(validator.resolvesOnlyToPublicAddresses(FEED_URL)).willReturn(true);
        given(validator.isValidFeedUrl("https://feeds.example.org/private")).willReturn(true);
        given(validator.resolvesOnlyToPublicAddresses("https://feeds.example.org/private")).willReturn(false);
        Queue<FakeConnection> responses = new ArrayDeque<>();
        responses.add(new FakeConnection(302, "/private", ""));
        BoundedFeedHttpFetcher fetcher = new BoundedFeedHttpFetcher(validator, ignored -> responses.remove());

        assertThrows(IOException.class, () -> fetcher.fetch(FEED_URL));
        assertEquals(0, responses.size());
    }

    @Test
    void rejectsResponsesLargerThanOneMegabyte() {
        RssFeedLinkValidator validator = safeValidator();
        FakeConnection connection = new FakeConnection(200, null, "x".repeat(1_000_001));
        BoundedFeedHttpFetcher fetcher = new BoundedFeedHttpFetcher(validator, ignored -> connection);

        assertThrows(IOException.class, () -> fetcher.fetch(FEED_URL));
    }

    @Test
    void propagatesNetworkTimeouts() {
        RssFeedLinkValidator validator = safeValidator();
        FakeConnection connection = new FakeConnection(200, null, "") {
            @Override
            public int getResponseCode() throws IOException {
                throw new SocketTimeoutException("timed out");
            }
        };
        BoundedFeedHttpFetcher fetcher = new BoundedFeedHttpFetcher(validator, ignored -> connection);

        assertThrows(SocketTimeoutException.class, () -> fetcher.fetch(FEED_URL));
    }

    private RssFeedLinkValidator safeValidator() {
        RssFeedLinkValidator validator = mock(RssFeedLinkValidator.class);
        given(validator.isValidFeedUrl(org.mockito.ArgumentMatchers.anyString())).willReturn(true);
        given(validator.resolvesOnlyToPublicAddresses(org.mockito.ArgumentMatchers.anyString())).willReturn(true);
        return validator;
    }

    private static class FakeConnection extends HttpURLConnection {
        private final int status;
        private final String location;
        private final byte[] body;

        private FakeConnection(int status, String location, String body) {
            super(feedUrl());
            this.status = status;
            this.location = location;
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override public int getResponseCode() throws IOException { return status; }
        @Override public String getHeaderField(String name) { return "Location".equalsIgnoreCase(name) ? location : null; }
        @Override public ByteArrayInputStream getInputStream() { return new ByteArrayInputStream(body); }
        @Override public void disconnect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }

        private static URL feedUrl() {
            try {
                return new URL(FEED_URL);
            } catch (MalformedURLException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
