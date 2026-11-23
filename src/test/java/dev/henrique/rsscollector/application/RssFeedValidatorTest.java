package dev.henrique.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RssFeedValidatorTest {
    private static final String VALID_RSS = """
            <rss version="2.0"><channel><title>Sample</title></channel></rss>
            """;

    private final RssFeedLinkValidator linkValidator = new RssFeedLinkValidator();
    private final RssFeedParser parser = new RssFeedParser(linkValidator);
    private final RssFeedValidator validator = new RssFeedValidator(linkValidator, parser);

    @Test
    void acceptsValidRssUrlAndDocument() {
        assertTrue(validator.isValidRssFeed("https://feeds.example.org/sample.xml", VALID_RSS));
    }

    @Test
    void rejectsUnsafeUrlOrDocumentThatIsNeitherRssNorAtom() {
        assertFalse(validator.isValidRssFeed("file:///tmp/feed.xml", VALID_RSS));
        assertFalse(validator.isValidRssFeed("https://feeds.example.org/feed", "<html/>"));
        assertFalse(validator.isValidRssFeed("https://feeds.example.org/feed", "<rss version='2.0'/ >"));
        assertFalse(validator.isValidRssFeed("https://feeds.example.org/feed",
                "<feed xmlns='http://www.w3.org/2005/Atom'><title>Atom</title></feed>"));
    }

    @Test
    void rejectsXmlWithExternalEntities() {
        String xxe = """
                <!DOCTYPE rss [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <rss version="2.0"><channel><title>&secret;</title></channel></rss>
                """;
        assertFalse(validator.isValidRssFeed("https://feeds.example.org/feed", xxe));
    }

    @Test
    void acceptsAtom10Document() {
        String atom = "<feed xmlns='http://www.w3.org/2005/Atom'><id>urn:test</id><title>Sample</title>"
                + "<updated>2026-11-05T10:00:00Z</updated></feed>";
        assertTrue(validator.isValidRssFeed("https://feeds.example.org/atom", atom));
    }
}
