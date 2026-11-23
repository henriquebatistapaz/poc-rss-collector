package dev.henrique.rsscollector.application;

import org.springframework.stereotype.Service;

@Service
public class RssFeedValidator {

    private final RssFeedLinkValidator linkValidator;
    private final RssFeedParser parser;

    public RssFeedValidator(RssFeedLinkValidator linkValidator, RssFeedParser parser) {
        this.linkValidator = linkValidator;
        this.parser = parser;
    }

    /** Validates a safe public HTTP(S) feed URL and confirms RSS 2.0 or Atom 1.0 XML. */
    public boolean isValidRssFeed(String feedUrl, String xml) {
        return linkValidator.isValidFeedUrl(feedUrl) && parser.isValidFeedDocument(xml);
    }
}
