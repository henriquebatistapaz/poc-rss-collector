package com.example.rsscollector.application;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
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

    /** Checks a hostname immediately before a network request to reduce DNS-based SSRF risk. */
    public boolean resolvesOnlyToPublicAddresses(String value) {
        if (!isValidFeedUrl(value)) return false;
        try {
            String host = new URI(value.trim()).getHost();
            if (isIpLiteral(host)) return isPublicAddress(InetAddress.getByName(host));
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) return false;
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) return false;
            }
            return true;
        } catch (Exception exception) {
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
            return isPublicAddress(address);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return first != 0 && first != 10 && first != 127 && first < 224
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 169 && second == 254)
                    && !(first == 172 && second >= 16 && second <= 31)
                    && !(first == 192 && second == 0)
                    && !(first == 192 && second == 168)
                    && !(first == 192 && second == 88 && third == 99)
                    && !(first == 198 && (second == 18 || second == 19));
        }
        if (bytes.length == 16) {
            boolean documentationRange = Byte.toUnsignedInt(bytes[0]) == 0x20
                    && Byte.toUnsignedInt(bytes[1]) == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0d
                    && Byte.toUnsignedInt(bytes[3]) == 0xb8;
            return (bytes[0] & 0xfe) != 0xfc && !documentationRange
                    && (Byte.toUnsignedInt(bytes[0]) != 0xfe || (bytes[1] & 0xc0) != 0x80)
                    && Byte.toUnsignedInt(bytes[0]) != 0xff;
        }
        return false;
    }

    private boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("[0-9.]+");
    }
}
