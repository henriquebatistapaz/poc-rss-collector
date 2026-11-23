package dev.henrique.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;

class RssFeedLinkNormalizerTest {
    private final RssFeedLinkNormalizer normalizer = new RssFeedLinkNormalizer();

    @Test
    void canonicalizesHostPortFragmentsAndKnownTrackingParameters() {
        URI normalized = normalizer.normalize(URI.create(
                "HTTPS://Example.ORG:443/a/../story%20one?utm_source=news&id=4&gclid=abc#comments"));

        assertEquals("https://example.org/story%20one?id=4", normalized.toString());
    }

    @Test
    void preservesFunctionalAndUnknownQueryParameters() {
        URI normalized = normalizer.normalize(URI.create("https://example.org/article?lang=pt&ref=partner"));

        assertEquals("https://example.org/article?lang=pt&ref=partner", normalized.toString());
    }

    @Test
    void rejectsNonHttpOrNonAbsoluteLinks() {
        assertNull(normalizer.normalize(URI.create("mailto:person@example.org")));
        assertNull(normalizer.normalize(URI.create("/relative/path")));
    }
}
