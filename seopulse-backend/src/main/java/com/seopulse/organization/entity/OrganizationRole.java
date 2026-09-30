package com.seopulse.organization.entity;

public enum OrganizationRole {
    VIEWER(0),
    MEMBER(1),
    ADMIN(2),
    OWNER(3);

    private final int rank;

    OrganizationRole(int rank) {
        this.rank = rank;
    }

    public boolean atLeast(OrganizationRole minimum) {
        return rank >= minimum.rank;
    }
}
