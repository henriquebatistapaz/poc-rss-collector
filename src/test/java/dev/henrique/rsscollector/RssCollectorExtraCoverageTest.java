package dev.henrique.rsscollector;

import dev.henrique.rsscollector.api.FeedRegistryController;
import dev.henrique.rsscollector.api.FeedValidationController;
import dev.henrique.rsscollector.application.BoundedFeedHttpFetcher;
import dev.henrique.rsscollector.application.FeedDocumentFetcher;
import dev.henrique.rsscollector.application.FeedRegistryService;
import dev.henrique.rsscollector.application.FeedUrlValidationService;
import dev.henrique.rsscollector.application.RssFeedLinkNormalizer;
import dev.henrique.rsscollector.application.RssFeedLinkValidator;
import dev.henrique.rsscollector.application.RssFeedParser;
import dev.henrique.rsscollector.application.RssFeedValidator;
import dev.henrique.rsscollector.domain.FeedItem;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RssCollectorExtraCoverageTest {

    @Test
    void linkValidatorAndNormalizerCoverAllBranches() throws Exception {
        RssFeedLinkValidator val = new RssFeedLinkValidator();
        assertFalse(val.isValidFeedUrl(null));
        assertFalse(val.isValidFeedUrl("   "));
        assertFalse(val.isValidFeedUrl("http://[::1"));
        assertFalse(val.isValidFeedUrl("/relative"));
        assertFalse(val.isValidFeedUrl("https:///nohost"));
        assertFalse(val.isValidFeedUrl("https://user:pass@example.com/rss"));
        assertFalse(val.isValidFeedUrl("https://example.com/rss#frag"));
        assertFalse(val.isValidFeedUrl("ftp://example.com/rss"));
        assertFalse(val.isValidFeedUrl("https://localhost/rss"));
        assertFalse(val.isValidFeedUrl("https://sub.localhost/rss"));
        assertFalse(val.isValidFeedUrl("https://my.local/rss"));
        assertFalse(val.isValidFeedUrl("https://my.internal/rss"));
        assertFalse(val.isValidFeedUrl("https://999.999.999.999/rss"));

        assertFalse(val.resolvesOnlyToPublicAddresses("http://localhost/rss"));
        assertTrue(val.resolvesOnlyToPublicAddresses("https://8.8.8.8/rss"));
        assertFalse(val.resolvesOnlyToPublicAddresses("https://127.0.0.1/rss"));
        assertFalse(val.resolvesOnlyToPublicAddresses("https://nonexistent.invalid.domain.test/rss"));

        Method isPub = RssFeedLinkValidator.class.getDeclaredMethod("isPublicAddress", InetAddress.class);
        isPub.setAccessible(true);

        byte[][] badIps = new byte[][]{
                {0, 0, 0, 0}, {0, 1, 1, 1}, {10, 0, 0, 1}, {127, 0, 0, 1}, {(byte) 224, 0, 0, 1},
                {100, 64, 0, 1}, {100, 127, 0, 1}, {(byte) 169, (byte) 254, 1, 1},
                {(byte) 172, 16, 0, 1}, {(byte) 172, 31, 0, 1}, {(byte) 192, 0, 0, 1},
                {(byte) 192, (byte) 168, 1, 1}, {(byte) 192, 88, 99, 1},
                {(byte) 198, 18, 0, 1}, {(byte) 198, 19, 0, 1},
                {(byte) 0xfc, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {(byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}
        };
        for (byte[] ip : badIps) {
            assertFalse((Boolean) isPub.invoke(val, InetAddress.getByAddress("h", ip)));
        }

        byte[][] goodIps = new byte[][]{
                {8, 8, 8, 8}, {100, 63, 0, 1}, {100, (byte) 128, 0, 1}, {(byte) 169, 1, 1, 1},
                {(byte) 172, 15, 0, 1}, {(byte) 172, 32, 0, 1}, {(byte) 192, 1, 1, 1},
                {(byte) 192, 88, 1, 1}, {(byte) 198, 20, 0, 1},
                {0x26, 0x06, 0x47, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x02, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0e, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0d, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {(byte) 0xfe, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}
        };
        for (byte[] ip : goodIps) {
            assertTrue((Boolean) isPub.invoke(val, InetAddress.getByAddress("h", ip)));
        }

        // Normalizer
        RssFeedLinkNormalizer norm = new RssFeedLinkNormalizer();
        assertNull(norm.normalize(null));
        assertNull(norm.normalize(URI.create("/rel")));
        assertNull(norm.normalize(URI.create("https:///nohost")));
        assertNull(norm.normalize(URI.create("https://u:p@e.com")));
        assertNull(norm.normalize(URI.create("ftp://e.com")));
        assertEquals("http://e.com/a", norm.normalize(URI.create("http://e.com:80/a?utm_source=x&fbclid=y")).toString());
        assertEquals("https://e.com:8443/a?flag&k=v", norm.normalize(URI.create("https://e.com:8443/a?flag&k=v&gclid=1")).toString());
        assertNull(norm.normalize(URI.create("https://[2606:4700::1]:443/a?")));
    }

    @Test
    void controllersServicesAndParserCoverBranches() throws Exception {
        // FeedValidationController
        FeedUrlValidationService valSvc = mock(FeedUrlValidationService.class);
        FeedValidationController valCtrl = new FeedValidationController(valSvc);
        assertThrows(ResponseStatusException.class, () -> valCtrl.validate(null));
        assertThrows(ResponseStatusException.class, () -> valCtrl.validate(new FeedValidationController.FeedValidationRequest(null)));
        assertThrows(ResponseStatusException.class, () -> valCtrl.validate(new FeedValidationController.FeedValidationRequest("  ")));

        when(valSvc.validate("bad")).thenThrow(new IllegalArgumentException("bad"));
        assertThrows(ResponseStatusException.class, () -> valCtrl.validate(new FeedValidationController.FeedValidationRequest("bad")));

        when(valSvc.validate("io")).thenThrow(new IOException("io"));
        assertThrows(ResponseStatusException.class, () -> valCtrl.validate(new FeedValidationController.FeedValidationRequest("io")));

        when(valSvc.validate("int")).thenThrow(new InterruptedException("int"));
        assertThrows(ResponseStatusException.class, () -> valCtrl.validate(new FeedValidationController.FeedValidationRequest("int")));
        Thread.interrupted();

        // FeedRegistryController
        FeedRegistryService regSvc = mock(FeedRegistryService.class);
        FeedRegistryController regCtrl = new FeedRegistryController(regSvc);
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(null));
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(new FeedRegistryController.FeedRegistrationRequest(null)));
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(new FeedRegistryController.FeedRegistrationRequest("  ")));

        when(regSvc.register("bad")).thenThrow(new IllegalArgumentException("bad"));
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(new FeedRegistryController.FeedRegistrationRequest("bad")));

        when(regSvc.register("full")).thenThrow(new IllegalStateException("full"));
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(new FeedRegistryController.FeedRegistrationRequest("full")));

        when(regSvc.register("io")).thenThrow(new IOException("io"));
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(new FeedRegistryController.FeedRegistrationRequest("io")));

        when(regSvc.register("int")).thenThrow(new InterruptedException("int"));
        assertThrows(ResponseStatusException.class, () -> regCtrl.register(new FeedRegistryController.FeedRegistrationRequest("int")));
        Thread.interrupted();

        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("not found")).when(regSvc).refresh(id);
        assertThrows(ResponseStatusException.class, () -> regCtrl.refresh(id));
        doThrow(new IOException("io")).when(regSvc).refresh(id);
        assertThrows(ResponseStatusException.class, () -> regCtrl.refresh(id));
        doThrow(new InterruptedException("int")).when(regSvc).refresh(id);
        assertThrows(ResponseStatusException.class, () -> regCtrl.refresh(id));
        Thread.interrupted();

        assertThrows(ResponseStatusException.class, () -> regCtrl.listLinks(null, Instant.now(), Instant.now().minusSeconds(10)));

        // FeedRegistryService branches
        RssFeedLinkValidator linkVal = new RssFeedLinkValidator();
        FeedDocumentFetcher fetcher = mock(FeedDocumentFetcher.class);
        RssFeedParser parser = new RssFeedParser(linkVal);
        RssFeedValidator validator = new RssFeedValidator(linkVal, parser);
        FeedRegistryService realReg = new FeedRegistryService(linkVal, fetcher, validator, parser);

        String validRss = """
                <rss version="2.0"><channel><title>T</title>
                <item><title>1</title><link>https://example.com/1</link><pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate></item>
                <item><title>2</title><link>https://example.com/2</link></item>
                </channel></rss>
                """;
        when(fetcher.fetch(anyString())).thenReturn(validRss);
        var s1 = realReg.register("http://example.com:80/feed.xml?a=1");
        var s2 = realReg.register("https://example.org:443/feed.xml");
        assertEquals(s1.id(), realReg.register("http://example.com/feed.xml?a=1").id());
        assertEquals(2, realReg.listFeeds().size());

        // listLinks with feedId present, missing, and date filters
        assertEquals(2, realReg.listLinks(s1.id(), null, null).size());
        assertTrue(realReg.listLinks(UUID.randomUUID(), null, null).isEmpty());
        assertEquals(1, realReg.listLinks(s1.id(), Instant.parse("2023-01-01T00:00:00Z"), Instant.parse("2025-01-01T00:00:00Z")).size());
        assertTrue(realReg.listLinks(s1.id(), Instant.parse("2025-01-01T00:00:00Z"), null).isEmpty());
        assertTrue(realReg.listLinks(s1.id(), null, Instant.parse("2023-01-01T00:00:00Z")).isEmpty());

        // Invalid XML response on register
        when(fetcher.fetch("https://example.com/bad.xml")).thenReturn("<not-rss/>");
        assertThrows(IOException.class, () -> realReg.register("https://example.com/bad.xml"));
        assertThrows(IllegalArgumentException.class, () -> realReg.register("http://localhost/feed"));
        assertThrows(IllegalArgumentException.class, () -> realReg.refresh(UUID.randomUUID()));

        // Parser Atom & RSS edge cases
        String atomXml = """
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <id>urn:1</id><title>Atom</title><updated>2024-01-01T00:00:00Z</updated>
                  <entry>
                    <title>E1</title>
                    <link rel="enclosure" href="https://example.com/enc"/>
                    <link rel="alternate" type="application/pdf" href="/doc.pdf"/>
                    <updated>invalid-date</updated>
                  </entry>
                  <entry>
                    <title>E2</title>
                    <link href="   "/>
                  </entry>
                  <entry>
                    <id>   </id>
                    <title>E3</title>
                    <link rel="alternate" type="application/xhtml+xml" href="https://example.com/e3"/>
                    <published>2024-01-02T00:00:00Z</published>
                  </entry>
                </feed>
                """;
        List<FeedItem> atomItems = parser.parse("https://example.com/atom.xml", atomXml);
        assertEquals(2, atomItems.size());

        assertNotNull(new BoundedFeedHttpFetcher(linkVal));
  
        // Additional branches for >95%
        RssFeedLinkNormalizer norm = new RssFeedLinkNormalizer();
        assertTrue(linkVal.resolvesOnlyToPublicAddresses("https://example.com/rss"));
        assertEquals("https://e.com", norm.normalize(URI.create("https://e.com:443")).toString());
        assertEquals("http://e.com:8080/p", norm.normalize(URI.create("http://e.com:8080/p")).toString());

        assertThrows(ResponseStatusException.class, () -> regCtrl.listLinks(null, Instant.now(), Instant.now().minusSeconds(10)));
        assertNotNull(regCtrl.listLinks(null, Instant.now().minusSeconds(10), Instant.now()));
        assertNotNull(regCtrl.listLinks(null, Instant.now(), null));
        assertNotNull(regCtrl.listLinks(null, null, Instant.now()));

        // FeedUrlValidationService false branch
        when(fetcher.fetch("https://example.com/invalid.xml")).thenReturn("<not-feed/>");
        FeedUrlValidationService realValSvc = new FeedUrlValidationService(linkVal, fetcher, validator, parser);
        assertFalse(realValSvc.validate("https://example.com/invalid.xml").valid());

        // Parser extra branches
        assertThrows(IllegalArgumentException.class, () -> parser.parse("https://example.com/r", "<rss version='2.0'></rss>"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("https://example.com/r", "<rss version='1.0'><channel/></rss>"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("https://example.com/r", null));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("https://example.com/r", "   "));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("https://example.com/r", "x".repeat(1_000_005)));

        // >1000 items in RSS and Atom + FeedState eviction
        StringBuilder bigRss = new StringBuilder("<rss version='2.0'><channel><title>B</title>");
        StringBuilder bigAtom = new StringBuilder("<feed xmlns='http://www.w3.org/2005/Atom'><id>1</id><title>T</title><updated>2024-01-01T00:00:00Z</updated>");
        for (int i = 0; i < 1005; i++) {
            bigRss.append("<item><title>T").append(i).append("</title><link>https://example.com/").append(i).append("</link></item>");
            bigAtom.append("<entry><title>T").append(i).append("</title><link href='https://example.com/").append(i).append("'/></entry>");
        }
        bigRss.append("</channel></rss>");
        bigAtom.append("</feed>");
        assertEquals(1000, parser.parse("https://example.com/big.xml", bigRss.toString()).size());
        assertEquals(1000, parser.parse("https://example.com/big-atom.xml", bigAtom.toString()).size());

        when(fetcher.fetch("https://example.com/big.xml")).thenReturn(bigRss.toString());
        var bigSum = realReg.register("https://example.com/big.xml");
        String extraRss = "<rss version='2.0'><channel><title>B</title><item><title>New</title><link>https://example.com/new1</link></item></channel></rss>";
        when(fetcher.fetch("https://example.com/big.xml")).thenReturn(extraRss);
        assertEquals(1000, realReg.refresh(bigSum.id()).totalCount());
    }
}
