package dev.henrique.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.henrique.rsscollector.domain.FeedItem;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RssFeedParserTest {
    private final RssFeedParser parser = new RssFeedParser(new RssFeedLinkValidator());

    @Test
    void parsesRssItemsAndNormalizesLinks() {
        String xml = """
                <rss version="2.0"><channel><title>Example</title>
                  <item><guid>story-1</guid><title> First story </title>
                    <link>HTTPS://Example.ORG:443/news/../story?id=1#top</link>
                    <pubDate>Wed, 05 Nov 2025 12:00:00 GMT</pubDate></item>
                  <item><title>Second story</title><link>https://example.org/second</link></item>
                </channel></rss>
                """;

        List<FeedItem> items = parser.parse("https://feeds.example.org/main.xml", xml);

        assertEquals(2, items.size());
        assertEquals("story-1", items.get(0).id());
        assertEquals("First story", items.get(0).title());
        assertEquals("https://example.org/story?id=1", items.get(0).link().toString());
        assertEquals(Instant.parse("2025-11-05T12:00:00Z"), items.get(0).publishedAt());
        assertEquals("https://example.org/second", items.get(1).id());
        assertNull(items.get(1).publishedAt());
        assertEquals("https://feeds.example.org/main.xml", items.get(0).sourceFeed());
    }

    @Test
    void skipsMalformedItemsButKeepsValidItems() {
        String xml = """
                <rss version="2.0"><channel>
                  <item><title>Missing link</title></item>
                  <item><title>Bad link</title><link>javascript:alert(1)</link></item>
                  <item><title>Kept story</title><link>https://example.org/kept</link></item>
                </channel></rss>
                """;

        List<FeedItem> items = parser.parse("https://feeds.example.org/main.xml", xml);

        assertEquals(1, items.size());
        assertEquals("Kept story", items.getFirst().title());
    }

    @Test
    void removesTrackingParametersAndDeduplicatesByFeedAndItemIdentity() {
        String xml = """
                <rss version="2.0"><channel>
                  <item><guid>story-1</guid><title>First version</title>
                    <link>HTTPS://Example.ORG:443/news?id=1&amp;utm_source=mail#top</link></item>
                  <item><guid>story-1</guid><title>Duplicate identity</title>
                    <link>https://example.org/other</link></item>
                  <item><title>Same canonical link</title>
                    <link>https://example.org/news?id=1&amp;fbclid=tracking</link></item>
                  <item><title>Duplicate fallback link</title>
                    <link>https://example.org/news?id=1&amp;utm_medium=email</link></item>
                </channel></rss>
                """;

        List<FeedItem> items = parser.parse("https://feeds.example.org/feed.xml", xml);

        assertEquals(2, items.size());
        assertEquals("https://example.org/news?id=1", items.getFirst().link().toString());
        assertEquals("https://example.org/news?id=1", items.getLast().link().toString());
        assertEquals("First version", items.getFirst().title());
    }

    @Test
    void rejectsMalformedDocumentsAndUnsafeXml() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("https://feeds.example.org/feed", "<rss version='2.0'/ >"));
        assertEquals(List.of(), parser.parse("https://feeds.example.org/feed",
                "<feed xmlns='http://www.w3.org/2005/Atom'><id>urn:test</id><title>Test</title>"
                        + "<updated>2026-11-05T10:00:00Z</updated></feed>"));
        String doctype = "<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"
                + "<rss version='2.0'><channel><item><title>&x;</title></item></channel></rss>";
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("https://feeds.example.org/feed", doctype));
    }

    @Test
    void rejectsInvalidFeedUrlsAndOversizedXml() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("http://127.0.0.1/feed.xml", "<rss version='2.0'><channel/></rss>"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("https://feeds.example.org/feed", "x".repeat(1_000_001)));
    }

    @Test
    void parsesAtomEntriesUsesAlternateLinksAndResolvesRelativeUrls() {
        String xml = """
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <id>urn:feed:example</id><title>Example Atom feed</title>
                  <updated>2026-11-05T10:00:00Z</updated>
                  <entry><id>tag:example.org,2026:1</id><title> Atom story </title>
                    <link rel="self" href="/self.xml" type="application/atom+xml"/>
                    <link rel="alternate" href="/stories/one" type="text/html"/>
                    <published>2026-11-05T10:15:30-03:00</published>
                  </entry>
                  <entry><title>Fallback identity</title><link href="https://example.org/two"/>
                    <updated>2026-11-05T13:30:00Z</updated></entry>
                </feed>
                """;

        List<FeedItem> items = parser.parse("https://feeds.example.org/atom/index.xml", xml);

        assertEquals(2, items.size());
        assertEquals("tag:example.org,2026:1", items.get(0).id());
        assertEquals("Atom story", items.get(0).title());
        assertEquals("https://feeds.example.org/stories/one", items.get(0).link().toString());
        assertEquals(Instant.parse("2026-11-05T13:15:30Z"), items.get(0).publishedAt());
        assertEquals("https://example.org/two", items.get(1).id());
        assertEquals(Instant.parse("2026-11-05T13:30:00Z"), items.get(1).publishedAt());
    }

    @Test
    void skipsAtomEntriesWithoutSafeAlternateLinksAndAcceptsAtomNamespaceOnly() {
        String xml = """
                <a:feed xmlns:a="http://www.w3.org/2005/Atom">
                  <a:id>urn:feed:test</a:id><a:title>Test</a:title><a:updated>2026-11-05T10:00:00Z</a:updated>
                  <a:entry><a:id>unsafe</a:id><a:link rel="alternate" href="javascript:alert(1)"/></a:entry>
                  <a:entry><a:id>kept</a:id><a:link href="https://example.org/kept"/></a:entry>
                </a:feed>
                """;
        assertEquals(1, parser.parse("https://feeds.example.org/atom", xml).size());
        assertThrows(IllegalArgumentException.class, () -> parser.parse("https://feeds.example.org/atom",
                "<feed><entry><link href='https://example.org/a'/></entry></feed>"));
    }
}
