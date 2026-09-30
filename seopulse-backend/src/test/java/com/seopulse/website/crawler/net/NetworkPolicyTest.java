package com.seopulse.website.crawler.net;

import com.seopulse.website.crawler.CrawlerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkPolicyTest {

    private final NetworkPolicy policy = new NetworkPolicy(new CrawlerProperties());

    @ParameterizedTest
    @ValueSource(strings = {
            "0.0.0.0", "0.1.2.3",
            "10.0.0.1", "10.255.255.255",
            "100.64.0.1", "100.127.255.254",
            "127.0.0.1", "127.8.9.10",
            "169.254.169.254",
            "172.16.0.1", "172.31.255.255",
            "192.0.0.8", "192.0.2.1", "192.88.99.1",
            "192.168.1.1",
            "198.18.0.1", "198.19.255.255", "198.51.100.7",
            "203.0.113.9",
            "224.0.0.1", "239.255.255.250", "240.0.0.1", "255.255.255.255",
            "::", "::1",
            "fe80::1", "fec0::1", "fc00::1", "fd12:3456::1", "ff02::1",
            "2001:db8::1",
            "2001:0:4136:e378:8000:63bf:3fff:fdd2",
            "::ffff:127.0.0.1", "::ffff:10.0.0.1", "::ffff:169.254.169.254",
            "::127.0.0.1",
            "64:ff9b::a9fe:a9fe",
            "2002:7f00:1::1", "2002:c0a8:101::1"
    })
    void blocksNonPublicAddresses(String address) throws UnknownHostException {
        assertThat(policy.isBlocked(InetAddress.getByName(address)))
                .as(address)
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "93.184.216.34", "8.8.8.8", "1.1.1.1",
            "100.63.255.255", "100.128.0.1",
            "172.15.255.255", "172.32.0.1",
            "192.169.0.1", "198.17.255.255", "198.20.0.1",
            "223.255.255.254",
            "2606:2800:220:1:248:1893:25c8:1946",
            "2001:4860:4860::8888",
            "::ffff:8.8.8.8",
            "64:ff9b::808:808",
            "2002:808:808::1"
    })
    void allowsPublicAddresses(String address) throws UnknownHostException {
        assertThat(policy.isBlocked(InetAddress.getByName(address)))
                .as(address)
                .isFalse();
    }

    @Test
    void allowPrivateNetworksDisablesChecks() throws UnknownHostException {

        CrawlerProperties properties = new CrawlerProperties();
        properties.setAllowPrivateNetworks(true);

        assertThat(new NetworkPolicy(properties).isBlocked(InetAddress.getByName("127.0.0.1")))
                .isFalse();
    }
}
