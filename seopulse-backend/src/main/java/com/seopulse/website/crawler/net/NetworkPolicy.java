package com.seopulse.website.crawler.net;

import com.seopulse.website.crawler.CrawlerProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * Decides which IP addresses the crawler may connect to.
 * Only globally routable unicast addresses are allowed.
 */
@Component
public class NetworkPolicy {

    private final CrawlerProperties properties;

    public NetworkPolicy(CrawlerProperties properties) {
        this.properties = properties;
    }

    public boolean isBlocked(InetAddress address) {

        if (properties.isAllowPrivateNetworks()) {
            return false;
        }

        byte[] bytes = address.getAddress();

        return bytes.length == 4
                ? isBlockedIpv4(bytes)
                : isBlockedIpv6(bytes);
    }

    static boolean isBlockedIpv4(byte[] b) {

        int first = b[0] & 0xff;
        int second = b[1] & 0xff;
        int third = b[2] & 0xff;

        return first == 0                                               // 0.0.0.0/8 "this network"
                || first == 10                                          // 10.0.0.0/8 private
                || (first == 100 && (second & 0xc0) == 64)              // 100.64.0.0/10 carrier-grade NAT
                || first == 127                                         // 127.0.0.0/8 loopback
                || (first == 169 && second == 254)                      // 169.254.0.0/16 link-local, cloud metadata
                || (first == 172 && (second & 0xf0) == 16)              // 172.16.0.0/12 private
                || (first == 192 && second == 0 && third == 0)          // 192.0.0.0/24 IETF protocol assignments
                || (first == 192 && second == 0 && third == 2)          // 192.0.2.0/24 documentation
                || (first == 192 && second == 88 && third == 99)        // 192.88.99.0/24 6to4 relay anycast
                || (first == 192 && second == 168)                      // 192.168.0.0/16 private
                || (first == 198 && (second & 0xfe) == 18)              // 198.18.0.0/15 benchmarking
                || (first == 198 && second == 51 && third == 100)       // 198.51.100.0/24 documentation
                || (first == 203 && second == 0 && third == 113)        // 203.0.113.0/24 documentation
                || first >= 224;                                        // 224.0.0.0/4 multicast, 240.0.0.0/4 reserved, broadcast
    }

    static boolean isBlockedIpv6(byte[] b) {

        if (allZero(b, 0, 15) && (b[15] == 0 || b[15] == 1)) {
            return true;                                                // :: unspecified, ::1 loopback
        }

        int first = b[0] & 0xff;
        int second = b[1] & 0xff;

        if ((first & 0xfe) == 0xfc) {
            return true;                                                // fc00::/7 unique local
        }

        if (first == 0xfe && (second & 0xc0) == 0x80) {
            return true;                                                // fe80::/10 link-local
        }

        if (first == 0xfe && (second & 0xc0) == 0xc0) {
            return true;                                                // fec0::/10 deprecated site-local
        }

        if (first == 0xff) {
            return true;                                                // ff00::/8 multicast
        }

        if (first == 0x20 && second == 0x01 && (b[2] & 0xff) == 0x0d && (b[3] & 0xff) == 0xb8) {
            return true;                                                // 2001:db8::/32 documentation
        }

        if (first == 0x20 && second == 0x01 && b[2] == 0 && b[3] == 0) {
            return true;                                                // 2001::/32 Teredo tunnelling
        }

        if (allZero(b, 0, 10) && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff) {
            return isBlockedIpv4(Arrays.copyOfRange(b, 12, 16));        // ::ffff:0:0/96 IPv4-mapped
        }

        if (allZero(b, 0, 12)) {
            return isBlockedIpv4(Arrays.copyOfRange(b, 12, 16));        // ::/96 IPv4-compatible (deprecated)
        }

        if (first == 0x00 && second == 0x64 && (b[2] & 0xff) == 0xff && (b[3] & 0xff) == 0x9b
                && allZero(b, 4, 12)) {
            return isBlockedIpv4(Arrays.copyOfRange(b, 12, 16));        // 64:ff9b::/96 NAT64
        }

        if (first == 0x20 && second == 0x02) {
            return isBlockedIpv4(Arrays.copyOfRange(b, 2, 6));          // 2002::/16 6to4
        }

        return false;
    }

    private static boolean allZero(byte[] b, int fromInclusive, int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
