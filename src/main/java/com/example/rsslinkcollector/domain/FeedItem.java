package com.example.rsslinkcollector.domain;

import java.net.URI;
import java.time.Instant;

public record FeedItem(String sourceFeed, String id, String title, URI link, Instant publishedAt) {
}
