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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Package-private mutable candidate state. Only LocalIssueOperationsRepository publishes it. */
final class IssueOperationsState {
    static final int SCHEMA_VERSION = 1;

    long nextId = 1;
    long nextActivitySequence = 1;
    final List<String> migrationWarnings = new ArrayList<>();
    final List<Complaint> complaints = new ArrayList<>();
    final List<Asset> assets = new ArrayList<>();
    final List<IssueRelation> relations = new ArrayList<>();
    final List<WorkOrder> workOrders = new ArrayList<>();
    final List<WorkLog> workLogs = new ArrayList<>();
    final List<EvidenceReference> evidence = new ArrayList<>();
    final List<ResolutionReview> resolutionReviews = new ArrayList<>();
    final List<RecurrenceEvent> recurrences = new ArrayList<>();
    final List<ActivityEvent> activities = new ArrayList<>();

    String newId(String prefix) {
        return prefix + "-" + String.format(Locale.US, "%06d", nextId++);
    }

    Complaint issue(String id) {
        for (Complaint issue : complaints) if (issue.id.equals(id)) return issue;
        return null;
    }

    Asset asset(String id) {
        for (Asset asset : assets) if (asset.id.equals(id)) return asset;
        return null;
    }

    WorkOrder workOrder(String id) {
        for (WorkOrder workOrder : workOrders) if (workOrder.id.equals(id)) return workOrder;
        return null;
    }

    WorkOrder activeWorkOrderForIssue(String issueId) {
        WorkOrder result = null;
        for (WorkOrder workOrder : workOrders) {
            if (workOrder.issueId.equals(issueId) && workOrder.isActive()) {
                if (result != null) throw new IllegalStateException("Issue has multiple active work orders: " + issueId);
                result = workOrder;
            }
        }
        return result;
    }

    WorkOrder latestWorkOrderForIssue(String issueId) {
        WorkOrder result = null;
        for (WorkOrder workOrder : workOrders) {
            if (workOrder.issueId.equals(issueId)) result = workOrder;
        }
        return result;
    }

    ResolutionReview activeReviewForIssue(String issueId) {
        for (int i = resolutionReviews.size() - 1; i >= 0; i--) {
            ResolutionReview review = resolutionReviews.get(i);
            if (review.issueId.equals(issueId) && review.status == ResolutionReview.Status.AWAITING_RESIDENT) {
                return review;
            }
        }
        return null;
    }

    WorkLog workLog(String id) {
        for (WorkLog workLog : workLogs) if (workLog.id.equals(id)) return workLog;
        return null;
    }

    void replace(Complaint replacement) {
        for (int i = 0; i < complaints.size(); i++) {
            if (complaints.get(i).id.equals(replacement.id)) {
                complaints.set(i, replacement);
                return;
            }
        }
        throw new IllegalArgumentException("Complaint not found: " + replacement.id);
    }

    void replace(WorkOrder replacement) {
        for (int i = 0; i < workOrders.size(); i++) {
            if (workOrders.get(i).id.equals(replacement.id)) {
                workOrders.set(i, replacement);
                return;
            }
        }
        throw new IllegalArgumentException("Work order not found: " + replacement.id);
    }

    void replace(ResolutionReview replacement) {
        for (int i = 0; i < resolutionReviews.size(); i++) {
            if (resolutionReviews.get(i).id.equals(replacement.id)) {
                resolutionReviews.set(i, replacement);
                return;
            }
        }
        throw new IllegalArgumentException("Resolution review not found: " + replacement.id);
    }

    ActivityEvent event(Actor actor, ActivityEvent.Type type, String issueId, String relatedIssueId,
                        String workOrderId, String assetId, String title, String detail, long at) {
        ActivityEvent event = new ActivityEvent(newId("ACT"), nextActivitySequence++,
                actor.communityId, issueId, relatedIssueId, workOrderId, assetId, actor.id,
                actor.role, type, title, detail, at);
        activities.add(event);
        return event;
    }

    ActivityEvent importedEvent(Complaint issue) {
        ActivityEvent event = new ActivityEvent(newId("ACT"), nextActivitySequence++,
                issue.communityId, issue.id, "", "", issue.assetId, "local-migration",
                CommunityRole.MANAGEMENT, ActivityEvent.Type.ISSUE_IMPORTED,
                "Existing complaint imported",
                "Legacy complaint and status history preserved. Original status: " + issue.status
                        + ". Exact historical instants are unavailable.", 0L);
        activities.add(event);
        return event;
    }
}
