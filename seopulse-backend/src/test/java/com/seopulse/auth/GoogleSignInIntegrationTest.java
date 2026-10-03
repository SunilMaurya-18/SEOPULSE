package com.seopulse.auth;

import com.seopulse.auth.service.GoogleIdTokenVerifier;
import com.seopulse.auth.service.GoogleIdTokenVerifier.GoogleIdentity;
import com.seopulse.common.exception.InvalidCredentialsException;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.support.AbstractIntegrationTest;
import com.seopulse.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleSignInIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    GoogleIdTokenVerifier googleVerifier;

    @Autowired
    OrganizationMemberRepository memberRepository;

    @Test
    void firstSignInCreatesAVerifiedAccountAndLaterSignInsReuseIt() throws Exception {
        String email = uniqueEmail("google-new");
        String subject = UUID.randomUUID().toString();
        when(googleVerifier.verify(eq("token-new"))).thenReturn(new GoogleIdentity(subject, email, true, "Grace Hopper"));

        long userId = JSON.readTree(signIn("token-new")
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.emailVerified").value(true))
                        .andExpect(jsonPath("$.name").value("Grace Hopper"))
                        .andReturn().getResponse().getContentAsString())
                .get("userId").asLong();

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getGoogleSubject()).isEqualTo(subject);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(memberRepository.findByUserIdOrderByIdAsc(userId)).isNotEmpty();

        signIn("token-new").andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void linksToAnExistingPasswordAccountWithTheSameEmail() throws Exception {
        TestUser existing = registerVerifiedUser("google-link");
        when(googleVerifier.verify(eq("token-link")))
                .thenReturn(new GoogleIdentity(UUID.randomUUID().toString(), existing.email(), true, "Someone"));

        signIn("token-link").andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(existing.id()));

        assertThat(userRepository.findById(existing.id()).orElseThrow().getGoogleSubject()).isNotNull();
        login(existing.email(), existing.password()).andExpect(status().isOk());
    }

    @Test
    void takingOverAnUnverifiedSignUpDropsItsPassword() throws Exception {
        TestUser squatter = registerUser("google-squat");
        when(googleVerifier.verify(eq("token-owner")))
                .thenReturn(new GoogleIdentity(UUID.randomUUID().toString(), squatter.email(), true, "Owner"));

        signIn("token-owner").andExpect(status().isOk());

        assertThat(userRepository.findById(squatter.id()).orElseThrow().isEmailVerified()).isTrue();
        login(squatter.email(), squatter.password()).andExpect(status().isUnauthorized());
    }

    @Test
    void refusesUnverifiedGoogleEmailsAndBadTokens() throws Exception {
        doThrow(new InvalidCredentialsException("Google sign-in failed. Please try again."))
                .when(googleVerifier).verify(anyString());
        doReturn(new GoogleIdentity(UUID.randomUUID().toString(), uniqueEmail("unverified"), false, null))
                .when(googleVerifier).verify("token-unverified");

        signIn("token-unverified")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Verify your email with Google")));
        signIn("forged").andExpect(status().isUnauthorized());
    }

    private ResultActions signIn(String credential) throws Exception {
        return mvc.perform(post("/api/v1/auth/google")
                .with(fromIp(randomIp()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\": \"" + credential + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(fromIp(randomIp()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }
}
