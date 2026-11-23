package dev.henrique.rsscollector.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Performs a bounded public-feed GET with manual redirect checks. */
@Component
public class BoundedFeedHttpFetcher implements FeedDocumentFetcher {
    private static final int MAX_BYTES = 1_000_000;
    private static final int MAX_REDIRECTS = 3;
    private static final int TIMEOUT_MILLIS = 5_000;
    private final RssFeedLinkValidator linkValidator;
    private final HttpConnectionFactory connectionFactory;

    @Autowired
    public BoundedFeedHttpFetcher(RssFeedLinkValidator linkValidator) {
        this(linkValidator, uri -> (HttpURLConnection) uri.toURL().openConnection());
    }

    BoundedFeedHttpFetcher(RssFeedLinkValidator linkValidator, HttpConnectionFactory connectionFactory) {
        this.linkValidator = linkValidator;
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public String fetch(String feedUrl) throws IOException {
        URI current = URI.create(feedUrl);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            String currentUrl = current.toString();
            if (!linkValidator.resolvesOnlyToPublicAddresses(currentUrl)) {
                throw new IOException("Feed URL resolves to a non-public or invalid destination");
            }
            HttpURLConnection connection = connectionFactory.open(current);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("Accept", "application/atom+xml, application/rss+xml, application/xml, text/xml;q=0.9");
            connection.setRequestProperty("User-Agent", "MultiSourceFeedValidator/0.1 (+public-feed-check)");
            connection.setInstanceFollowRedirects(false);
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects == MAX_REDIRECTS) {
                        throw new IOException("Feed redirect is missing or exceeds the limit");
                    }
                    current = current.resolve(location);
                    if (!linkValidator.isValidFeedUrl(current.toString())) {
                        throw new IOException("Feed redirected to an unsafe URL");
                    }
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new IOException("Feed server returned HTTP " + status);
                }
                try (var input = connection.getInputStream(); var output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_BYTES) throw new IOException("Feed response exceeds the size limit");
                        output.write(buffer, 0, read);
                    }
                    return output.toString(StandardCharsets.UTF_8);
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Feed redirect limit exceeded");
    }

    @FunctionalInterface
    interface HttpConnectionFactory {
        HttpURLConnection open(URI uri) throws IOException;
    }
}
