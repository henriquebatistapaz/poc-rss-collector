package com.example.rsslinkcollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rsslinkcollector.domain.FeedItem;
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
    void rejectsNonRssDocumentsAndUnsafeXml() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("https://feeds.example.org/feed", "<rss version='2.0'/ >"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("https://feeds.example.org/feed", "<feed xmlns='http://www.w3.org/2005/Atom'/>"));
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
}
