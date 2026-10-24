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
import com.apartment.community.util.DomainValidation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Transactional local implementation. Every mutation runs against a deep-copied candidate;
 * the candidate is published only after the versioned JSON has been synchronously persisted.
 */
public final class LocalIssueOperationsRepository implements IssueOperationsRepository {
    public interface Storage {
        String read() throws Exception;
        /** Must return only after the value is durably accepted, or throw/return failure. */
        void write(String value) throws Exception;
    }

    private interface Mutation<T> {
        OperationResult<T> apply(IssueOperationsState candidate);
    }

    private static final class DomainFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final OperationResult.Error error;
        DomainFailure(OperationResult.Error error, String message) {
            super(message);
            this.error = error;
        }
    }

    private final Storage storage;
    private final OperationsClock clock;
    private IssueOperationsState state;

    private LocalIssueOperationsRepository(Storage storage, OperationsClock clock,
                                           IssueOperationsState initialState) {
        this.storage = storage;
        this.clock = clock;
        this.state = initialState;
    }

    /** Loads schema v1 or performs a one-time import of the existing complaint JSON projection. */
    public static OperationResult<LocalIssueOperationsRepository> load(
            Storage storage, String legacyComplaintsJson, OperationsClock clock) {
        if (storage == null || clock == null) {
            return OperationResult.failure(OperationResult.Error.INVALID_INPUT,
                    "Storage and clock are required.");
        }
        final String existing;
        try {
            existing = storage.read();
        } catch (Exception error) {
            return OperationResult.failure(OperationResult.Error.STORAGE_ERROR,
                    "Issue operations storage could not be read.");
        }
        final IssueOperationsState initial;
        try {
            if (existing == null || existing.trim().isEmpty()) {
                initial = IssueOperationsJson.migrateLegacy(legacyComplaintsJson);
            } else {
                initial = IssueOperationsJson.parse(existing);
            }
        } catch (RuntimeException error) {
            return OperationResult.failure(OperationResult.Error.CORRUPT_DATA,
                    "Issue operations could not be loaded. Existing stored data was not replaced. "
                            + safeMessage(error));
        }
        if (existing == null || existing.trim().isEmpty()) {
            try {
                storage.write(IssueOperationsJson.write(initial));
            } catch (Exception error) {
                return OperationResult.failure(OperationResult.Error.STORAGE_ERROR,
                        "Issue operations could not initialize local storage. Legacy data was not replaced.");
            }
        }
        return OperationResult.success(new LocalIssueOperationsRepository(storage, clock, initial));
    }

    @Override
    public synchronized OperationResult<Complaint> createComplaint(
            Actor actor, String title, String description, String category, String priority,
            String apartmentUnit, String location) {
        return mutate(candidate -> {
            requireRole(actor, CommunityRole.RESIDENT, CommunityRole.MANAGEMENT);
            return OperationResult.success(createComplaintRecord(candidate, actor, actor.id,
                    title, description, category, priority, apartmentUnit, location, "", now()));
        });
    }

    @Override
    public synchronized OperationResult<Asset> createAsset(
            Actor actor, String name, String type, String location,
            Asset.Criticality criticality, long installedAtEpochMs) {
        return mutate(candidate -> {
            requireRole(actor, CommunityRole.MANAGEMENT);
            long at = now();
            Asset asset = new Asset(candidate.newId("ASSET"), actor.communityId, name, type,
                    location, criticality, Asset.Status.OPERATIONAL, installedAtEpochMs, at);
            candidate.assets.add(asset);
            candidate.event(actor, ActivityEvent.Type.ASSET_CREATED, "", "", "", asset.id,
                    "Asset added", asset.name + " · " + asset.location, at);
            return OperationResult.success(asset);
        });
    }

    @Override
    public synchronized OperationResult<WorkOrder> assignIssue(
            Actor actor, String issueId, String assigneeId, String assetId) {
        return mutate(candidate -> {
            requireRole(actor, CommunityRole.MANAGEMENT);
            Complaint issue = requireIssue(candidate, actor, issueId);
            if ("Resolved".equals(issue.status)) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Reopen a resolved issue before assigning new work.");
            }
            if (candidate.activeWorkOrderForIssue(issue.id) != null) {
                throw failure(OperationResult.Error.CONFLICT,
                        "This issue already has an active work order.");
            }
            String linkedAsset = assetId == null || assetId.trim().isEmpty() ? issue.assetId : assetId.trim();
            if (!linkedAsset.isEmpty()) {
                Asset asset = candidate.asset(linkedAsset);
                if (asset == null) throw failure(OperationResult.Error.NOT_FOUND, "Asset not found.");
                requireCommunity(actor, asset.communityId);
            }
            String assignee = DomainValidation.id(assigneeId, "Assignee ID");
            long at = now();
            WorkOrder workOrder = new WorkOrder(candidate.newId("WO"), actor.communityId,
                    issue.id, linkedAsset, assignee, actor.id, WorkOrder.Status.ASSIGNED,
                    at, at, 1L);
            candidate.workOrders.add(workOrder);
            Complaint updated = issue.workflowCopy(issue.status, issue.outcomeStatus, linkedAsset,
                    displayTime(at), at);
            updated.history.add(displayTime(at) + " · Assigned to " + assignee);
            candidate.replace(updated);
            candidate.event(actor, ActivityEvent.Type.ISSUE_ASSIGNED, issue.id, "", workOrder.id,
                    linkedAsset, "Work assigned", "Assigned to " + assignee
                            + (linkedAsset.isEmpty() ? "" : " · Asset " + linkedAsset), at);
            return OperationResult.success(workOrder);
        });
    }

    @Override
    public synchronized OperationResult<WorkOrder> changeWorkOrderStatus(
            Actor actor, String workOrderId, long expectedVersion, WorkOrder.Status targetStatus) {
        return mutate(candidate -> {
            WorkOrder current = requireWorkOrder(candidate, actor, workOrderId);
            requireWorkActor(actor, current);
            if (targetStatus == WorkOrder.Status.CANCELLED) requireRole(actor, CommunityRole.MANAGEMENT);
            if (targetStatus == WorkOrder.Status.AWAITING_CONFIRMATION
                    || targetStatus == WorkOrder.Status.COMPLETED
                    || targetStatus == WorkOrder.Status.REOPENED) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Use the resolution or reopen workflow for this status.");
            }
            requireVersion(current, expectedVersion);
            long at = now();
            WorkOrder updated = current.transition(targetStatus, at);
            candidate.replace(updated);
            Complaint issue = requireIssue(candidate, actor, current.issueId);
            String nextIssueStatus = issue.status;
            if (targetStatus == WorkOrder.Status.IN_PROGRESS) nextIssueStatus = "In Progress";
            if (targetStatus == WorkOrder.Status.CANCELLED) nextIssueStatus = "Open";
            Complaint updatedIssue = issue.workflowCopy(nextIssueStatus, issue.outcomeStatus,
                    issue.assetId, displayTime(at), at);
            updatedIssue.history.add(displayTime(at) + " · Work order " + targetStatus);
            candidate.replace(updatedIssue);
            candidate.event(actor, ActivityEvent.Type.WORK_ORDER_STATUS_CHANGED, issue.id, "",
                    current.id, current.assetId, "Work order " + display(targetStatus),
                    "Status changed from " + display(current.status) + " to " + display(targetStatus), at);
            return OperationResult.success(updated);
        });
    }

    @Override
    public synchronized OperationResult<WorkLog> addWorkLog(
            Actor actor, String workOrderId, long expectedVersion, WorkLog.Type type,
            String details, String nextStep, String materials) {
        return mutate(candidate -> {
            WorkOrder current = requireWorkOrder(candidate, actor, workOrderId);
            requireWorkActor(actor, current);
            requireVersion(current, expectedVersion);
            if (current.status == WorkOrder.Status.AWAITING_CONFIRMATION
                    || current.status == WorkOrder.Status.COMPLETED
                    || current.status == WorkOrder.Status.CANCELLED) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Reopen this work before adding another work log.");
            }
            long at = now();
            WorkOrder active = current;
            if (current.status == WorkOrder.Status.ASSIGNED
                    || current.status == WorkOrder.Status.ACCEPTED
                    || current.status == WorkOrder.Status.REOPENED) {
                active = current.transition(WorkOrder.Status.IN_PROGRESS, at);
                candidate.replace(active);
                candidate.event(actor, ActivityEvent.Type.WORK_ORDER_STATUS_CHANGED,
                        current.issueId, "", current.id, current.assetId, "Work in progress",
                        "Work started from " + display(current.status) + ".", at);
            }
            WorkLog log = new WorkLog(candidate.newId("LOG"), actor.communityId,
                    current.issueId, current.id, actor.id, type, details, nextStep, materials, at);
            candidate.workLogs.add(log);
            Complaint issue = requireIssue(candidate, actor, current.issueId);
            Complaint updatedIssue = issue.workflowCopy("In Progress", issue.outcomeStatus,
                    issue.assetId, displayTime(at), at);
            updatedIssue.history.add(displayTime(at) + " · " + display(type));
            candidate.replace(updatedIssue);
            setAssetMaintenance(candidate, current.assetId);
            candidate.event(actor, ActivityEvent.Type.WORK_LOG_ADDED, issue.id, "", current.id,
                    current.assetId, display(type), log.details
                            + (log.nextStep.isEmpty() ? "" : " Next step: " + log.nextStep), at);
            return OperationResult.success(log);
        });
    }

    @Override
    public synchronized OperationResult<EvidenceReference> addEvidence(
            Actor actor, String workLogId, String displayName, String reference,
            String mediaType, long sizeBytes, EvidenceReference.AccessScope accessScope,
            int pageNumber, String section, long retentionUntilEpochMs) {
        return mutate(candidate -> {
            WorkLog log = candidate.workLog(workLogId);
            if (log == null) throw failure(OperationResult.Error.NOT_FOUND, "Work log not found.");
            requireCommunity(actor, log.communityId);
            WorkOrder workOrder = requireWorkOrder(candidate, actor, log.workOrderId);
            requireWorkActor(actor, workOrder);
            if (workOrder.status == WorkOrder.Status.COMPLETED
                    || workOrder.status == WorkOrder.Status.CANCELLED) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Evidence metadata cannot be added to closed work.");
            }
            long at = now();
            EvidenceReference evidence = new EvidenceReference(candidate.newId("EVIDENCE"),
                    actor.communityId, log.issueId, log.id, actor.id, displayName, reference,
                    mediaType, sizeBytes, accessScope, EvidenceReference.StorageState.METADATA_ONLY,
                    pageNumber, section, retentionUntilEpochMs, at);
            candidate.evidence.add(evidence);
            candidate.event(actor, ActivityEvent.Type.EVIDENCE_ADDED, log.issueId, "",
                    workOrder.id, workOrder.assetId, "Evidence metadata added",
                    evidence.displayName + " · " + evidence.mediaType
                            + " · Metadata reference only; no binary file uploaded.", at, evidence.id);
            return OperationResult.success(evidence);
        });
    }

    @Override
    public synchronized OperationResult<ResolutionReview> proposeResolution(
            Actor actor, String workOrderId, long expectedVersion, String summary) {
        return mutate(candidate -> {
            WorkOrder current = requireWorkOrder(candidate, actor, workOrderId);
            requireWorkActor(actor, current);
            requireVersion(current, expectedVersion);
            if (current.status != WorkOrder.Status.IN_PROGRESS) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Work must be in progress before a resolution is proposed.");
            }
            if (candidate.activeReviewForIssue(current.issueId) != null) {
                throw failure(OperationResult.Error.CONFLICT,
                        "A resolution is already awaiting resident review.");
            }
            boolean hasWorkPerformed = false;
            for (WorkLog log : candidate.workLogs) {
                if (log.workOrderId.equals(current.id) && log.type == WorkLog.Type.WORK_PERFORMED) {
                    hasWorkPerformed = true;
                    break;
                }
            }
            if (!hasWorkPerformed) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Record work performed before proposing a resolution.");
            }
            long at = now();
            WorkOrder updated = current.transition(WorkOrder.Status.AWAITING_CONFIRMATION, at);
            candidate.replace(updated);
            ResolutionReview review = new ResolutionReview(candidate.newId("REVIEW"),
                    actor.communityId, current.issueId, current.id, actor.id, at, summary,
                    ResolutionReview.Status.AWAITING_RESIDENT, "", 0L, "");
            candidate.resolutionReviews.add(review);
            Complaint issue = requireIssue(candidate, actor, current.issueId);
            Complaint updatedIssue = issue.workflowCopy("Resolved",
                    Complaint.OutcomeStatus.AWAITING_CONFIRMATION, issue.assetId, displayTime(at), at);
            updatedIssue.history.add(displayTime(at) + " · Resolution proposed; resident verification pending");
            candidate.replace(updatedIssue);
            candidate.event(actor, ActivityEvent.Type.RESOLUTION_PROPOSED, issue.id, "",
                    current.id, current.assetId, "Resolution proposed — verification pending",
                    review.summary, at);
            return OperationResult.success(review);
        });
    }

    @Override
    public synchronized OperationResult<ResolutionReview> reviewResolution(
            Actor actor, String reviewId, boolean confirmed, String note) {
        return mutate(candidate -> {
            requireRole(actor, CommunityRole.RESIDENT);
            ResolutionReview current = requireReview(candidate, reviewId);
            requireCommunity(actor, current.communityId);
            Complaint issue = requireIssue(candidate, actor, current.issueId);
            if (!issue.reporterId.equals(actor.id)) {
                throw failure(OperationResult.Error.NOT_AUTHORIZED,
                        "Only the resident who reported this issue can review its resolution.");
            }
            WorkOrder workOrder = requireWorkOrder(candidate, actor, current.workOrderId);
            if (workOrder.status != WorkOrder.Status.AWAITING_CONFIRMATION) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "This work order is not awaiting resident confirmation.");
            }
            long at = now();
            ResolutionReview updated = current.review(confirmed, actor.id, at, note);
            candidate.replace(updated);
            WorkOrder updatedWorkOrder = workOrder.transition(confirmed
                    ? WorkOrder.Status.COMPLETED : WorkOrder.Status.IN_PROGRESS, at);
            candidate.replace(updatedWorkOrder);
            Complaint updatedIssue = issue.workflowCopy(confirmed ? "Resolved" : "In Progress",
                    confirmed ? Complaint.OutcomeStatus.VERIFIED : Complaint.OutcomeStatus.DISPUTED,
                    issue.assetId, displayTime(at), at);
            updatedIssue.history.add(displayTime(at) + (confirmed
                    ? " · Resident verified the resolution" : " · Resident disputed the resolution: " + note));
            candidate.replace(updatedIssue);
            if (confirmed) setAssetOperational(candidate, workOrder.assetId);
            candidate.event(actor, confirmed ? ActivityEvent.Type.RESOLUTION_CONFIRMED
                            : ActivityEvent.Type.RESOLUTION_DISPUTED, issue.id, "", workOrder.id,
                    workOrder.assetId, confirmed ? "Resident verified resolution" : "Resident disputed resolution",
                    confirmed && (note == null || note.trim().isEmpty()) ? "Resident confirmed the proposed outcome." : note,
                    at);
            return OperationResult.success(updated);
        });
    }

    @Override
    public synchronized OperationResult<Complaint> reopenIssue(Actor actor, String issueId, String reason) {
        return mutate(candidate -> {
            Complaint issue = requireIssue(candidate, actor, issueId);
            requireResidentOwnerOrManagement(actor, issue);
            if (issue.outcomeStatus != Complaint.OutcomeStatus.AWAITING_CONFIRMATION
                    && issue.outcomeStatus != Complaint.OutcomeStatus.VERIFIED
                    && issue.outcomeStatus != Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED) {
                throw failure(OperationResult.Error.INVALID_TRANSITION,
                        "Only a proposed, verified, or legacy-resolved issue can be reopened.");
            }
            String validReason = DomainValidation.required(reason, "Reopen reason", 3, 2000);
            long at = now();
            ResolutionReview awaiting = candidate.activeReviewForIssue(issue.id);
            if (awaiting != null) candidate.replace(awaiting.supersede(actor.id, at, validReason));
            WorkOrder latest = candidate.latestWorkOrderForIssue(issue.id);
            if (latest != null && (latest.status == WorkOrder.Status.AWAITING_CONFIRMATION
                    || latest.status == WorkOrder.Status.COMPLETED)) {
                candidate.replace(latest.transition(WorkOrder.Status.REOPENED, at));
                setAssetMaintenance(candidate, latest.assetId);
            }
            Complaint updated = issue.workflowCopy("Open", Complaint.OutcomeStatus.REOPENED,
                    issue.assetId, displayTime(at), at);
            updated.history.add(displayTime(at) + " · Reopened: " + validReason);
            candidate.replace(updated);
            candidate.event(actor, ActivityEvent.Type.ISSUE_REOPENED, issue.id, "",
                    latest == null ? "" : latest.id, issue.assetId, "Issue reopened", validReason, at);
            return OperationResult.success(updated);
        });
    }

    @Override
    public synchronized OperationResult<Complaint> reportRecurrence(
            Actor actor, String previousIssueId, String title, String description,
            String category, String priority, String apartmentUnit, String location, String notes) {
        return mutate(candidate -> {
            Complaint previous = requireIssue(candidate, actor, previousIssueId);
            requireResidentOwnerOrManagement(actor, previous);
            String validNotes = DomainValidation.required(notes, "Recurrence notes", 3, 2000);
            long at = now();
            String reporter = actor.role == CommunityRole.RESIDENT
                    || Complaint.LEGACY_REPORTER_ID.equals(previous.reporterId)
                    ? actor.id : previous.reporterId;
            Complaint created = createComplaintRecord(candidate, actor, reporter, title, description,
                    category, priority, apartmentUnit, location, previous.assetId, at);
            IssueRelation relation = new IssueRelation(candidate.newId("RELATION"),
                    actor.communityId, created.id, previous.id, IssueRelation.Type.RECURRENCE,
                    IssueRelation.ReviewStatus.ACCEPTED, validNotes, actor.id, at, actor.id, at);
            candidate.relations.add(relation);
            RecurrenceEvent recurrence = new RecurrenceEvent(candidate.newId("RECURRENCE"),
                    actor.communityId, previous.id, created.id, previous.assetId, actor.id,
                    validNotes, at);
            candidate.recurrences.add(recurrence);
            candidate.event(actor, ActivityEvent.Type.RECURRENCE_REPORTED, created.id, previous.id,
                    "", previous.assetId, "Recurrence reported",
                    "New issue " + created.id + " linked to prior issue " + previous.id
                            + ". Reason: " + validNotes, at);
            return OperationResult.success(created);
        });
    }

    @Override
    public synchronized OperationResult<List<Complaint>> listIssues(Actor actor) {
        try {
            List<Complaint> result = visibleIssues(actor);
            result.sort((left, right) -> {
                int time = Long.compare(right.createdAtEpochMs, left.createdAtEpochMs);
                return time != 0 ? time : right.id.compareTo(left.id);
            });
            return OperationResult.success(Collections.unmodifiableList(result));
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        } catch (RuntimeException error) {
            return OperationResult.failure(OperationResult.Error.INVALID_INPUT, safeMessage(error));
        }
    }

    @Override
    public synchronized OperationResult<IssueDetails> issueDetails(Actor actor, String issueId) {
        try {
            Complaint issue = requireIssue(state, actor, issueId);
            List<WorkOrder> workOrders = new ArrayList<>();
            List<WorkLog> logs = new ArrayList<>();
            List<EvidenceReference> evidence = new ArrayList<>();
            List<ResolutionReview> reviews = new ArrayList<>();
            List<IssueRelation> relations = new ArrayList<>();
            List<RecurrenceEvent> recurrences = new ArrayList<>();
            List<ActivityEvent> timeline = new ArrayList<>();
            for (WorkOrder value : state.workOrders) if (value.issueId.equals(issue.id)) workOrders.add(value);
            for (WorkLog value : state.workLogs) if (value.issueId.equals(issue.id)) logs.add(value);
            for (EvidenceReference value : state.evidence) {
                if (value.issueId.equals(issue.id) && (actor.role != CommunityRole.RESIDENT
                        || value.accessScope == EvidenceReference.AccessScope.ISSUE_PARTICIPANTS)) {
                    evidence.add(value);
                }
            }
            for (ResolutionReview value : state.resolutionReviews) if (value.issueId.equals(issue.id)) reviews.add(value);
            for (IssueRelation value : state.relations) {
                if (value.sourceIssueId.equals(issue.id) || value.targetIssueId.equals(issue.id)) relations.add(value);
            }
            for (RecurrenceEvent value : state.recurrences) {
                if (value.previousIssueId.equals(issue.id) || value.newIssueId.equals(issue.id)) recurrences.add(value);
            }
            for (ActivityEvent value : state.activities) {
                if ((value.issueId.equals(issue.id) || value.relatedIssueId.equals(issue.id))
                        && canReadEvent(actor, value)) timeline.add(value);
            }
            timeline.sort(Comparator.comparingLong(event -> event.sequence));
            return OperationResult.success(new IssueDetails(issue.copy(), workOrders, logs,
                    evidence, reviews, relations, recurrences, timeline));
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        } catch (RuntimeException error) {
            return OperationResult.failure(OperationResult.Error.INVALID_INPUT, safeMessage(error));
        }
    }

    @Override
    public synchronized OperationResult<List<ActivityEvent>> activityFeed(
            Actor actor, String query, ActivityEvent.Type type, int offset, int limit) {
        try {
            if (offset < 0 || limit < 1 || limit > 200) {
                throw failure(OperationResult.Error.INVALID_INPUT,
                        "Feed offset must be non-negative and limit must be between 1 and 200.");
            }
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.US);
            List<ActivityEvent> visible = new ArrayList<>();
            for (ActivityEvent event : state.activities) {
                if (!canReadEvent(actor, event) || (type != null && event.type != type)) continue;
                String searchable = (event.title + " " + event.detail + " " + event.actorId + " "
                        + event.issueId + " " + event.relatedIssueId + " " + event.assetId).toLowerCase(Locale.US);
                if (!normalizedQuery.isEmpty() && !searchable.contains(normalizedQuery)) continue;
                visible.add(event);
            }
            visible.sort((left, right) -> {
                int time = Long.compare(right.occurredAtEpochMs, left.occurredAtEpochMs);
                return time != 0 ? time : Long.compare(right.sequence, left.sequence);
            });
            List<ActivityEvent> page = new ArrayList<>();
            if (offset < visible.size()) {
                int end = (int) Math.min(visible.size(), (long) offset + limit);
                page.addAll(visible.subList(offset, end));
            }
            return OperationResult.success(Collections.unmodifiableList(page));
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        } catch (RuntimeException error) {
            return OperationResult.failure(OperationResult.Error.INVALID_INPUT, safeMessage(error));
        }
    }

    @Override
    public synchronized OperationResult<List<Asset>> listAssets(Actor actor) {
        try {
            requireRole(actor, CommunityRole.MANAGEMENT, CommunityRole.MAINTENANCE);
            List<Asset> result = new ArrayList<>();
            for (Asset asset : state.assets) if (asset.communityId.equals(actor.communityId)) result.add(asset);
            result.sort(Comparator.comparing(asset -> asset.name.toLowerCase(Locale.US)));
            return OperationResult.success(Collections.unmodifiableList(result));
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        }
    }

    @Override
    public synchronized OperationResult<AssetHistory> assetHistory(Actor actor, String assetId) {
        try {
            requireRole(actor, CommunityRole.MANAGEMENT, CommunityRole.MAINTENANCE);
            Asset asset = state.asset(assetId);
            if (asset == null) throw failure(OperationResult.Error.NOT_FOUND, "Asset not found.");
            requireCommunity(actor, asset.communityId);
            List<Complaint> issues = new ArrayList<>();
            List<WorkLog> logs = new ArrayList<>();
            List<ActivityEvent> events = new ArrayList<>();
            Set<String> issueIds = new HashSet<>();
            for (Complaint issue : state.complaints) {
                if (asset.id.equals(issue.assetId) && issue.communityId.equals(actor.communityId)
                        && canReadIssue(actor, issue, state)) {
                    issues.add(issue.copy());
                    issueIds.add(issue.id);
                }
            }
            for (WorkLog log : state.workLogs) if (issueIds.contains(log.issueId)) logs.add(log);
            for (ActivityEvent event : state.activities) {
                if ((asset.id.equals(event.assetId) || issueIds.contains(event.issueId)
                        || issueIds.contains(event.relatedIssueId)) && canReadEvent(actor, event)) {
                    events.add(event);
                }
            }
            events.sort(Comparator.comparingLong(event -> event.sequence));
            return OperationResult.success(new AssetHistory(asset, issues, logs, events));
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        }
    }

    @Override
    public synchronized OperationResult<ResolutionQuality> resolutionQuality(Actor actor) {
        try {
            requireRole(actor, CommunityRole.RESIDENT, CommunityRole.MANAGEMENT, CommunityRole.MAINTENANCE);
            List<Complaint> issues = visibleIssues(actor);
            List<ActivityEvent> events = new ArrayList<>();
            for (ActivityEvent event : state.activities) if (canReadEvent(actor, event)) events.add(event);
            return OperationResult.success(ResolutionQualityCalculator.calculate(issues, events, now()));
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        }
    }

    /** Legacy Android projection only; callers must not mutate the returned records. */
    public synchronized List<Complaint> exportComplaints() {
        List<Complaint> result = new ArrayList<>();
        for (Complaint issue : state.complaints) result.add(issue.copy());
        return result;
    }

    public synchronized String snapshotJson() {
        return IssueOperationsJson.write(state);
    }

    public synchronized List<String> migrationWarnings() {
        return Collections.unmodifiableList(new ArrayList<>(state.migrationWarnings));
    }

    private <T> OperationResult<T> mutate(Mutation<T> mutation) {
        try {
            IssueOperationsState candidate = IssueOperationsJson.parse(IssueOperationsJson.write(state));
            OperationResult<T> result = mutation.apply(candidate);
            if (!result.ok) return result;
            String serialized = IssueOperationsJson.write(candidate);
            try {
                storage.write(serialized);
            } catch (Exception error) {
                return OperationResult.failure(OperationResult.Error.STORAGE_ERROR,
                        "The change could not be saved. Previous local data is unchanged.");
            }
            state = candidate;
            return result;
        } catch (DomainFailure failure) {
            return OperationResult.failure(failure.error, failure.getMessage());
        } catch (IllegalStateException error) {
            return OperationResult.failure(OperationResult.Error.INVALID_TRANSITION, safeMessage(error));
        } catch (IllegalArgumentException error) {
            return OperationResult.failure(OperationResult.Error.INVALID_INPUT, safeMessage(error));
        } catch (RuntimeException error) {
            return OperationResult.failure(OperationResult.Error.STORAGE_ERROR,
                    "The operation could not be completed safely. Previous local data is unchanged.");
        }
    }

    private Complaint createComplaintRecord(IssueOperationsState candidate, Actor actor,
            String reporterId, String title, String description, String category, String priority,
            String apartmentUnit, String location, String assetId, long at) {
        String id = candidate.newId("ISSUE");
        Complaint issue = new Complaint(id, title, description, category, priority, apartmentUnit,
                displayTime(at), at, displayTime(at), at, "Open",
                Complaint.OutcomeStatus.NOT_PROPOSED, actor.communityId, reporterId,
                assetId, location, 1L);
        candidate.complaints.add(0, issue);
        candidate.event(actor, ActivityEvent.Type.ISSUE_CREATED, issue.id, "", "", assetId,
                "Issue reported", issue.title + " · " + issue.category + " · " + issue.location, at);
        return issue;
    }

    private List<Complaint> visibleIssues(Actor actor) {
        validateActor(actor);
        List<Complaint> result = new ArrayList<>();
        for (Complaint issue : state.complaints) {
            if (issue.communityId.equals(actor.communityId) && canReadIssue(actor, issue, state)) result.add(issue.copy());
        }
        return result;
    }

    private boolean canReadIssue(Actor actor, Complaint issue, IssueOperationsState source) {
        if (!issue.communityId.equals(actor.communityId)) return false;
        if (actor.role == CommunityRole.MANAGEMENT) return true;
        if (actor.role == CommunityRole.RESIDENT) {
            return actor.id.equals(issue.reporterId) || Complaint.LEGACY_REPORTER_ID.equals(issue.reporterId);
        }
        if (actor.role == CommunityRole.MAINTENANCE) {
            for (WorkOrder workOrder : source.workOrders) {
                if (workOrder.issueId.equals(issue.id) && workOrder.assigneeId.equals(actor.id)) return true;
            }
        }
        return false;
    }

    private boolean canReadEvent(Actor actor, ActivityEvent event) {
        if (!event.communityId.equals(actor.communityId)) return false;
        if (!event.issueId.isEmpty()) {
            Complaint issue = state.issue(event.issueId);
            if (issue == null || !canReadIssue(actor, issue, state)) return false;
        } else if (actor.role != CommunityRole.MANAGEMENT && actor.role != CommunityRole.MAINTENANCE) {
            return false;
        }
        if (actor.role == CommunityRole.RESIDENT && event.type == ActivityEvent.Type.EVIDENCE_ADDED) {
            EvidenceReference evidence = state.evidence(event.evidenceId);
            // Events written before evidenceId was introduced are hidden from residents rather
            // than risking disclosure of a staff-only display name. Staff retain full history.
            if (evidence == null || evidence.accessScope == EvidenceReference.AccessScope.STAFF_ONLY) {
                return false;
            }
        }
        return true;
    }

    private Complaint requireIssue(IssueOperationsState candidate, Actor actor, String issueId) {
        validateActor(actor);
        Complaint issue = candidate.issue(issueId);
        if (issue == null || !issue.communityId.equals(actor.communityId)) {
            throw failure(OperationResult.Error.NOT_FOUND, "Issue not found in this community.");
        }
        if (!canReadIssue(actor, issue, candidate)) {
            throw failure(OperationResult.Error.NOT_AUTHORIZED,
                    "Your local demo role cannot access this issue.");
        }
        return issue;
    }

    private WorkOrder requireWorkOrder(IssueOperationsState candidate, Actor actor, String id) {
        validateActor(actor);
        WorkOrder workOrder = candidate.workOrder(id);
        if (workOrder == null) throw failure(OperationResult.Error.NOT_FOUND, "Work order not found.");
        requireCommunity(actor, workOrder.communityId);
        return workOrder;
    }

    private ResolutionReview requireReview(IssueOperationsState candidate, String id) {
        for (ResolutionReview review : candidate.resolutionReviews) if (review.id.equals(id)) return review;
        throw failure(OperationResult.Error.NOT_FOUND, "Resolution review not found.");
    }

    private void requireResidentOwnerOrManagement(Actor actor, Complaint issue) {
        if (actor.role == CommunityRole.MANAGEMENT) return;
        if (actor.role != CommunityRole.RESIDENT || !actor.id.equals(issue.reporterId)) {
            throw failure(OperationResult.Error.NOT_AUTHORIZED,
                    "Only the reporting resident or local Management can perform this action.");
        }
    }

    private void requireWorkActor(Actor actor, WorkOrder workOrder) {
        if (actor.role == CommunityRole.MANAGEMENT) return;
        if (actor.role != CommunityRole.MAINTENANCE || !workOrder.assigneeId.equals(actor.id)) {
            throw failure(OperationResult.Error.NOT_AUTHORIZED,
                    "Only the assigned Maintenance actor or local Management can update this work.");
        }
    }

    private void requireVersion(WorkOrder workOrder, long expectedVersion) {
        if (workOrder.version != expectedVersion) {
            throw failure(OperationResult.Error.CONFLICT,
                    "This work order changed. Reload it before trying again.");
        }
    }

    private void requireCommunity(Actor actor, String communityId) {
        validateActor(actor);
        if (!actor.communityId.equals(communityId)) {
            throw failure(OperationResult.Error.COMMUNITY_MISMATCH,
                    "The record belongs to a different community.");
        }
    }

    private void requireRole(Actor actor, CommunityRole... roles) {
        validateActor(actor);
        for (CommunityRole role : roles) if (actor.role == role) return;
        throw failure(OperationResult.Error.NOT_AUTHORIZED,
                "Your local demo role cannot perform this action.");
    }

    private void validateActor(Actor actor) {
        if (actor == null) throw failure(OperationResult.Error.INVALID_INPUT, "Actor is required.");
    }

    private long now() {
        long value = clock.nowMillis();
        if (value <= 0) throw new IllegalArgumentException("Clock must return a positive UTC epoch value.");
        return value;
    }

    private void setAssetMaintenance(IssueOperationsState candidate, String assetId) {
        updateAssetStatus(candidate, assetId, Asset.Status.UNDER_MAINTENANCE);
    }

    private void setAssetOperational(IssueOperationsState candidate, String assetId) {
        updateAssetStatus(candidate, assetId, Asset.Status.OPERATIONAL);
    }

    private void updateAssetStatus(IssueOperationsState candidate, String assetId, Asset.Status status) {
        if (assetId == null || assetId.isEmpty()) return;
        for (int i = 0; i < candidate.assets.size(); i++) {
            Asset asset = candidate.assets.get(i);
            if (asset.id.equals(assetId) && asset.status != Asset.Status.RETIRED) {
                candidate.assets.set(i, asset.withStatus(status));
                return;
            }
        }
    }

    private static String display(Enum<?> value) {
        return value.name().toLowerCase(Locale.US).replace('_', ' ');
    }

    private static String displayTime(long epochMs) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(epochMs));
    }

    private static DomainFailure failure(OperationResult.Error error, String message) {
        return new DomainFailure(error, message);
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName() : message;
    }
}
