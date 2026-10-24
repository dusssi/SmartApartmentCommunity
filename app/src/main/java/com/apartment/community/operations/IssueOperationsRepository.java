package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;

import java.util.List;

/** Local/domain contract that a later server-backed repository can implement without changing UI intent. */
public interface IssueOperationsRepository {
    OperationResult<Complaint> createComplaint(Actor actor, String title, String description,
            String category, String priority, String apartmentUnit, String location);

    OperationResult<Asset> createAsset(Actor actor, String name, String type, String location,
            Asset.Criticality criticality, long installedAtEpochMs);

    OperationResult<WorkOrder> assignIssue(Actor actor, String issueId, String assigneeId, String assetId);

    OperationResult<WorkOrder> changeWorkOrderStatus(Actor actor, String workOrderId,
            long expectedVersion, WorkOrder.Status targetStatus);

    OperationResult<WorkLog> addWorkLog(Actor actor, String workOrderId, long expectedVersion,
            WorkLog.Type type, String details, String nextStep, String materials);

    OperationResult<EvidenceReference> addEvidence(Actor actor, String workLogId,
            String displayName, String reference, String mediaType, long sizeBytes,
            EvidenceReference.AccessScope accessScope, int pageNumber, String section,
            long retentionUntilEpochMs);

    OperationResult<ResolutionReview> proposeResolution(Actor actor, String workOrderId,
            long expectedVersion, String summary);

    OperationResult<ResolutionReview> reviewResolution(Actor actor, String reviewId,
            boolean confirmed, String note);

    OperationResult<Complaint> reopenIssue(Actor actor, String issueId, String reason);

    OperationResult<Complaint> reportRecurrence(Actor actor, String previousIssueId,
            String title, String description, String category, String priority,
            String apartmentUnit, String location, String notes);

    OperationResult<List<Complaint>> listIssues(Actor actor);

    OperationResult<IssueDetails> issueDetails(Actor actor, String issueId);

    OperationResult<List<ActivityEvent>> activityFeed(Actor actor, String query,
            ActivityEvent.Type type, int offset, int limit);

    OperationResult<List<Asset>> listAssets(Actor actor);

    OperationResult<AssetHistory> assetHistory(Actor actor, String assetId);

    OperationResult<ResolutionQuality> resolutionQuality(Actor actor);
}
