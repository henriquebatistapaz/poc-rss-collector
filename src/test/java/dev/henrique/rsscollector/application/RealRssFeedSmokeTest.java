package dev.henrique.rsscollector.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@EnabledIfEnvironmentVariable(named = "RSS_EXTERNAL_TESTS", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealRssFeedSmokeTest {
    private static final int MAX_BYTES = 1_000_000;
    private final RssFeedLinkValidator linkValidator = new RssFeedLinkValidator();
    private final RssFeedParser parser = new RssFeedParser(linkValidator);
    private final RssFeedValidator validator = new RssFeedValidator(linkValidator, parser);

    Stream<String> realRssFeedUrls() {
        return Stream.of(
                "https://www.nasa.gov/news-release/feed/",
                "https://www.nasa.gov/feeds/iotd-feed/",
                "https://feeds.bbci.co.uk/news/rss.xml");
    }

    @ParameterizedTest
    @MethodSource("realRssFeedUrls")
    void downloadsAndParsesPublicRssFeed(String feedUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(feedUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            assertEquals(200, response.statusCode(), "Feed must respond with HTTP 200: " + feedUrl);
            byte[] xmlBytes = body.readNBytes(MAX_BYTES + 1);
            assertTrue(xmlBytes.length <= MAX_BYTES, "Feed exceeds the smoke-test size limit: " + feedUrl);
            String xml = new String(xmlBytes, java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(validator.isValidRssFeed(feedUrl, xml), "URL and XML must validate as RSS: " + feedUrl);
            assertFalse(parser.parse(feedUrl, xml).isEmpty(), "Feed should contain at least one valid item: " + feedUrl);
        }
    }
}
