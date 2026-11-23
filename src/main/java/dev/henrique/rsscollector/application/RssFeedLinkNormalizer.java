package dev.henrique.rsscollector.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Canonicalizes article links without discarding query parameters that may change the resource. */
@Component
public class RssFeedLinkNormalizer {
    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid", "yclid");

    public URI normalize(URI value) {
        if (value == null || value.getScheme() == null || value.getHost() == null
                || value.getUserInfo() != null
                || (!"http".equalsIgnoreCase(value.getScheme()) && !"https".equalsIgnoreCase(value.getScheme()))) {
            return null;
        }
        String scheme = value.getScheme().toLowerCase(Locale.ROOT);
        int port = value.getPort();
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) port = -1;
        try {
            String host = value.getHost().toLowerCase(Locale.ROOT);
            String authority = host.contains(":") ? "[" + host + "]" : host;
            String path = value.getRawPath() == null ? "" : value.getRawPath();
            String query = normalizeQuery(value.getRawQuery());
            return new URI(scheme + "://" + authority + (port < 0 ? "" : ":" + port) + path
                    + (query == null ? "" : "?" + query)).normalize();
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return null;
        List<String> kept = new ArrayList<>();
        for (String parameter : rawQuery.split("&", -1)) {
            String name = parameter.contains("=") ? parameter.substring(0, parameter.indexOf('=')) : parameter;
            String decodedName = URLDecoder.decode(name.replace("+", "%2B"), StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT);
            if (decodedName.startsWith("utm_") || TRACKING_PARAMETERS.contains(decodedName)) continue;
            kept.add(parameter);
        }
        return kept.isEmpty() ? null : String.join("&", kept);
    }
}
