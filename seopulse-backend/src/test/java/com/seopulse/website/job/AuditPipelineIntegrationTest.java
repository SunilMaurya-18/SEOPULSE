package com.seopulse.website.job;

import com.seopulse.support.AbstractWorkerIntegrationTest;
import com.seopulse.support.TestSite;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole audit flow as a user drives it: API, outbox, Redis stream,
 * worker, crawler, analyzers, and the SSE status stream.
 */
class AuditPipelineIntegrationTest extends AbstractWorkerIntegrationTest {

    @Test
    void auditRunsEndToEnd() throws Exception {

        String root = SITE.createSite(Map.of(
                "", TestSite.page(
                        "SEOPulse test home page with a good title",
                        "A home page with a meta description that is long enough to pass the length check.",
                        "<h1>Home</h1><a href=\"about\">About</a><a href=\"thin\">Thin</a><img src=\"a.png\">"
                ),
                "about", TestSite.page(
                        "About the SEOPulse test site and its people",
                        null,
                        "<h1>About</h1><h1>Second heading</h1><a href=\"./\">Home</a>"
                ),
                "thin", TestSite.page("", null, "<p>Too short</p>")
        ));

        TestUser user = registerVerifiedUser("pipeline");
        long projectId = createProject(user.accessToken());
        long websiteId = createWebsite(user.accessToken(), projectId, root);

        MvcResult created = mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits")
                        .param("websiteId", String.valueOf(websiteId)), user.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        long auditId = JSON.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        Audit audit = awaitAudit(auditId, a -> !a.getStatus().isActive(), Duration.ofSeconds(60));

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED);
        assertThat(audit.getPagesCrawled()).isEqualTo(3);
        assertThat(audit.getPagesAnalyzed()).isEqualTo(3);
        assertThat(audit.getScore()).isBetween(1, 99);

        List<String> ruleCodes = issueRuleCodes(user.accessToken(), projectId, auditId);
        assertThat(ruleCodes).contains(
                "META_DESCRIPTION_MISSING",
                "MULTIPLE_H1",
                "TITLE_MISSING",
                "H1_MISSING",
                "IMAGE_ALT_MISSING",
                "LOW_WORD_COUNT"
        );

        // A finished audit's stream sends the final state and closes.
        MvcResult stream = mvc.perform(authed(get("/api/v1/projects/" + projectId + "/audits/" + auditId + "/events"),
                        user.accessToken()))
                .andExpect(request().asyncStarted())
                .andReturn();

        String events = mvc.perform(asyncDispatch(stream))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(events).contains("event:audit").contains("\"status\":\"COMPLETED\"");
    }

    @Test
    void userCanCancelRunningAudit() throws Exception {

        Map<String, String> pages = new LinkedHashMap<>();
        StringBuilder links = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            links.append("<a href=\"p").append(i).append("\">p").append(i).append("</a>");
            pages.put("p" + i, TestSite.page("Slow page " + i, null, "<h1>Slow</h1>"));
        }
        pages.put("", TestSite.page("Slow home", null, links.toString()));

        String root = SITE.createSite(pages, Duration.ofMillis(400));

        TestUser user = registerVerifiedUser("cancel");
        long projectId = createProject(user.accessToken());
        long websiteId = createWebsite(user.accessToken(), projectId, root);

        long auditId = JSON.readTree(mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits")
                                .param("websiteId", String.valueOf(websiteId)), user.accessToken()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        awaitAudit(auditId, a -> a.getStatus() == AuditStatus.CRAWLING, Duration.ofSeconds(30));

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits/" + auditId + "/cancel"),
                        user.accessToken()))
                .andExpect(status().isOk());

        // Give the worker time to notice; its late updates must not overwrite CANCELLED.
        Thread.sleep(1500);

        Audit audit = auditRepository.findById(auditId).orElseThrow();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.CANCELLED);
        assertThat(audit.getCompletedAt()).isNotNull();

        mvc.perform(authed(post("/api/v1/projects/" + projectId + "/audits/" + auditId + "/cancel"),
                        user.accessToken()))
                .andExpect(status().isConflict());
    }

    private List<String> issueRuleCodes(String token, long projectId, long auditId) throws Exception {
        String body = mvc.perform(authed(get("/api/v1/projects/" + projectId + "/audits/" + auditId + "/issues")
                        .param("size", "100"), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> codes = new ArrayList<>();
        for (JsonNode issue : JSON.readTree(body).get("content")) {
            codes.add(issue.get("ruleCode").asString());
        }
        return codes;
    }

    private long createProject(String token) throws Exception {
        String body = mvc.perform(authed(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Pipeline\"}"), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).get("id").asLong();
    }

    private long createWebsite(String token, long projectId, String url) throws Exception {
        String body = mvc.perform(authed(post("/api/v1/projects/" + projectId + "/websites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test site\", \"url\": \"" + url + "\"}"), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).get("id").asLong();
    }
}
