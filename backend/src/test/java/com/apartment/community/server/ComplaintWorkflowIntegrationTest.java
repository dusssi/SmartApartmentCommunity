package com.apartment.community.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplaintWorkflowIntegrationTest extends OperationsTestSupport {
    @Test
    void completeIssueToOutcomeWorkflowPersistsAndFiltersEvidence() throws Exception {
        Fixture fixture = fixture();
        String base = community(fixture.admin());
        JsonNode asset = post(base + "/assets", fixture.admin().accessToken(), Map.of(
                "name", "Riser Pump", "type", "Pump", "location", "Basement",
                "criticality", "HIGH"), 201);
        Map<String, Object> complaintBody = complaintBody("Leak below kitchen sink", asset.get("id").asText());
        JsonNode complaint = post(base + "/complaints", fixture.resident().accessToken(), complaintBody,
                "complaint-request-0001", 201);
        JsonNode replay = post(base + "/complaints", fixture.resident().accessToken(), complaintBody,
                "complaint-request-0001", 200);
        assertEquals(complaint.get("id").asText(), replay.get("id").asText());
        Map<String, Object> changedRequest = new HashMap<>(complaintBody);
        changedRequest.put("title", "A different complaint must not reuse the key");
        JsonNode reused = post(base + "/complaints", fixture.resident().accessToken(), changedRequest,
                "complaint-request-0001", 409);
        assertEquals("IDEMPOTENCY_KEY_REUSED", reused.get("code").asText());
        assertEquals(1, get(base + "/complaints", fixture.resident().accessToken(), 200)
                .get("totalElements").asInt());
        String complaintId = complaint.get("id").asText();

        details(fixture, fixture.other(), complaintId, 403);
        details(fixture, fixture.maintenance(), complaintId, 403);
        post(base + "/complaints/" + complaintId + "/assignment", fixture.admin().accessToken(),
                Map.of("assigneeId", fixture.other().id(), "expectedVersion", 0), 400);
        JsonNode work = assign(fixture, complaint);
        String workId = work.get("id").asText();
        assertEquals("ASSIGNED", work.get("status").asText());
        details(fixture, fixture.maintenance(), complaintId, 200);

        post(base + "/work-orders/" + workId + "/status", fixture.resident().accessToken(),
                Map.of("status", "ACCEPTED", "expectedVersion", 0), 403);
        work = post(base + "/work-orders/" + workId + "/status", fixture.maintenance().accessToken(),
                Map.of("status", "ACCEPTED", "expectedVersion", 0), 200);
        assertEquals(1, work.get("version").asLong());
        JsonNode inspection = addWorkLog(fixture, workId, 1, "INSPECTION",
                "Isolated the fictional leak and inspected the supply line.", 201);
        assertEquals(2, inspection.get("workOrderVersion").asLong());
        JsonNode performed = addWorkLog(fixture, workId, 2, "WORK_PERFORMED",
                "Replaced the fictional worn connector and completed a leak check.", 201);
        assertEquals(3, performed.get("workOrderVersion").asLong());

        JsonNode staffEvidence = post(base + "/work-logs/" + performed.get("id").asText() + "/evidence",
                fixture.maintenance().accessToken(), evidenceBody("Staff access note", "STAFF_ONLY", 3), 201);
        assertEquals(4, staffEvidence.get("workOrderVersion").asLong());
        assertEquals("METADATA_ONLY", staffEvidence.get("storageState").asText());
        JsonNode participantEvidence = post(base + "/work-logs/" + performed.get("id").asText() + "/evidence",
                fixture.maintenance().accessToken(), evidenceBody("Repair photo metadata", "ISSUE_PARTICIPANTS", 4), 201);
        assertEquals(5, participantEvidence.get("workOrderVersion").asLong());

        JsonNode review = post(base + "/work-orders/" + workId + "/resolution-proposal",
                fixture.maintenance().accessToken(), Map.of(
                        "summary", "Connector replaced and leak check passed.", "expectedVersion", 5), 201);
        assertEquals("AWAITING_RESIDENT", review.get("status").asText());
        String reviewId = review.get("id").asText();
        post(base + "/work-orders/" + workId + "/resolution-proposal", fixture.maintenance().accessToken(),
                Map.of("summary", "Duplicate proposal", "expectedVersion", 5), 409);

        JsonNode residentDetails = details(fixture, fixture.resident(), complaintId, 200);
        assertEquals(1, residentDetails.get("evidence").size());
        assertEquals("ISSUE_PARTICIPANTS", residentDetails.at("/evidence/0/accessScope").asText());
        JsonNode maintenanceDetails = details(fixture, fixture.maintenance(), complaintId, 200);
        assertEquals(2, maintenanceDetails.get("evidence").size());
        assertEquals("AWAITING_CONFIRMATION", residentDetails.at("/complaint/outcomeStatus").asText());

        JsonNode residentActivity = get(base + "/activity?complaintId=" + complaintId + "&size=100",
                fixture.resident().accessToken(), 200);
        assertEquals(1, evidenceEvents(residentActivity));
        JsonNode managementActivity = get(base + "/activity?complaintId=" + complaintId + "&size=100",
                fixture.admin().accessToken(), 200);
        assertEquals(2, evidenceEvents(managementActivity));

        post(base + "/resolution-reviews/" + reviewId + "/review", fixture.other().accessToken(),
                Map.of("confirmed", true, "expectedVersion", 0), 403);
        JsonNode confirmed = post(base + "/resolution-reviews/" + reviewId + "/review",
                fixture.resident().accessToken(), Map.of(
                        "confirmed", true, "note", "The fictional repair is verified.", "expectedVersion", 0), 200);
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        post(base + "/resolution-reviews/" + reviewId + "/review", fixture.resident().accessToken(),
                Map.of("confirmed", true, "expectedVersion", 0), 409);

        JsonNode verified = details(fixture, fixture.resident(), complaintId, 200);
        assertEquals("VERIFIED", verified.at("/complaint/outcomeStatus").asText());
        assertEquals("COMPLETED", verified.at("/workOrders/0/status").asText());
        JsonNode reopened = post(base + "/complaints/" + complaintId + "/reopen",
                fixture.resident().accessToken(), Map.of("reason", "The fictional leak returned.",
                        "expectedVersion", verified.at("/complaint/version").asLong()), 200);
        assertEquals("REOPENED", reopened.get("outcomeStatus").asText());
        JsonNode reopenedDetails = details(fixture, fixture.resident(), complaintId, 200);
        assertEquals("REOPENED", reopenedDetails.at("/workOrders/0/status").asText());

        JsonNode audit = get(base + "/audit?size=100", fixture.admin().accessToken(), 200);
        assertTrue(audit.get("totalElements").asLong() >= 10);
        get(base + "/audit", fixture.resident().accessToken(), 403);
    }

    @Test
    void staleRetriesCannotDuplicateTransitionsAndDisputeReturnsToWork() throws Exception {
        Fixture fixture = fixture();
        String base = community(fixture.admin());
        JsonNode complaint = createComplaint(fixture, "complaint-request-0002", "Disputed repair", null);
        String complaintId = complaint.get("id").asText();
        JsonNode work = assign(fixture, complaint);
        String workId = work.get("id").asText();
        Map<String, Object> accept = Map.of("status", "ACCEPTED", "expectedVersion", 0);
        post(base + "/work-orders/" + workId + "/status", fixture.maintenance().accessToken(), accept, 200);
        JsonNode staleStatus = post(base + "/work-orders/" + workId + "/status",
                fixture.maintenance().accessToken(), accept, 409);
        assertEquals("STALE_VERSION", staleStatus.get("code").asText());

        JsonNode performed = addWorkLog(fixture, workId, 1, "WORK_PERFORMED",
                "Completed the first fictional repair attempt.", 201);
        addWorkLog(fixture, workId, 1, "WORK_PERFORMED",
                "Completed the first fictional repair attempt.", 409);
        JsonNode review = post(base + "/work-orders/" + workId + "/resolution-proposal",
                fixture.maintenance().accessToken(), Map.of(
                        "summary", "First repair attempt completed.", "expectedVersion",
                        performed.get("workOrderVersion").asLong()), 201);
        String reviewId = review.get("id").asText();
        post(base + "/resolution-reviews/" + reviewId + "/review", fixture.resident().accessToken(),
                Map.of("confirmed", false, "note", "  ", "expectedVersion", 0), 400);
        JsonNode disputed = post(base + "/resolution-reviews/" + reviewId + "/review",
                fixture.resident().accessToken(), Map.of("confirmed", false,
                        "note", "The issue is still present.", "expectedVersion", 0), 200);
        assertEquals("DISPUTED", disputed.get("status").asText());
        JsonNode details = details(fixture, fixture.resident(), complaintId, 200);
        assertEquals("DISPUTED", details.at("/complaint/outcomeStatus").asText());
        assertEquals("IN_PROGRESS", details.at("/complaint/status").asText());
        assertEquals("IN_PROGRESS", details.at("/workOrders/0/status").asText());
    }

    private static Map<String, Object> evidenceBody(String name, String scope, long expectedVersion) {
        Map<String, Object> body = new HashMap<>();
        body.put("displayName", name);
        body.put("referenceValue", "content://fictional/" + name.replace(' ', '-'));
        body.put("mediaType", "image/jpeg");
        body.put("sizeBytes", 1024);
        body.put("accessScope", scope);
        body.put("pageNumber", 0);
        body.put("sectionName", "After repair");
        body.put("expectedVersion", expectedVersion);
        return body;
    }

    private static int evidenceEvents(JsonNode activity) {
        int count = 0;
        for (JsonNode event : activity.get("items")) {
            if ("EVIDENCE_ADDED".equals(event.get("type").asText())) count++;
        }
        return count;
    }
}
