package com.seopulse.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The client address as seen by the servlet container. Behind the reverse
 * proxy, prod sets {@code server.forward-headers-strategy=framework} so
 * this is the real client IP, not the proxy's. X-Forwarded-For is never
 * read directly because clients can forge it.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return address == null || address.isBlank() ? "unknown" : address;
    }
}
