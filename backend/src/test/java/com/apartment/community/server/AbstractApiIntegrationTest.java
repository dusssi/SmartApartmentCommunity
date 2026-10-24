package com.apartment.community.server;

import com.apartment.community.server.security.RateLimitFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractApiIntegrationTest {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected Flyway flyway;
    @Autowired protected RateLimitFilter rateLimits;

    @DynamicPropertySource
    static void ephemeralJwtKey(DynamicPropertyRegistry registry) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(bytes));
    }

    @BeforeEach
    void resetDatabase() {
        rateLimits.reset();
        flyway.clean();
        flyway.migrate();
    }

    protected JsonNode post(String path, String token, Object body, int expectedStatus) throws Exception {
        return request("POST", path, token, body, null, expectedStatus);
    }

    protected JsonNode post(String path, String token, Object body, String idempotencyKey,
                            int expectedStatus) throws Exception {
        return request("POST", path, token, body, idempotencyKey, expectedStatus);
    }

    protected JsonNode patch(String path, String token, Object body, int expectedStatus) throws Exception {
        return request("PATCH", path, token, body, null, expectedStatus);
    }

    protected JsonNode get(String path, String token, int expectedStatus) throws Exception {
        return request("GET", path, token, null, null, expectedStatus);
    }

    protected JsonNode request(String method, String path, String token, Object body,
                               String idempotencyKey, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
        if (body != null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : json.readTree(content);
    }
}
