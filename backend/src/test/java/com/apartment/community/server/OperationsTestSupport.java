package com.apartment.community.server;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

abstract class OperationsTestSupport extends AbstractApiIntegrationTest {
    protected Fixture fixture() throws Exception {
        TestUser admin = register("Admin", "admin@example.test", "Admin Community");
        TestUser resident = register("Resident", "resident@example.test", "Resident Community");
        TestUser maintenance = register("Maintainer", "maintainer@example.test", "Maintenance Community");
        TestUser other = register("Other Resident", "other@example.test", "Other Community");
        JsonNode residentMembership = addMembership(admin, resident, "RESIDENT", "A-101");
        JsonNode maintenanceMembership = addMembership(admin, maintenance, "MAINTENANCE", null);
        JsonNode otherMembership = addMembership(admin, other, "RESIDENT", "A-102");
        return new Fixture(admin, resident, maintenance, other,
                residentMembership, maintenanceMembership, otherMembership);
    }

    protected TestUser register(String name, String email, String community) throws Exception {
        JsonNode response = post("/api/v1/auth/register", null,
                AuthIntegrationTest.registration(name, email, community), 201);
        return new TestUser(response.at("/user/id").asText(), email,
                response.get("accessToken").asText(), response.get("refreshToken").asText(),
                response.at("/user/memberships/0/communityId").asText());
    }

    protected JsonNode addMembership(TestUser admin, TestUser user, String role, String unit) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("email", user.email());
        body.put("role", role);
        body.put("unitLabel", unit);
        return post(community(admin) + "/memberships", admin.accessToken(), body, 201);
    }

    protected JsonNode createComplaint(Fixture fixture, String key, String title, String assetId) throws Exception {
        Map<String, Object> body = complaintBody(title, assetId);
        return post(community(fixture.admin()) + "/complaints", fixture.resident().accessToken(),
                body, key, 201);
    }

    protected Map<String, Object> complaintBody(String title, String assetId) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "A fictional integration-test complaint with enough detail to be actionable.");
        body.put("category", "Plumbing");
        body.put("priority", "NORMAL");
        body.put("apartmentUnit", "A-101");
        body.put("location", "Kitchen");
        body.put("assetId", assetId);
        return body;
    }

    protected JsonNode assign(Fixture fixture, JsonNode complaint) throws Exception {
        return post(community(fixture.admin()) + "/complaints/" + complaint.get("id").asText() + "/assignment",
                fixture.admin().accessToken(), Map.of("assigneeId", fixture.maintenance().id(),
                        "expectedVersion", complaint.get("version").asLong()), 201);
    }

    protected JsonNode addWorkLog(Fixture fixture, String workOrderId, long version,
                                  String type, String details, int expectedStatus) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("details", details);
        body.put("nextStep", "Review the outcome");
        body.put("materials", "Fictional test material");
        body.put("expectedVersion", version);
        return post(community(fixture.admin()) + "/work-orders/" + workOrderId + "/logs",
                fixture.maintenance().accessToken(), body, expectedStatus);
    }

    protected JsonNode details(Fixture fixture, TestUser user, String complaintId, int status) throws Exception {
        return get(community(fixture.admin()) + "/complaints/" + complaintId, user.accessToken(), status);
    }

    protected String community(TestUser user) {
        return "/api/v1/communities/" + user.ownCommunityId();
    }

    protected record TestUser(String id, String email, String accessToken, String refreshToken,
                              String ownCommunityId) { }

    protected record Fixture(TestUser admin, TestUser resident, TestUser maintenance, TestUser other,
                             JsonNode residentMembership, JsonNode maintenanceMembership,
                             JsonNode otherMembership) { }
}
