package com.example.rsscollector.application;

import com.example.rsscollector.domain.FeedItem;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.springframework.stereotype.Component;

@Component
public class RssFeedParser {
    private static final int MAX_DOCUMENT_CHARS = 1_000_000;
    private static final int MAX_ITEMS = 1_000;

    private final RssFeedLinkValidator linkValidator;

    public RssFeedParser(RssFeedLinkValidator linkValidator) {
        this.linkValidator = linkValidator;
    }

    public List<FeedItem> parse(String feedUrl, String xml) {
        if (!linkValidator.isValidFeedUrl(feedUrl)) {
            throw new IllegalArgumentException("Feed URL must be an absolute public HTTP(S) URL");
        }
        Document document = parseDocument(xml);
        Element root = document.getDocumentElement();
        List<FeedItem> items = new ArrayList<>();
        if (isRssRoot(root)) {
            Element channel = firstChild(root, "channel");
            if (channel == null) throw new IllegalArgumentException("RSS feed must contain a channel");
            for (Element item : childElements(channel, "item")) {
                if (items.size() >= MAX_ITEMS) break;
                FeedItem parsed = parseRssItem(feedUrl, item);
                if (parsed != null) items.add(parsed);
            }
        } else if (isValidAtomFeed(root)) {
            for (Element entry : childElements(root, "entry")) {
                if (items.size() >= MAX_ITEMS) break;
                FeedItem parsed = parseAtomEntry(feedUrl, entry);
                if (parsed != null) items.add(parsed);
            }
        } else {
            throw new IllegalArgumentException("Document is neither RSS 2.0 nor Atom 1.0");
        }
        return List.copyOf(items);
    }

    boolean isValidRssDocument(String xml) {
        return isValidFeedDocument(xml);
    }

    boolean isValidFeedDocument(String xml) {
        try {
            Element root = parseDocument(xml).getDocumentElement();
            return (isRssRoot(root) && firstChild(root, "channel") != null) || isValidAtomFeed(root);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private FeedItem parseRssItem(String feedUrl, Element element) {
        String linkValue = childText(element, "link");
        URI link = normalizeHttpUrl(linkValue);
        if (link == null) {
            return null;
        }
        String guid = childText(element, "guid");
        String id = guid == null ? link.toString() : guid;
        String title = childText(element, "title");
        Instant publishedAt = parseDate(childText(element, "pubDate"));
        return new FeedItem(feedUrl.trim(), id, title, link, publishedAt);
    }

    private FeedItem parseAtomEntry(String feedUrl, Element entry) {
        URI link = atomLink(feedUrl, entry);
        if (link == null) return null;
        String id = childText(entry, "id");
        if (id == null) id = link.toString();
        String title = childText(entry, "title");
        String dateValue = childText(entry, "published");
        if (dateValue == null) dateValue = childText(entry, "updated");
        return new FeedItem(feedUrl.trim(), id, title, link, parseDate(dateValue));
    }

    private URI atomLink(String feedUrl, Element entry) {
        Element fallback = null;
        for (Element linkElement : childElements(entry, "link")) {
            String rel = linkElement.getAttribute("rel");
            if (rel.isBlank()) rel = "alternate";
            if (!"alternate".equals(rel)) continue;
            String type = linkElement.getAttribute("type");
            if (fallback == null) fallback = linkElement;
            if (type.isBlank() || type.equalsIgnoreCase("text/html")
                    || type.equalsIgnoreCase("application/xhtml+xml")) {
                URI candidate = resolveAtomHref(feedUrl, linkElement.getAttribute("href"));
                if (candidate != null) return candidate;
            }
        }
        return fallback == null ? null : resolveAtomHref(feedUrl, fallback.getAttribute("href"));
    }

    private URI resolveAtomHref(String feedUrl, String href) {
        if (href == null || href.isBlank()) return null;
        try {
            URI resolved = new URI(feedUrl).resolve(href.trim());
            return normalizeHttpUrl(resolved.toString());
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private boolean isRssRoot(Element root) {
        return "rss".equals(localName(root)) && "2.0".equals(root.getAttribute("version"));
    }

    private boolean isAtomRoot(Element root) {
        return "feed".equals(localName(root))
                && "http://www.w3.org/2005/Atom".equals(root.getNamespaceURI());
    }

    private boolean isValidAtomFeed(Element root) {
        if (!isAtomRoot(root)) return false;
        return childText(root, "id") != null && childText(root, "title") != null
                && parseDate(childText(root, "updated")) != null;
    }

    private Document parseDocument(String xml) {
        if (xml == null || xml.isBlank() || xml.length() > MAX_DOCUMENT_CHARS) {
            throw new IllegalArgumentException("RSS XML is empty or exceeds the size limit");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void fatalError(org.xml.sax.SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid or unsafe RSS XML", exception);
        }
    }

    private URI normalizeHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value.trim()).normalize();
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            int port = uri.getPort();
            if ((scheme.equalsIgnoreCase("http") && port == 80)
                    || (scheme.equalsIgnoreCase("https") && port == 443)) {
                port = -1;
            }
            return new URI(scheme.toLowerCase(Locale.ROOT), null, host.toLowerCase(Locale.ROOT), port,
                    uri.getPath(), uri.getQuery(), null).normalize();
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private Instant parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant();
            } catch (DateTimeParseException invalidDate) {
                return null;
            }
        }
    }

    private Element firstChild(Element parent, String name) {
        for (Element child : childElements(parent, name)) {
            return child;
        }
        return null;
    }

    private List<Element> childElements(Element parent, String name) {
        List<Element> matches = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element child && name.equals(localName(child))) {
                matches.add(child);
            }
        }
        return matches;
    }

    private String childText(Element parent, String name) {
        Element child = firstChild(parent, name);
        if (child == null) {
            return null;
        }
        String text = child.getTextContent().trim();
        return text.isEmpty() ? null : text;
    }

    private String localName(Element element) {
        String localName = element.getLocalName();
        return localName == null ? element.getTagName() : localName;
    }
}
