package com.example.rsslinkcollector.application;

public class RssFeedValidator {

    private final RssFeedLinkValidator linkValidator;
    private final RssFeedParser parser;

    public RssFeedValidator(RssFeedLinkValidator linkValidator, RssFeedParser parser) {
        this.linkValidator = linkValidator;
        this.parser = parser;
    }

    /** Validates the feed URL shape and confirms that the supplied document is RSS 2.0 XML. */
    public boolean isValidRssFeed(String feedUrl, String xml) {
        return linkValidator.isValidFeedUrl(feedUrl) && parser.isValidRssDocument(xml);
    }
}
