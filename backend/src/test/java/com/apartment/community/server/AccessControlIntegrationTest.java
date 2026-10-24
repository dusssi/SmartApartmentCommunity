package com.apartment.community.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlIntegrationTest extends OperationsTestSupport {
    @Test
    void communityIsolationRoleVisibilityAndPaginationAreServerEnforced() throws Exception {
        Fixture fixture = fixture();
        TestUser outsider = register("Outsider", "outsider@example.test", "Outside Community");
        String base = community(fixture.admin());
        String complaintId = null;
        for (int index = 0; index < 3; index++) {
            JsonNode complaint = createComplaint(fixture, "pagination-request-" + index,
                    "Fictional issue " + index, null);
            if (index == 0) complaintId = complaint.get("id").asText();
        }
        JsonNode firstPage = get(base + "/complaints?page=0&size=2", fixture.admin().accessToken(), 200);
        assertEquals(3, firstPage.get("totalElements").asInt());
        assertEquals(2, firstPage.get("items").size());
        assertTrue(firstPage.get("hasNext").asBoolean());
        JsonNode secondPage = get(base + "/complaints?page=1&size=2", fixture.admin().accessToken(), 200);
        assertEquals(1, secondPage.get("items").size());
        assertEquals(3, get(base + "/complaints", fixture.resident().accessToken(), 200)
                .get("totalElements").asInt());
        assertEquals(0, get(base + "/complaints", fixture.other().accessToken(), 200)
                .get("totalElements").asInt());

        details(fixture, fixture.other(), complaintId, 403);
        get(base + "/complaints", outsider.accessToken(), 404);
        get(community(fixture.resident()) + "/complaints/" + complaintId,
                fixture.resident().accessToken(), 404);
        get(base + "/complaints?page=0&size=101", fixture.admin().accessToken(), 400);
        post(base + "/complaints", fixture.resident().accessToken(),
                complaintBody("Missing key", null), 400);

        JsonNode changed = patch(base + "/memberships/" + fixture.otherMembership().get("membershipId").asText(),
                fixture.admin().accessToken(), Map.of("role", "SECURITY", "active", true,
                        "unitLabel", "A-102", "expectedVersion",
                        fixture.otherMembership().get("version").asLong()), 200);
        assertEquals("SECURITY", changed.get("role").asText());
        assertEquals(0, get(base + "/complaints", fixture.other().accessToken(), 200)
                .get("totalElements").asInt());
        details(fixture, fixture.other(), complaintId, 403);
    }

    @Test
    void accessReviewRevokesImmediatelyAndProtectsLastAdministrator() throws Exception {
        Fixture fixture = fixture();
        String base = community(fixture.admin());
        get(base + "/memberships", fixture.resident().accessToken(), 403);
        JsonNode memberships = get(base + "/memberships?size=100", fixture.admin().accessToken(), 200);
        assertEquals(4, memberships.get("totalElements").asInt());

        JsonNode revoked = patch(base + "/memberships/" + fixture.maintenanceMembership().get("membershipId").asText(),
                fixture.admin().accessToken(), Map.of("role", "MAINTENANCE", "active", false,
                        "expectedVersion", fixture.maintenanceMembership().get("version").asLong()), 200);
        assertTrue(!revoked.get("active").asBoolean());
        get(base + "/complaints", fixture.maintenance().accessToken(), 404);
        addMembership(fixture.admin(), fixture.maintenance(), "MAINTENANCE", null);
        get(base + "/complaints", fixture.maintenance().accessToken(), 200);

        JsonNode adminMembership = null;
        for (JsonNode membership : memberships.get("items")) {
            if (fixture.admin().email().equals(membership.get("email").asText())) adminMembership = membership;
        }
        assertTrue(adminMembership != null);
        JsonNode lastAdmin = patch(base + "/memberships/" + adminMembership.get("membershipId").asText(),
                fixture.admin().accessToken(), Map.of("role", "RESIDENT", "active", true,
                        "unitLabel", "OWN-1", "expectedVersion", adminMembership.get("version").asLong()), 409);
        assertEquals("LAST_ADMIN", lastAdmin.get("code").asText());
    }

    @Test
    void validationDoesNotLeakPersistenceOrRequestSecrets() throws Exception {
        Fixture fixture = fixture();
        String base = community(fixture.admin());
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("title", "");
        invalid.put("description", "x");
        invalid.put("category", "Plumbing");
        invalid.put("priority", "NORMAL");
        invalid.put("apartmentUnit", "A-101");
        invalid.put("location", "Kitchen");
        JsonNode response = post(base + "/complaints", fixture.resident().accessToken(), invalid,
                "validation-request-0001", 400);
        assertEquals("VALIDATION_ERROR", response.get("code").asText());
        assertTrue(response.has("fieldErrors"));
        assertTrue(!response.toString().contains("test-password-12345"));
        assertEquals(0, get(base + "/complaints", fixture.resident().accessToken(), 200)
                .get("totalElements").asInt());
    }
}
