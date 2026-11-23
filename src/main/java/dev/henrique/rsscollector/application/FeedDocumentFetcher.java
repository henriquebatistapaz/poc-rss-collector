package dev.henrique.rsscollector.application;

import java.io.IOException;

public interface FeedDocumentFetcher {
    String fetch(String feedUrl) throws IOException, InterruptedException;
}
