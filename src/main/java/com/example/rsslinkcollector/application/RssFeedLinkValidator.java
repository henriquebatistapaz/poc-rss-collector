package com.example.rsslinkcollector.application;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class RssFeedLinkValidator {

    public boolean isValidFeedUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isBlank() || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                return false;
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return false;
            }
            return isPublicHost(host);
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isPublicHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("localhost") || normalized.endsWith(".localhost")
                || normalized.endsWith(".local") || normalized.endsWith(".internal")) {
            return false;
        }
        if (!isIpLiteral(normalized)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(normalized);
            byte[] bytes = address.getAddress();
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                return false;
            }
            if (bytes.length == 4) {
                int first = Byte.toUnsignedInt(bytes[0]);
                int second = Byte.toUnsignedInt(bytes[1]);
                return first != 0 && first != 10 && first != 127 && first < 224
                        && !(first == 100 && second >= 64 && second <= 127)
                        && !(first == 169 && second == 254)
                        && !(first == 172 && second >= 16 && second <= 31)
                        && !(first == 192 && second == 0)
                        && !(first == 192 && second == 168)
                        && !(first == 198 && (second == 18 || second == 19));
            }
            return (bytes[0] & 0xfe) != 0xfc;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("[0-9.]+");
    }
}
