package com.apartment.community.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthIntegrationTest extends AbstractApiIntegrationTest {
    @Autowired DataSource dataSource;

    @Test
    void registrationLoginRefreshRotationAndLogoutAreRealSessionTransitions() throws Exception {
        JsonNode registered = post("/api/v1/auth/register", null, registration(
                "Admin One", "admin@example.test", "Example Homes"), 201);
        assertEquals("COMMUNITY_ADMIN", registered.at("/user/memberships/0/role").asText());
        assertTrue(get("/api/v1/auth/me", registered.get("accessToken").asText(), 200)
                .get("email").asText().equals("admin@example.test"));

        JsonNode login = post("/api/v1/auth/login", null, Map.of(
                "email", "ADMIN@example.test", "password", "test-password-12345"), 200);
        JsonNode refreshed = post("/api/v1/auth/refresh", null,
                Map.of("refreshToken", login.get("refreshToken").asText()), 200);
        assertNotEquals(login.get("refreshToken").asText(), refreshed.get("refreshToken").asText());
        post("/api/v1/auth/refresh", null,
                Map.of("refreshToken", login.get("refreshToken").asText()), 401);
        get("/api/v1/auth/me", login.get("accessToken").asText(), 401);
        get("/api/v1/auth/me", refreshed.get("accessToken").asText(), 200);

        post("/api/v1/auth/logout", null,
                Map.of("refreshToken", refreshed.get("refreshToken").asText()), 204);
        get("/api/v1/auth/me", refreshed.get("accessToken").asText(), 401);
        post("/api/v1/auth/logout", null, Map.of("refreshToken", "unknown-token"), 204);
    }

    @Test
    void validationAuthenticationAndMalformedRequestsUseStructuredErrors() throws Exception {
        Map<String, Object> invalid = registration("Bad User", "bad@example.test", "Bad Community");
        invalid.put("password", "short");
        JsonNode validation = post("/api/v1/auth/register", null, invalid, 400);
        assertEquals("VALIDATION_ERROR", validation.get("code").asText());
        assertTrue(validation.get("fieldErrors").has("password"));
        assertTrue(validation.get("requestId").asText().length() >= 8);

        JsonNode unauthenticated = get("/api/v1/auth/me", null, 401);
        assertEquals("UNAUTHORIZED", unauthenticated.get("code").asText());

        post("/api/v1/auth/register", null, registration(
                "Wrong Password", "wrong@example.test", "Example Homes"), 201);
        JsonNode wrong = post("/api/v1/auth/login", null, Map.of(
                "email", "wrong@example.test", "password", "incorrect-password"), 401);
        assertEquals("UNAUTHORIZED", wrong.get("code").asText());
        assertEquals("Invalid email or password.", wrong.get("message").asText());
    }

    @Test
    void flywayCreatesEveryRequiredTableAndJpaValidatesIt() throws Exception {
        assertEquals(1, flyway.info().applied().length);
        Set<String> tables = new HashSet<>();
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet result = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (result.next()) tables.add(result.getString("TABLE_NAME").toLowerCase());
            }
        }
        Set<String> required = Set.of("community", "app_user", "community_membership", "complaint",
                "asset", "work_order", "work_log", "evidence_reference", "resolution_review",
                "activity_event", "audit_event", "auth_session");
        assertTrue(tables.containsAll(required), () -> "Missing tables: " + required + " in " + tables);
    }

    static Map<String, Object> registration(String name, String email, String community) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", "test-password-12345");
        body.put("communityName", community);
        body.put("unitLabel", "OWN-1");
        return body;
    }
}
