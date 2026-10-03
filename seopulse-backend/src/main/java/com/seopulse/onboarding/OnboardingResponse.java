package com.seopulse.onboarding;

import java.util.List;

public record OnboardingResponse(boolean dismissed, List<Step> steps) {

    public enum StepId {
        VERIFY_EMAIL,
        ADD_WEBSITE,
        RUN_AUDIT,
        SHARE_REPORT,
        INVITE_TEAM
    }

    public record Step(StepId id, boolean done) {
    }
}
