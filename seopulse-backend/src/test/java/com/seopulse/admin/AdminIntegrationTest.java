package com.seopulse.admin;

import com.seopulse.support.AbstractIntegrationTest;
import com.seopulse.user.entity.Role;
import com.seopulse.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AdminProperties adminProperties;

    @AfterEach
    void clearAdminList() {
        adminProperties.setEmails(List.of());
    }

    @Test
    void regularUsersAreRefused() throws Exception {
        TestUser user = registerVerifiedUser("not-admin");

        mvc.perform(authed(get("/api/v1/admin/stats"), user.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listedEmailsBecomeAdminsAndSeePlatformData() throws Exception {
        String email = uniqueEmail("operator");
        adminProperties.setEmails(List.of(email.toUpperCase()));

        String token = JSON.readTree(mvc.perform(post("/api/v1/auth/register")
                                .with(fromIp(randomIp()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "Operator", "email": "%s", "password": "%s"}
                                        """.formatted(email, TEST_PASSWORD)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.role").value("ADMIN"))
                        .andReturn().getResponse().getContentAsString())
                .get("accessToken").asString();

        mvc.perform(authed(get("/api/v1/admin/stats"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.workspacesByPlan.FREE", greaterThanOrEqualTo(1)));

        mvc.perform(authed(get("/api/v1/admin/users").param("q", email.substring(0, 20)), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(email))
                .andExpect(jsonPath("$.content[0].workspaces").value(1));

        mvc.perform(authed(get("/api/v1/admin/organizations").param("size", "5"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].plan").exists());

        mvc.perform(authed(get("/api/v1/admin/audits/failed"), token))
                .andExpect(status().isOk());
    }

    @Test
    void adminsCanUnlockAccountsAndDemotionTakesEffectImmediately() throws Exception {
        TestUser admin = registerVerifiedUser("unlock-admin");
        User adminEntity = userRepository.findById(admin.id()).orElseThrow();
        adminEntity.setRole(Role.ADMIN);
        userRepository.save(adminEntity);
        String adminToken = loginToken(admin.email());

        TestUser locked = registerVerifiedUser("locked");
        User lockedEntity = userRepository.findById(locked.id()).orElseThrow();
        lockedEntity.setLockedUntil(Instant.now().plusSeconds(900));
        lockedEntity.setFailedLoginCount(4);
        userRepository.save(lockedEntity);

        mvc.perform(authed(post("/api/v1/admin/users/{id}/unlock", locked.id()), adminToken))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(locked.id()).orElseThrow().getLockedUntil()).isNull();

        adminEntity = userRepository.findById(admin.id()).orElseThrow();
        adminEntity.setRole(Role.USER);
        userRepository.save(adminEntity);
        mvc.perform(authed(get("/api/v1/admin/stats"), adminToken))
                .andExpect(status().isForbidden());
    }

    private String loginToken(String email) throws Exception {
        return JSON.readTree(mvc.perform(post("/api/v1/auth/login")
                                .with(fromIp(randomIp()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"" + email + "\", \"password\": \"" + TEST_PASSWORD + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("accessToken").asString();
    }
}
