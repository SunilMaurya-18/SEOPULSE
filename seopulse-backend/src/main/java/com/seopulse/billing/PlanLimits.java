package com.seopulse.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record PlanLimits(int websites, int pagesPerAudit, int auditsPerMonth, int members) {

    public static PlanLimits parse(String json, ObjectMapper mapper) {
        try {
            JsonNode node = mapper.readTree(json);
            return new PlanLimits(
                    node.path("websites").asInt(1),
                    node.path("pagesPerAudit").asInt(100),
                    node.path("auditsPerMonth").asInt(5),
                    node.path("members").asInt(1)
            );
        } catch (Exception ex) {
            return new PlanLimits(1, 100, 5, 1);
        }
    }
}
