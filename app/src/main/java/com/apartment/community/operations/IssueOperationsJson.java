package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.model.CommunityRole;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.IssueRelation;
import com.apartment.community.operations.model.RecurrenceEvent;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Versioned, explicit JSON mapping. No exception is swallowed and no malformed root is reseeded. */
final class IssueOperationsJson {
    private IssueOperationsJson() { }

    static IssueOperationsState empty() {
        return new IssueOperationsState();
    }

    static IssueOperationsState migrateLegacy(String legacyJson) {
        IssueOperationsState state = empty();
        if (legacyJson == null || legacyJson.trim().isEmpty()) return state;
        JSONArray array = new JSONArray(legacyJson);
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject object = array.getJSONObject(i);
                Complaint issue = readComplaint(object);
                if (state.issue(issue.id) != null) {
                    state.migrationWarnings.add("Duplicate legacy complaint skipped: " + issue.id);
                    continue;
                }
                state.complaints.add(issue);
                state.importedEvent(issue);
            } catch (RuntimeException error) {
                state.migrationWarnings.add("Legacy complaint at index " + i
                        + " was not imported: " + safeMessage(error));
            }
        }
        return state;
    }

    static String write(IssueOperationsState state) {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", IssueOperationsState.SCHEMA_VERSION);
        root.put("nextId", state.nextId);
        root.put("nextActivitySequence", state.nextActivitySequence);
        root.put("migrationWarnings", strings(state.migrationWarnings));
        JSONArray complaints = new JSONArray();
        for (Complaint value : state.complaints) complaints.put(writeComplaint(value));
        root.put("complaints", complaints);
        JSONArray assets = new JSONArray();
        for (Asset value : state.assets) {
            assets.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("name", value.name).put("type", value.type)
                    .put("location", value.location).put("criticality", value.criticality.name())
                    .put("status", value.status.name()).put("installedAtEpochMs", value.installedAtEpochMs)
                    .put("createdAtEpochMs", value.createdAtEpochMs));
        }
        root.put("assets", assets);
        JSONArray relations = new JSONArray();
        for (IssueRelation value : state.relations) {
            relations.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("sourceIssueId", value.sourceIssueId).put("targetIssueId", value.targetIssueId)
                    .put("type", value.type.name()).put("reviewStatus", value.reviewStatus.name())
                    .put("rationale", value.rationale).put("createdBy", value.createdBy)
                    .put("createdAtEpochMs", value.createdAtEpochMs).put("reviewedBy", value.reviewedBy)
                    .put("reviewedAtEpochMs", value.reviewedAtEpochMs));
        }
        root.put("issueRelations", relations);
        JSONArray workOrders = new JSONArray();
        for (WorkOrder value : state.workOrders) {
            workOrders.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("issueId", value.issueId).put("assetId", value.assetId)
                    .put("assigneeId", value.assigneeId).put("assignedBy", value.assignedBy)
                    .put("status", value.status.name()).put("createdAtEpochMs", value.createdAtEpochMs)
                    .put("updatedAtEpochMs", value.updatedAtEpochMs).put("version", value.version));
        }
        root.put("workOrders", workOrders);
        JSONArray workLogs = new JSONArray();
        for (WorkLog value : state.workLogs) {
            workLogs.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("issueId", value.issueId).put("workOrderId", value.workOrderId)
                    .put("authorId", value.authorId).put("type", value.type.name())
                    .put("details", value.details).put("nextStep", value.nextStep)
                    .put("materials", value.materials).put("createdAtEpochMs", value.createdAtEpochMs));
        }
        root.put("workLogs", workLogs);
        JSONArray evidence = new JSONArray();
        for (EvidenceReference value : state.evidence) {
            evidence.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("issueId", value.issueId).put("workLogId", value.workLogId)
                    .put("uploadedBy", value.uploadedBy).put("displayName", value.displayName)
                    .put("reference", value.reference).put("mediaType", value.mediaType)
                    .put("sizeBytes", value.sizeBytes).put("accessScope", value.accessScope.name())
                    .put("storageState", value.storageState.name()).put("pageNumber", value.pageNumber)
                    .put("section", value.section).put("retentionUntilEpochMs", value.retentionUntilEpochMs)
                    .put("createdAtEpochMs", value.createdAtEpochMs));
        }
        root.put("evidenceReferences", evidence);
        JSONArray reviews = new JSONArray();
        for (ResolutionReview value : state.resolutionReviews) {
            reviews.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("issueId", value.issueId).put("workOrderId", value.workOrderId)
                    .put("proposedBy", value.proposedBy).put("proposedAtEpochMs", value.proposedAtEpochMs)
                    .put("summary", value.summary).put("status", value.status.name())
                    .put("reviewedBy", value.reviewedBy).put("reviewedAtEpochMs", value.reviewedAtEpochMs)
                    .put("reviewNote", value.reviewNote));
        }
        root.put("resolutionReviews", reviews);
        JSONArray recurrences = new JSONArray();
        for (RecurrenceEvent value : state.recurrences) {
            recurrences.put(new JSONObject()
                    .put("id", value.id).put("communityId", value.communityId)
                    .put("previousIssueId", value.previousIssueId).put("newIssueId", value.newIssueId)
                    .put("assetId", value.assetId).put("reportedBy", value.reportedBy)
                    .put("notes", value.notes).put("occurredAtEpochMs", value.occurredAtEpochMs));
        }
        root.put("recurrenceEvents", recurrences);
        JSONArray activities = new JSONArray();
        for (ActivityEvent value : state.activities) {
            activities.put(new JSONObject()
                    .put("id", value.id).put("sequence", value.sequence)
                    .put("communityId", value.communityId).put("issueId", value.issueId)
                    .put("relatedIssueId", value.relatedIssueId).put("workOrderId", value.workOrderId)
                    .put("assetId", value.assetId).put("actorId", value.actorId)
                    .put("actorRole", value.actorRole.name()).put("type", value.type.name())
                    .put("title", value.title).put("detail", value.detail)
                    .put("occurredAtEpochMs", value.occurredAtEpochMs)
                    .put("evidenceId", value.evidenceId));
        }
        root.put("activityEvents", activities);
        return root.toString();
    }

    static IssueOperationsState parse(String json) {
        if (json == null || json.trim().isEmpty()) return empty();
        JSONObject root = new JSONObject(json);
        int version = root.getInt("schemaVersion");
        if (version != IssueOperationsState.SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported operations schema version: " + version);
        }
        IssueOperationsState state = empty();
        state.nextId = root.getLong("nextId");
        state.nextActivitySequence = root.getLong("nextActivitySequence");
        if (state.nextId <= 0 || state.nextActivitySequence <= 0) {
            throw new IllegalArgumentException("Operations ID sequences must be positive.");
        }
        JSONArray warnings = root.optJSONArray("migrationWarnings");
        if (warnings != null) for (int i = 0; i < warnings.length(); i++) state.migrationWarnings.add(warnings.getString(i));
        JSONArray complaints = requiredArray(root, "complaints");
        for (int i = 0; i < complaints.length(); i++) state.complaints.add(readComplaint(complaints.getJSONObject(i)));
        JSONArray assets = requiredArray(root, "assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject o = assets.getJSONObject(i);
            state.assets.add(new Asset(o.getString("id"), o.getString("communityId"), o.getString("name"),
                    o.getString("type"), o.getString("location"), enumValue(Asset.Criticality.class, o.getString("criticality")),
                    enumValue(Asset.Status.class, o.getString("status")), o.getLong("installedAtEpochMs"),
                    o.getLong("createdAtEpochMs")));
        }
        JSONArray relations = requiredArray(root, "issueRelations");
        for (int i = 0; i < relations.length(); i++) {
            JSONObject o = relations.getJSONObject(i);
            state.relations.add(new IssueRelation(o.getString("id"), o.getString("communityId"),
                    o.getString("sourceIssueId"), o.getString("targetIssueId"),
                    enumValue(IssueRelation.Type.class, o.getString("type")),
                    enumValue(IssueRelation.ReviewStatus.class, o.getString("reviewStatus")),
                    o.getString("rationale"), o.getString("createdBy"), o.getLong("createdAtEpochMs"),
                    o.optString("reviewedBy", ""), o.optLong("reviewedAtEpochMs", 0L)));
        }
        JSONArray workOrders = requiredArray(root, "workOrders");
        for (int i = 0; i < workOrders.length(); i++) {
            JSONObject o = workOrders.getJSONObject(i);
            state.workOrders.add(new WorkOrder(o.getString("id"), o.getString("communityId"),
                    o.getString("issueId"), o.optString("assetId", ""), o.getString("assigneeId"),
                    o.getString("assignedBy"), enumValue(WorkOrder.Status.class, o.getString("status")),
                    o.getLong("createdAtEpochMs"), o.getLong("updatedAtEpochMs"), o.getLong("version")));
        }
        JSONArray workLogs = requiredArray(root, "workLogs");
        for (int i = 0; i < workLogs.length(); i++) {
            JSONObject o = workLogs.getJSONObject(i);
            state.workLogs.add(new WorkLog(o.getString("id"), o.getString("communityId"),
                    o.getString("issueId"), o.getString("workOrderId"), o.getString("authorId"),
                    enumValue(WorkLog.Type.class, o.getString("type")), o.getString("details"),
                    o.optString("nextStep", ""), o.optString("materials", ""), o.getLong("createdAtEpochMs")));
        }
        JSONArray evidence = requiredArray(root, "evidenceReferences");
        for (int i = 0; i < evidence.length(); i++) {
            JSONObject o = evidence.getJSONObject(i);
            state.evidence.add(new EvidenceReference(o.getString("id"), o.getString("communityId"),
                    o.getString("issueId"), o.getString("workLogId"), o.getString("uploadedBy"),
                    o.getString("displayName"), o.getString("reference"), o.getString("mediaType"),
                    o.getLong("sizeBytes"), enumValue(EvidenceReference.AccessScope.class, o.getString("accessScope")),
                    enumValue(EvidenceReference.StorageState.class, o.getString("storageState")),
                    o.optInt("pageNumber", 0), o.optString("section", ""),
                    o.optLong("retentionUntilEpochMs", 0L), o.getLong("createdAtEpochMs")));
        }
        JSONArray reviews = requiredArray(root, "resolutionReviews");
        for (int i = 0; i < reviews.length(); i++) {
            JSONObject o = reviews.getJSONObject(i);
            state.resolutionReviews.add(new ResolutionReview(o.getString("id"), o.getString("communityId"),
                    o.getString("issueId"), o.getString("workOrderId"), o.getString("proposedBy"),
                    o.getLong("proposedAtEpochMs"), o.getString("summary"),
                    enumValue(ResolutionReview.Status.class, o.getString("status")),
                    o.optString("reviewedBy", ""), o.optLong("reviewedAtEpochMs", 0L),
                    o.optString("reviewNote", "")));
        }
        JSONArray recurrences = requiredArray(root, "recurrenceEvents");
        for (int i = 0; i < recurrences.length(); i++) {
            JSONObject o = recurrences.getJSONObject(i);
            state.recurrences.add(new RecurrenceEvent(o.getString("id"), o.getString("communityId"),
                    o.getString("previousIssueId"), o.getString("newIssueId"), o.optString("assetId", ""),
                    o.getString("reportedBy"), o.getString("notes"), o.getLong("occurredAtEpochMs")));
        }
        JSONArray activities = requiredArray(root, "activityEvents");
        long maximumSequence = 0;
        for (int i = 0; i < activities.length(); i++) {
            JSONObject o = activities.getJSONObject(i);
            ActivityEvent value = new ActivityEvent(o.getString("id"), o.getLong("sequence"),
                    o.getString("communityId"), o.optString("issueId", ""),
                    o.optString("relatedIssueId", ""), o.optString("workOrderId", ""),
                    o.optString("assetId", ""), o.getString("actorId"),
                    enumValue(CommunityRole.class, o.getString("actorRole")),
                    enumValue(ActivityEvent.Type.class, o.getString("type")), o.getString("title"),
                    o.optString("detail", ""), o.optLong("occurredAtEpochMs", 0L),
                    o.optString("evidenceId", ""));
            maximumSequence = Math.max(maximumSequence, value.sequence);
            state.activities.add(value);
        }
        if (state.nextActivitySequence <= maximumSequence) {
            throw new IllegalArgumentException("Next activity sequence would duplicate an existing event.");
        }
        validate(state);
        return state;
    }

    private static JSONObject writeComplaint(Complaint value) {
        return new JSONObject().put("id", value.id).put("title", value.title)
                .put("description", value.description).put("category", value.category)
                .put("priority", value.priority).put("apartmentUnit", value.apartmentUnit)
                .put("createdAt", value.createdAt).put("createdAtEpochMs", value.createdAtEpochMs)
                .put("updatedAt", value.updatedAt).put("updatedAtEpochMs", value.updatedAtEpochMs)
                .put("status", value.status).put("outcomeStatus", value.outcomeStatus.name())
                .put("communityId", value.communityId).put("reporterId", value.reporterId)
                .put("assetId", value.assetId).put("location", value.location)
                .put("version", value.version).put("history", strings(value.history));
    }

    private static Complaint readComplaint(JSONObject o) {
        String status = o.optString("status", "Open");
        String createdAt = o.getString("createdAt");
        Complaint.OutcomeStatus defaultOutcome = "Resolved".equals(status)
                ? Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED : Complaint.OutcomeStatus.NOT_PROPOSED;
        Complaint value = new Complaint(o.getString("id"), o.getString("title"), o.getString("description"),
                o.getString("category"), o.getString("priority"), o.getString("apartmentUnit"),
                createdAt, o.optLong("createdAtEpochMs", 0L), o.optString("updatedAt", createdAt),
                o.optLong("updatedAtEpochMs", 0L), status,
                o.has("outcomeStatus") ? enumValue(Complaint.OutcomeStatus.class, o.getString("outcomeStatus")) : defaultOutcome,
                o.optString("communityId", Complaint.LEGACY_COMMUNITY_ID),
                o.optString("reporterId", Complaint.LEGACY_REPORTER_ID), o.optString("assetId", ""),
                o.optString("location", o.getString("apartmentUnit")), o.optLong("version", 0L));
        JSONArray history = o.optJSONArray("history");
        if (history != null) {
            value.history.clear();
            for (int i = 0; i < history.length(); i++) value.history.add(history.getString(i));
        }
        return value;
    }

    private static JSONArray strings(Iterable<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array;
    }

    private static JSONArray requiredArray(JSONObject root, String key) {
        JSONArray value = root.optJSONArray(key);
        if (value == null) throw new IllegalArgumentException("Missing operations array: " + key);
        return value;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Unsupported " + type.getSimpleName() + ": " + value, error);
        }
    }

    private static void validate(IssueOperationsState state) {
        Set<String> activitySequences = new HashSet<>();
        long maximumGeneratedSuffix = 0;
        for (Complaint value : state.complaints) {
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
            if (!value.assetId.isEmpty()) {
                Asset asset = state.asset(value.assetId);
                require(asset != null && asset.communityId.equals(value.communityId), "Complaint references an invalid asset.");
            }
            state.activeWorkOrderForIssue(value.id);
        }
        requireUniqueComplaints(state);
        requireUniqueAssets(state);
        for (WorkOrder value : state.workOrders) {
            Complaint issue = state.issue(value.issueId);
            require(issue != null && issue.communityId.equals(value.communityId), "Work order references an invalid issue.");
            if (!value.assetId.isEmpty()) {
                Asset asset = state.asset(value.assetId);
                require(asset != null && asset.communityId.equals(value.communityId), "Work order references an invalid asset.");
            }
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        Set<String> workOrderIds = new HashSet<>();
        for (WorkOrder value : state.workOrders) require(workOrderIds.add(value.id), "Duplicate work order ID: " + value.id);
        Set<String> workLogIds = new HashSet<>();
        for (WorkLog value : state.workLogs) {
            require(workLogIds.add(value.id), "Duplicate work log ID: " + value.id);
            Complaint issue = state.issue(value.issueId);
            WorkOrder workOrder = state.workOrder(value.workOrderId);
            require(issue != null && workOrder != null && issue.communityId.equals(value.communityId)
                    && workOrder.issueId.equals(value.issueId) && workOrder.communityId.equals(value.communityId),
                    "Work log references an invalid issue or work order.");
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        Set<String> evidenceIds = new HashSet<>();
        for (EvidenceReference value : state.evidence) {
            require(evidenceIds.add(value.id), "Duplicate evidence ID: " + value.id);
            WorkLog log = state.workLog(value.workLogId);
            require(log != null && log.issueId.equals(value.issueId) && log.communityId.equals(value.communityId),
                    "Evidence references an invalid work log.");
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        Set<String> reviewIds = new HashSet<>();
        Set<String> awaitingReviewIssueIds = new HashSet<>();
        for (ResolutionReview value : state.resolutionReviews) {
            require(reviewIds.add(value.id), "Duplicate resolution review ID: " + value.id);
            if (value.status == ResolutionReview.Status.AWAITING_RESIDENT) {
                require(awaitingReviewIssueIds.add(value.issueId),
                        "Issue has multiple awaiting resolution reviews: " + value.issueId);
            }
            Complaint issue = state.issue(value.issueId);
            WorkOrder workOrder = state.workOrder(value.workOrderId);
            require(issue != null && workOrder != null && issue.communityId.equals(value.communityId)
                    && workOrder.issueId.equals(value.issueId), "Resolution review references an invalid issue or work order.");
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        Set<String> relationIds = new HashSet<>();
        for (IssueRelation value : state.relations) {
            require(relationIds.add(value.id), "Duplicate relation ID: " + value.id);
            Complaint source = state.issue(value.sourceIssueId);
            Complaint target = state.issue(value.targetIssueId);
            require(source != null && target != null && source.communityId.equals(value.communityId)
                    && target.communityId.equals(value.communityId), "Relation references an invalid issue.");
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        Set<String> recurrenceIds = new HashSet<>();
        for (RecurrenceEvent value : state.recurrences) {
            require(recurrenceIds.add(value.id), "Duplicate recurrence ID: " + value.id);
            Complaint previous = state.issue(value.previousIssueId);
            Complaint current = state.issue(value.newIssueId);
            require(previous != null && current != null && previous.communityId.equals(value.communityId)
                    && current.communityId.equals(value.communityId), "Recurrence references an invalid issue.");
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        Set<String> activityIds = new HashSet<>();
        for (ActivityEvent value : state.activities) {
            require(activityIds.add(value.id), "Duplicate activity ID: " + value.id);
            require(activitySequences.add(String.valueOf(value.sequence)), "Duplicate activity sequence: " + value.sequence);
            if (!value.issueId.isEmpty()) {
                Complaint issue = state.issue(value.issueId);
                require(issue != null && issue.communityId.equals(value.communityId), "Activity references an invalid issue.");
            }
            if (!value.relatedIssueId.isEmpty()) {
                Complaint issue = state.issue(value.relatedIssueId);
                require(issue != null && issue.communityId.equals(value.communityId), "Activity references an invalid related issue.");
            }
            if (!value.evidenceId.isEmpty()) {
                EvidenceReference evidence = state.evidence(value.evidenceId);
                require(value.type == ActivityEvent.Type.EVIDENCE_ADDED && evidence != null
                                && evidence.communityId.equals(value.communityId)
                                && evidence.issueId.equals(value.issueId),
                        "Activity references invalid evidence metadata.");
            }
            if (value.type != ActivityEvent.Type.ISSUE_IMPORTED) {
                require(value.occurredAtEpochMs > 0, "Only imported events may have an unknown instant.");
            }
            maximumGeneratedSuffix = Math.max(maximumGeneratedSuffix, generatedSuffix(value.id));
        }
        state.nextId = Math.max(state.nextId, maximumGeneratedSuffix + 1);
    }

    private static void requireUniqueComplaints(IssueOperationsState state) {
        Set<String> ids = new HashSet<>();
        for (Complaint value : state.complaints) require(ids.add(value.id), "Duplicate complaint ID: " + value.id);
    }

    private static void requireUniqueAssets(IssueOperationsState state) {
        Set<String> ids = new HashSet<>();
        for (Asset value : state.assets) require(ids.add(value.id), "Duplicate asset ID: " + value.id);
    }

    private static long generatedSuffix(String id) {
        int separator = id.lastIndexOf('-');
        if (separator < 0 || separator == id.length() - 1) return 0;
        String suffix = id.substring(separator + 1);
        if (!suffix.matches("[0-9]{6,}")) return 0;
        try { return Long.parseLong(suffix); } catch (NumberFormatException ignored) { return 0; }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }
}
