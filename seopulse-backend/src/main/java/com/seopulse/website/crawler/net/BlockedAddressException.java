package com.seopulse.website.crawler.net;

import java.net.UnknownHostException;

public class BlockedAddressException extends UnknownHostException {

    public BlockedAddressException(String host) {
        super("Blocked: " + host + " resolves to a restricted network address");
    }
}
