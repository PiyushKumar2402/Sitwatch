package com.sitewatch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/** Normalises user supplied URLs and blocks SSRF targets (localhost, private networks, cloud metadata). */
@Component
public class UrlValidator {

    private final boolean allowPrivate;

    public UrlValidator(@Value("${sitewatch.allow-private-hosts:false}") boolean allowPrivate) {
        this.allowPrivate = allowPrivate;
    }

    /** @return the normalised URL, or throws IllegalArgumentException with a user-friendly message. */
    public String validate(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Enter a website URL.");
        String s = raw.trim();
        if (!s.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) s = "https://" + s;

        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("That URL is not valid.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https"))
            throw new IllegalArgumentException("Only http:// and https:// URLs can be monitored.");
        if (uri.getUserInfo() != null)
            throw new IllegalArgumentException("URLs with a username or password are not allowed.");
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new IllegalArgumentException("The URL has no valid host name.");

        if (!allowPrivate) assertPublicHost(host);
        return uri.toString();
    }

    private void assertPublicHost(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isPrivate(address))
                    throw new IllegalArgumentException("Private, local and internal addresses cannot be monitored.");
            }
        } catch (UnknownHostException e) {
            // Not resolvable right now. That is allowed: the site will simply show as DOWN.
        }
    }

    public static boolean isPrivate(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) return true;
        byte[] b = a.getAddress();
        if (a instanceof Inet4Address) {
            return (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;   // 100.64.0.0/10 carrier-grade NAT
        }
        if (a instanceof Inet6Address) {
            return (b[0] & 0xFE) == 0xFC;                           // fc00::/7 unique local
        }
        return false;
    }
}
