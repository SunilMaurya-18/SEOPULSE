package com.seopulse.website.crawler.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Hostname resolution used for every outbound crawler connection.
 */
@FunctionalInterface
public interface HostResolver {

    List<InetAddress> resolve(String host) throws UnknownHostException;
}
