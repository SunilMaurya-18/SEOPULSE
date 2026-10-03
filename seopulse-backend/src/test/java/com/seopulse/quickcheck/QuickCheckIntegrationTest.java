package com.seopulse.quickcheck;

import com.seopulse.support.AbstractWorkerIntegrationTest;
import com.seopulse.support.TestSite;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuickCheckIntegrationTest extends AbstractWorkerIntegrationTest {

    @Test
    void checksASiteWithoutSigningUpOrSavingAnything() throws Exception {
        String url = SITE.createSite(Map.of(
                "", TestSite.page("Quick check home page", null, "<h1>Home</h1><a href=\"about\">About</a>"),
                "about", TestSite.page("Quick check about page", null, "<h1>About</h1><a href=\"./\">Home</a>")
        ));
        long auditsBefore = auditRepository.count();

        check(url, randomIp())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(url))
                .andExpect(jsonPath("$.pagesChecked").value(2))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.categoryScores.CONTENT").isNumber())
                .andExpect(jsonPath("$.issues[*].ruleCode").value(hasItem("META_DESCRIPTION_MISSING")))
                .andExpect(jsonPath("$.issues[0].title").isString())
                .andExpect(jsonPath("$.issues[0].recommendation").isString());

        assertThat(auditRepository.count()).isEqualTo(auditsBefore);
    }

    @Test
    void explainsWhyAHomepageCannotBeChecked() throws Exception {
        String url = SITE.createSite(Map.of("about", TestSite.page("Only an inner page", null, "")));

        check(url, randomIp())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("HTTP 404")));
    }

    @Test
    void rejectsUrlsThatAreNotWebsites() throws Exception {
        check("ftp://example.com/", randomIp())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("HTTP and HTTPS")));
    }

    @Test
    void limitsChecksPerVisitor() throws Exception {
        String ip = randomIp();
        for (int i = 0; i < 5; i++) {
            check("ftp://example.com/", ip).andExpect(status().isBadRequest());
        }
        check("ftp://example.com/", ip).andExpect(status().isTooManyRequests());
    }

    @Test
    void limitsChecksPerTargetSite() throws Exception {
        String url = "https://www.quick-check-target.invalid/";
        for (int i = 0; i < 10; i++) {
            check(url, randomIp()).andExpect(status().isBadRequest());
        }
        check("https://quick-check-target.invalid/other", randomIp()).andExpect(status().isTooManyRequests());
    }

    @Test
    void addsHttpsToBareDomains() {
        assertThat(QuickCheckService.normalize(" example.com/page ")).isEqualTo("https://example.com/page");
        assertThat(QuickCheckService.normalize("http://example.com")).isEqualTo("http://example.com");
        assertThat(QuickCheckService.normalize("")).isEmpty();
    }

    private ResultActions check(String url, String ip) throws Exception {
        return mvc.perform(post("/api/v1/public/quick-check")
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("url", url))));
    }
}
