package com.seopulse.billing;

public class PlanLimitExceededException extends RuntimeException {

    private final String meter;
    private final int limit;
    private final int used;
    private final String upgradeTo;

    public PlanLimitExceededException(String meter, int limit, int used, String upgradeTo) {
        super("Plan limit reached for " + meter);
        this.meter = meter;
        this.limit = limit;
        this.used = used;
        this.upgradeTo = upgradeTo;
    }

    public String getMeter() {
        return meter;
    }

    public int getLimit() {
        return limit;
    }

    public int getUsed() {
        return used;
    }

    public String getUpgradeTo() {
        return upgradeTo;
    }
}
