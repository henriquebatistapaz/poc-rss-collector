package dev.henrique.rsscollector.application;

import dev.henrique.rsscollector.api.FeedRegistryController;
import dev.henrique.rsscollector.domain.FeedItem;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApplicationExtraCoverageTest {

    @Test
    void boundedFetcherAndParserAndServiceCoverRemainingBranches() throws Exception {
        RssFeedLinkValidator linkVal = mock(RssFeedLinkValidator.class);
        when(linkVal.resolvesOnlyToPublicAddresses(anyString())).thenReturn(true);

        // Status 404 (<200 or >=300)
        HttpURLConnection conn404 = mock(HttpURLConnection.class);
        when(conn404.getResponseCode()).thenReturn(404);
        BoundedFeedHttpFetcher f404 = new BoundedFeedHttpFetcher(linkVal, u -> conn404);
        assertThrows(IOException.class, () -> f404.fetch("https://example.com/rss"));

        // Status 100 (<200)
        HttpURLConnection conn100 = mock(HttpURLConnection.class);
        when(conn100.getResponseCode()).thenReturn(100);
        BoundedFeedHttpFetcher f100 = new BoundedFeedHttpFetcher(linkVal, u -> conn100);
        assertThrows(IOException.class, () -> f100.fetch("https://example.com/rss"));

        // Redirect with null Location
        HttpURLConnection conn302Null = mock(HttpURLConnection.class);
        when(conn302Null.getResponseCode()).thenReturn(302);
        when(conn302Null.getHeaderField("Location")).thenReturn(null);
        BoundedFeedHttpFetcher f302Null = new BoundedFeedHttpFetcher(linkVal, u -> conn302Null);
        assertThrows(IOException.class, () -> f302Null.fetch("https://example.com/rss"));

        // Redirect to unsafe URL
        HttpURLConnection conn302Unsafe = mock(HttpURLConnection.class);
        when(conn302Unsafe.getResponseCode()).thenReturn(302);
        when(conn302Unsafe.getHeaderField("Location")).thenReturn("http://localhost/evil");
        when(linkVal.isValidFeedUrl("http://localhost/evil")).thenReturn(false);
        BoundedFeedHttpFetcher f302Unsafe = new BoundedFeedHttpFetcher(linkVal, u -> conn302Unsafe);
        assertThrows(IOException.class, () -> f302Unsafe.fetch("https://example.com/rss"));

        // Parser package-private & Atom branches
        RssFeedLinkValidator realVal = new RssFeedLinkValidator();
        RssFeedParser parser = new RssFeedParser(realVal);
        assertTrue(parser.isValidRssDocument("<rss version='2.0'><channel><title>T</title></channel></rss>"));
        assertFalse(parser.isValidFeedDocument("<rss version='2.0'></rss>"));
        String atomMore = """
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <id>1</id><title>T</title><updated>2024-01-01T00:00:00Z</updated>
                  <entry><title>NoLinksAtAll</title></entry>
                  <entry><title>MissingHrefAttr</title><link rel="alternate"/></entry>
                </feed>
                """;
        assertTrue(parser.parse("https://example.com/a2", atomMore).isEmpty());
        String rssMissingLinkTag = "<rss version='2.0'><channel><item><title>NoLinkTag</title></item></channel></rss>";
        assertTrue(parser.parse("https://example.com/r2", rssMissingLinkTag).isEmpty());
        assertFalse(realVal.resolvesOnlyToPublicAddresses("https://localhost.domain.invalid/rss"));
        java.lang.reflect.Method isPub = RssFeedLinkValidator.class.getDeclaredMethod("isPublicAddress", java.net.InetAddress.class);
        isPub.setAccessible(true);
        java.net.InetAddress badLenAddr = mock(java.net.InetAddress.class);
        when(badLenAddr.getAddress()).thenReturn(new byte[]{1, 2});
        assertFalse((Boolean) isPub.invoke(realVal, badLenAddr));
        java.lang.reflect.Method localName = RssFeedParser.class.getDeclaredMethod("localName", org.w3c.dom.Element.class);
        localName.setAccessible(true);
        org.w3c.dom.Element mockEl = mock(org.w3c.dom.Element.class);
        when(mockEl.getLocalName()).thenReturn(null);
        when(mockEl.getTagName()).thenReturn("rss");
        assertEquals("rss", localName.invoke(parser, mockEl));
        java.lang.reflect.Method resAtom = RssFeedParser.class.getDeclaredMethod("resolveAtomHref", String.class, String.class);
        resAtom.setAccessible(true);
        assertNull(resAtom.invoke(parser, "https://example.com", null));
        java.lang.reflect.Method normHttp = RssFeedParser.class.getDeclaredMethod("normalizeHttpUrl", String.class);
        normHttp.setAccessible(true);
        assertNull(normHttp.invoke(parser, "   "));

        assertFalse(parser.isValidFeedDocument("<atom xmlns='http://www.w3.org/2005/Atom'/>"));
        assertFalse(parser.isValidFeedDocument("<feed xmlns='http://www.w3.org/2005/Atom'><title>NoId</title><updated>2024-01-01T00:00:00Z</updated></feed>"));
        assertFalse(parser.isValidFeedDocument("<feed xmlns='http://www.w3.org/2005/Atom'><id>1</id><updated>2024-01-01T00:00:00Z</updated></feed>"));
        assertFalse(parser.isValidFeedDocument("<feed xmlns='http://www.w3.org/2005/Atom'><id>1</id><title>NoDate</title></feed>"));

        String atomMultiLinks = """
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <id>1</id><title>T</title><updated>2024-01-01T00:00:00Z</updated>
                  <entry>
                    <title>E1</title>
                    <link rel="alternate" type="application/pdf" href="https://example.com/1.pdf"/>
                    <link rel="alternate" type="application/json" href="https://example.com/1.json"/>
                  </entry>
                  <entry>
                    <title>E2</title>
                    <link rel="alternate" type="text/html" href="ftp://example.com/file"/>
                  </entry>
                </feed>
                """;
        assertEquals(1, parser.parse("https://example.com/a", atomMultiLinks).size());

        String rssBadLink = "<rss version='2.0'><channel><item><link>   </link></item><item><link>http://[::1</link></item></channel></rss>";
        assertTrue(parser.parse("https://example.com/r", rssBadLink).isEmpty());

        // addIfNew with null/blank id via reflection
        java.lang.reflect.Method addIfNew = RssFeedParser.class.getDeclaredMethod("addIfNew", java.util.Map.class, FeedItem.class);
        addIfNew.setAccessible(true);
        java.util.Map<Object, FeedItem> map = new java.util.LinkedHashMap<>();
        addIfNew.invoke(parser, map, new FeedItem("s", null, "t", URI.create("https://example.com/1"), null));
        addIfNew.invoke(parser, map, new FeedItem("s", "   ", "t", URI.create("https://example.com/2"), null));
        assertEquals(2, map.size());

        // FeedRegistryService identity with null/blank id + summary(null) + MAX_FEEDS
        java.lang.reflect.Method ident = FeedRegistryService.class.getDeclaredMethod("identity", FeedItem.class);
        ident.setAccessible(true);
        assertNotNull(ident.invoke(null, new FeedItem("s", null, "t", URI.create("https://example.com/1"), null)));
        assertNotNull(ident.invoke(null, new FeedItem("s", "  ", "t", URI.create("https://example.com/1"), null)));

        java.lang.reflect.Method sum = FeedRegistryService.class.getDeclaredMethod("summary",
                Class.forName("dev.henrique.rsscollector.application.FeedRegistryService$FeedState"));
        sum.setAccessible(true);
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> sum.invoke(null, new Object[]{null}));

        FeedDocumentFetcher mockFetcher = mock(FeedDocumentFetcher.class);
        when(mockFetcher.fetch(anyString())).thenReturn("<rss version='2.0'><channel><title>T</title></channel></rss>");
        FeedRegistryService regSvc = new FeedRegistryService(realVal, mockFetcher, new RssFeedValidator(realVal, parser), parser);
        for (int i = 0; i < 100; i++) {
            regSvc.register("https://example.com:8443/feed" + i + ".xml");
        }
        assertThrows(IllegalStateException.class, () -> regSvc.register("https://example.com/overflow.xml"));

        // Controller refresh success branch
        FeedRegistryController ctrl = new FeedRegistryController(regSvc);
        UUID firstId = regSvc.listFeeds().get(0).id();
        assertNotNull(ctrl.refresh(firstId));
    }
}
