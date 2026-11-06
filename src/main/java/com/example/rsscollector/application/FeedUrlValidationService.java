package com.example.rsscollector.application;

import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class FeedUrlValidationService {
    private final RssFeedLinkValidator linkValidator;
    private final FeedDocumentFetcher fetcher;
    private final RssFeedValidator validator;
    private final RssFeedParser parser;

    public FeedUrlValidationService(RssFeedLinkValidator linkValidator, FeedDocumentFetcher fetcher,
                                    RssFeedValidator validator, RssFeedParser parser) {
        this.linkValidator = linkValidator;
        this.fetcher = fetcher;
        this.validator = validator;
        this.parser = parser;
    }

    public ValidationResult validate(String feedUrl) throws IOException, InterruptedException {
        if (!linkValidator.isValidFeedUrl(feedUrl)) {
            throw new IllegalArgumentException("feedUrl must be an absolute public HTTP(S) URL");
        }
        String xml = fetcher.fetch(feedUrl);
        boolean valid = validator.isValidRssFeed(feedUrl, xml);
        int itemCount = valid ? parser.parse(feedUrl, xml).size() : 0;
        return new ValidationResult(feedUrl, valid, itemCount,
                valid ? "Valid RSS 2.0 or Atom 1.0 feed" : "Response is not a valid RSS 2.0 or Atom 1.0 feed");
    }

    public record ValidationResult(String feedUrl, boolean valid, int itemCount, String message) {}
}
