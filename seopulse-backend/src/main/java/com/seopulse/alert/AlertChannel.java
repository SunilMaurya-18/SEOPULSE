package com.seopulse.alert;

public enum AlertChannel {
    EMAIL,
    SLACK_WEBHOOK,
    WEBHOOK;

    public boolean isWebhook() {
        return this != EMAIL;
    }
}
