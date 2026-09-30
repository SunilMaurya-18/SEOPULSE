package com.seopulse.support;

import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Boots the full application against real Postgres and Redis containers.
 * Skipped when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestEmailConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    public static final String TEST_JWT_SECRET =
            "test-only-jwt-secret-that-is-long-enough-for-hs256";

    public static final String TEST_PASSWORD = "correct-horse-battery";

    protected static final ObjectMapper JSON = new ObjectMapper();

    // Shared for the whole JVM so the cached Spring context keeps valid ports.
    // Ryuk removes the containers when the JVM exits.
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:8-alpine")
                    .withExposedPorts(6379);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CapturingEmailSender emails;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        startContainers();

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    private static synchronized void startContainers() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    public record TestUser(Long id, String email, String password, String accessToken, Cookie refreshCookie) {
    }

    /**
     * Rate limits are keyed by client IP, so each simulated client gets
     * its own address to keep tests independent.
     */
    protected static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    protected static String randomIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(1, 255);
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    protected static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    protected TestUser registerUser(String prefix) throws Exception {

        String email = uniqueEmail(prefix);

        MockHttpServletResponse response = mvc.perform(
                        post("/api/v1/auth/register")
                                .with(fromIp(randomIp()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "%s", "email": "%s", "password": "%s"}
                                        """.formatted(prefix, email, TEST_PASSWORD))
                )
                .andReturn()
                .getResponse();

        if (response.getStatus() != 201) {
            throw new AssertionError("Registration failed: " + response.getStatus() + " " + response.getContentAsString());
        }

        JsonNode body = JSON.readTree(response.getContentAsString());

        return new TestUser(
                body.get("userId").asLong(),
                email,
                TEST_PASSWORD,
                body.get("accessToken").asString(),
                response.getCookie("seopulse_refresh")
        );
    }

    protected TestUser registerVerifiedUser(String prefix) throws Exception {
        TestUser user = registerUser(prefix);
        User entity = userRepository.findById(user.id()).orElseThrow();
        entity.setEmailVerifiedAt(Instant.now());
        userRepository.save(entity);
        return user;
    }
}
