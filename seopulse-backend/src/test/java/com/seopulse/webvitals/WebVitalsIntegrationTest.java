package com.seopulse.webvitals;

import com.seopulse.support.AbstractWorkerIntegrationTest;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.entity.Website;
import com.seopulse.website.events.AuditFinishedEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebVitalsIntegrationTest extends AbstractWorkerIntegrationTest {

    @Autowired
    WebVitalsProperties properties;

    @Autowired
    WebVitalsService webVitalsService;

    @Autowired
    AuditWebVitalsRepository webVitalsRepository;

    @Autowired
    ApplicationEventPublisher events;

    private HttpServer pageSpeed;

    @BeforeEach
    void fakePageSpeed() throws IOException {
        pageSpeed = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        pageSpeed.createContext("/psi", exchange -> {
            byte[] body = PageSpeedSamples.WITH_FIELD_DATA.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        pageSpeed.start();
        properties.setApiUrl("http://127.0.0.1:" + pageSpeed.getAddress().getPort() + "/psi");
        properties.setEnabled(true);
    }

    @AfterEach
    void restore() {
        properties.setEnabled(false);
        properties.setApiUrl(new WebVitalsProperties().getApiUrl());
        pageSpeed.stop(0);
    }

    @Test
    void measuresTheHomepageOnceTheAuditCompletes() throws Exception {
        Website website = createWebsiteFor("https://vitals.example.com/");
        Audit audit = completedAudit(website);
        Long projectId = website.getProject().getId();
        Long ownerId = website.getProject().getUser().getId();

        assertThat(webVitalsService.get(projectId, audit.getId(), ownerId).state())
                .isEqualTo(WebVitalsResponse.State.UNAVAILABLE);

        events.publishEvent(new AuditFinishedEvent(audit.getId(), AuditStatus.COMPLETED));

        long deadline = System.nanoTime() + 10_000_000_000L;
        while (webVitalsRepository.findById(audit.getId()).map(r -> r.getStatus() != AuditWebVitals.Status.READY).orElse(true)) {
            assertThat(System.nanoTime()).as("web vitals measured in time").isLessThan(deadline);
            Thread.sleep(100);
        }

        WebVitalsResponse response = webVitalsService.get(projectId, audit.getId(), ownerId);
        assertThat(response.state()).isEqualTo(WebVitalsResponse.State.READY);
        assertThat(response.url()).isEqualTo("https://vitals.example.com/");
        assertThat(response.performanceScore()).isEqualTo(73);
        assertThat(response.lab().lcpMs()).isEqualTo(3120);
        assertThat(response.field().inpMs()).isEqualTo(180);
    }

    @Test
    void otherUsersCannotReadIt() throws Exception {
        Website website = createWebsiteFor("https://private-vitals.example.com/");
        Audit audit = completedAudit(website);
        TestUser stranger = registerVerifiedUser("vitals-stranger");

        mvc.perform(authed(get("/api/v1/projects/{p}/audits/{a}/web-vitals",
                        website.getProject().getId(), audit.getId()), stranger.accessToken()))
                .andExpect(status().isNotFound());
    }

    private Audit completedAudit(Website website) {
        Audit audit = queuedAudit(website);
        audit.setStatus(AuditStatus.COMPLETED);
        audit.setCompletedAt(Instant.now());
        audit.setScore(80);
        return auditRepository.save(audit);
    }
}
