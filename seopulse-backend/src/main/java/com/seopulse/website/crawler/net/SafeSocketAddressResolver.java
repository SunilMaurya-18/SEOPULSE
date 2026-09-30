package com.seopulse.website.crawler.net;

import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Resolves a hostname once and validates every address before the HTTP
 * client connects to it. Because validation and connection use the same
 * resolution, a DNS answer that changes between checks (DNS rebinding)
 * cannot redirect the crawler to an internal address. TLS still uses the
 * original hostname for SNI and certificate checks.
 */
@Component
public class SafeSocketAddressResolver implements SocketAddressResolver {

    private final HostResolver hostResolver;
    private final NetworkPolicy networkPolicy;

    public SafeSocketAddressResolver(
            HostResolver hostResolver,
            NetworkPolicy networkPolicy
    ) {
        this.hostResolver = hostResolver;
        this.networkPolicy = networkPolicy;
    }

    @Override
    public void resolve(
            String host,
            int port,
            Map<String, Object> context,
            Promise<List<InetSocketAddress>> promise
    ) {
        try {
            List<InetSocketAddress> addresses = resolveAllowed(host).stream()
                    .map(address -> new InetSocketAddress(address, port))
                    .toList();

            promise.succeeded(addresses);
        } catch (UnknownHostException ex) {
            promise.failed(ex);
        }
    }

    /**
     * Resolves {@code host} and fails if any resulting address is blocked.
     */
    public List<InetAddress> resolveAllowed(String host) throws UnknownHostException {

        List<InetAddress> addresses = hostResolver.resolve(host);

        if (addresses.isEmpty()) {
            throw new UnknownHostException(host);
        }

        for (InetAddress address : addresses) {
            if (networkPolicy.isBlocked(address)) {
                throw new BlockedAddressException(host);
            }
        }

        return addresses;
    }
}
