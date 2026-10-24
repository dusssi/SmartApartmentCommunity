package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.model.CommunityRole;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.IssueRelation;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LocalIssueOperationsRepositoryTest {
    private static final String COMMUNITY = "community-test";
    private static final long START = 1_700_000_000_000L;

    private MemoryStorage storage;
    private MutableClock clock;
    private LocalIssueOperationsRepository repository;
    private Actor resident;
    private Actor otherResident;
    private Actor management;
    private Actor maintenance;
    private Actor otherMaintenance;
    private Actor security;

    @Before
    public void setUp() {
        storage = new MemoryStorage();
        clock = new MutableClock();
        resident = new Actor("resident-1", COMMUNITY, CommunityRole.RESIDENT);
        otherResident = new Actor("resident-2", COMMUNITY, CommunityRole.RESIDENT);
        management = new Actor("manager-1", COMMUNITY, CommunityRole.MANAGEMENT);
        maintenance = new Actor("tech-1", COMMUNITY, CommunityRole.MAINTENANCE);
        otherMaintenance = new Actor("tech-2", COMMUNITY, CommunityRole.MAINTENANCE);
        security = new Actor("security-1", COMMUNITY, CommunityRole.SECURITY);
        repository = load(storage, "[]");
    }

    @Test
    public void createComplaint_persistsStableIdUtcAndActivityAcrossReload() {
        Complaint issue = createIssue(repository, resident, "Lift sensor fault");
        assertEquals("ISSUE-000001", issue.id);
        assertEquals(START, issue.createdAtEpochMs);
        assertEquals(1L, issue.version);
        assertEquals(Complaint.OutcomeStatus.NOT_PROPOSED, issue.outcomeStatus);

        IssueDetails details = details(repository, resident, issue.id);
        assertEquals(1, details.timeline.size());
        assertEquals(ActivityEvent.Type.ISSUE_CREATED, details.timeline.get(0).type);
        assertEquals(START, details.timeline.get(0).occurredAtEpochMs);

        LocalIssueOperationsRepository reloaded = load(storage, "[]");
        assertEquals(repository.snapshotJson(), reloaded.snapshotJson());
        assertEquals(issue.id, details(reloaded, resident, issue.id).issue.id);
        assertEquals(2, storage.writes); // initial migration + complaint mutation; reload adds no write
    }

    @Test
    public void createComplaint_rejectsInvalidInputWithoutChangingSnapshot() {
        String before = repository.snapshotJson();
        OperationResult<Complaint> result = repository.createComplaint(resident, "x", "short",
                "E", "Normal", "A-1", "A-1");
        assertFailure(result, OperationResult.Error.INVALID_INPUT);
        assertEquals(before, repository.snapshotJson());
        assertEquals(0, storage.writesAfterInitialLoad());
    }

    @Test
    public void createComplaint_rejectsRolesOutsideTheReportingWorkflow() {
        assertFailure(repository.createComplaint(maintenance, "Valid title", "Valid description",
                "Electrical", "Normal", "A-1", "Corridor"), OperationResult.Error.NOT_AUTHORIZED);
        assertFailure(repository.createComplaint(security, "Valid title", "Valid description",
                "Electrical", "Normal", "A-1", "Corridor"), OperationResult.Error.NOT_AUTHORIZED);
    }

    @Test
    public void residentReadModel_doesNotExposeAnotherResidentsNewIssue() {
        Complaint mine = createIssue(repository, resident, "My corridor light");
        Complaint other = createIssue(repository, otherResident, "Another residents leak");
        List<Complaint> visible = success(repository.listIssues(resident));
        assertEquals(1, visible.size());
        assertEquals(mine.id, visible.get(0).id);
        assertFailure(repository.issueDetails(resident, other.id), OperationResult.Error.NOT_AUTHORIZED);
        assertFailure(repository.issueDetails(otherResident, mine.id), OperationResult.Error.NOT_AUTHORIZED);
        assertEquals(2, success(repository.listIssues(management)).size());
        assertEquals(0, success(repository.listIssues(security)).size());
    }

    @Test
    public void crossCommunityRecordLookup_isDeniedWithoutChangingData() {
        Complaint issue = createIssue(repository, resident, "Community scoped issue");
        Actor outsider = new Actor("outsider-manager", "community-other", CommunityRole.MANAGEMENT);
        String before = repository.snapshotJson();
        assertFailure(repository.issueDetails(outsider, issue.id), OperationResult.Error.NOT_FOUND);
        assertFailure(repository.assignIssue(outsider, issue.id, "outside-tech", ""),
                OperationResult.Error.NOT_FOUND);
        assertEquals(before, repository.snapshotJson());
    }

    @Test
    public void assignment_linksAssetAndCreatesExactlyOneActiveWorkOrder() {
        Complaint issue = createIssue(repository, resident, "Pump making noise");
        Asset asset = createAsset(repository, "Main water pump");
        WorkOrder workOrder = success(repository.assignIssue(management, issue.id, maintenance.id, asset.id));
        assertEquals(WorkOrder.Status.ASSIGNED, workOrder.status);
        assertEquals(asset.id, workOrder.assetId);
        IssueDetails details = details(repository, management, issue.id);
        assertEquals(asset.id, details.issue.assetId);
        assertEquals(2, details.timeline.size());
        assertEquals(ActivityEvent.Type.ISSUE_ASSIGNED, details.timeline.get(1).type);

        String before = repository.snapshotJson();
        assertFailure(repository.assignIssue(management, issue.id, maintenance.id, asset.id),
                OperationResult.Error.CONFLICT);
        assertFailure(repository.assignIssue(maintenance, issue.id, maintenance.id, asset.id),
                OperationResult.Error.NOT_AUTHORIZED);
        assertEquals(before, repository.snapshotJson());
    }

    @Test
    public void workOrder_rejectsStaleVersionAndReservedStatusTransitions() {
        Complaint issue = createIssue(repository, resident, "Garage exhaust fault");
        WorkOrder assigned = assign(repository, issue);
        assertFailure(repository.changeWorkOrderStatus(maintenance, assigned.id, 99,
                WorkOrder.Status.ACCEPTED), OperationResult.Error.CONFLICT);
        assertFailure(repository.changeWorkOrderStatus(maintenance, assigned.id, assigned.version,
                WorkOrder.Status.COMPLETED), OperationResult.Error.INVALID_TRANSITION);
        assertFailure(repository.changeWorkOrderStatus(otherMaintenance, assigned.id, assigned.version,
                WorkOrder.Status.ACCEPTED), OperationResult.Error.NOT_AUTHORIZED);
        WorkOrder accepted = success(repository.changeWorkOrderStatus(maintenance, assigned.id,
                assigned.version, WorkOrder.Status.ACCEPTED));
        assertEquals(WorkOrder.Status.ACCEPTED, accepted.status);
        assertEquals(2L, accepted.version);
    }

    @Test
    public void workLog_startsWorkAppendsTimelineAndUpdatesAssetServiceState() {
        Complaint issue = createIssue(repository, resident, "Asset service history");
        WorkOrder assigned = assign(repository, issue);
        WorkLog log = addWorkLog(repository, assigned, "Replaced the worn drive belt.");
        assertEquals(WorkLog.Type.WORK_PERFORMED, log.type);
        IssueDetails details = details(repository, maintenance, issue.id);
        assertEquals(WorkOrder.Status.IN_PROGRESS, details.workOrders.get(0).status);
        assertEquals("In Progress", details.issue.status);
        assertEquals(1, details.workLogs.size());
        assertEquals(4, details.timeline.size()); // created, assigned, work started, work log
        assertEquals(ActivityEvent.Type.WORK_LOG_ADDED,
                details.timeline.get(details.timeline.size() - 1).type);
        AssetHistory history = success(repository.assetHistory(management, details.issue.assetId));
        assertEquals(Asset.Status.UNDER_MAINTENANCE, history.asset.status);
        assertEquals(1, history.issues.size());
        assertEquals(1, history.workLogs.size());
    }

    @Test
    public void assetHistory_maintenanceSeesOnlyAssignedIssueRecords() {
        Asset shared = createAsset(repository, "Shared pump");
        Complaint mine = createIssue(repository, resident, "Assigned pump repair");
        WorkOrder assigned = success(repository.assignIssue(management, mine.id, maintenance.id, shared.id));
        addWorkLog(repository, assigned, "Replaced the assigned pump coupling.");
        Complaint other = createIssue(repository, otherResident, "Other technician pump repair");
        success(repository.assignIssue(management, other.id, otherMaintenance.id, shared.id));

        AssetHistory history = success(repository.assetHistory(maintenance, shared.id));
        assertEquals(1, history.issues.size());
        assertEquals(mine.id, history.issues.get(0).id);
        assertEquals(1, history.workLogs.size());
        for (ActivityEvent event : history.events) {
            assertNotEquals(other.id, event.issueId);
            assertNotEquals(other.id, event.relatedIssueId);
        }
        AssetHistory otherHistory = success(repository.assetHistory(otherMaintenance, shared.id));
        assertEquals(1, otherHistory.issues.size());
        assertEquals(other.id, otherHistory.issues.get(0).id);
        assertEquals(0, otherHistory.workLogs.size());
    }

    @Test
    public void proposal_requiresAWorkPerformedLog() {
        Complaint issue = createIssue(repository, resident, "Resolution requires evidence");
        WorkOrder assigned = assign(repository, issue);
        WorkLog inspection = success(repository.addWorkLog(maintenance, assigned.id, assigned.version,
                WorkLog.Type.INSPECTION, "Inspected the controller; no repair yet.", "Replace switch", ""));
        WorkOrder current = details(repository, maintenance, issue.id).workOrders.get(0);
        String before = repository.snapshotJson();
        assertFailure(repository.proposeResolution(maintenance, current.id, current.version,
                "Claimed resolution without work"), OperationResult.Error.INVALID_TRANSITION);
        assertEquals(before, repository.snapshotJson());
        assertEquals(WorkLog.Type.INSPECTION, inspection.type);
    }

    @Test
    public void proposedResolution_isNotResidentVerified() {
        Complaint issue = createIssue(repository, resident, "Distinguish proposed outcome");
        ResolutionReview review = propose(repository, issue);
        IssueDetails details = details(repository, resident, issue.id);
        assertEquals("Resolved", details.issue.status);
        assertEquals(Complaint.OutcomeStatus.AWAITING_CONFIRMATION, details.issue.outcomeStatus);
        assertEquals(ResolutionReview.Status.AWAITING_RESIDENT, review.status);
        ResolutionQuality quality = success(repository.resolutionQuality(management));
        assertEquals(1, quality.staffMarkedResolvedCount);
        assertEquals(0, quality.residentVerifiedCount);
        assertEquals(1, quality.awaitingConfirmationCount);
    }

    @Test
    public void onlyReportingResident_canConfirmAndConfirmationCompletesWork() {
        Complaint issue = createIssue(repository, resident, "Resident confirms repair");
        ResolutionReview review = propose(repository, issue);
        String before = repository.snapshotJson();
        assertFailure(repository.reviewResolution(otherResident, review.id, true, ""),
                OperationResult.Error.NOT_AUTHORIZED);
        assertFailure(repository.reviewResolution(management, review.id, true, ""),
                OperationResult.Error.NOT_AUTHORIZED);
        assertEquals(before, repository.snapshotJson());

        ResolutionReview confirmed = success(repository.reviewResolution(resident, review.id, true,
                "The lift is working correctly now."));
        assertEquals(ResolutionReview.Status.CONFIRMED, confirmed.status);
        IssueDetails details = details(repository, resident, issue.id);
        assertEquals(Complaint.OutcomeStatus.VERIFIED, details.issue.outcomeStatus);
        assertEquals(WorkOrder.Status.COMPLETED, details.workOrders.get(0).status);
        ResolutionQuality quality = success(repository.resolutionQuality(management));
        assertEquals(1, quality.staffMarkedResolvedCount);
        assertEquals(1, quality.residentVerifiedCount);
        assertEquals(0, quality.awaitingConfirmationCount);
        assertEquals(Asset.Status.OPERATIONAL,
                success(repository.assetHistory(management, details.issue.assetId)).asset.status);
    }

    @Test
    public void dispute_returnsToWorkAndPreservesOriginalReview() {
        Complaint issue = createIssue(repository, resident, "Disputed pump repair");
        ResolutionReview review = propose(repository, issue);
        assertFailure(repository.reviewResolution(resident, review.id, false, "x"),
                OperationResult.Error.INVALID_INPUT);
        ResolutionReview disputed = success(repository.reviewResolution(resident, review.id, false,
                "The same noise returned within an hour."));
        assertEquals(ResolutionReview.Status.DISPUTED, disputed.status);
        IssueDetails details = details(repository, resident, issue.id);
        assertEquals(Complaint.OutcomeStatus.DISPUTED, details.issue.outcomeStatus);
        assertEquals(WorkOrder.Status.IN_PROGRESS, details.workOrders.get(0).status);
        assertEquals("In Progress", details.issue.status);
        assertEquals(1, details.resolutionReviews.size());
        assertEquals(1, success(repository.resolutionQuality(management)).disputedCount);
    }

    @Test
    public void reopenAwaitingProposal_supersedesReviewWithoutErasingHistory() {
        Complaint issue = createIssue(repository, resident, "Reopen awaiting proposal");
        ResolutionReview review = propose(repository, issue);
        Complaint reopened = success(repository.reopenIssue(resident, issue.id,
                "The socket is still not working."));
        assertEquals(Complaint.OutcomeStatus.REOPENED, reopened.outcomeStatus);
        IssueDetails details = details(repository, resident, issue.id);
        assertEquals(ResolutionReview.Status.SUPERSEDED, details.resolutionReviews.get(0).status);
        assertEquals(WorkOrder.Status.REOPENED, details.workOrders.get(0).status);
        assertEquals(review.id, details.resolutionReviews.get(0).id);
        assertTrue(hasEvent(details.timeline, ActivityEvent.Type.RESOLUTION_PROPOSED));
        assertTrue(hasEvent(details.timeline, ActivityEvent.Type.ISSUE_REOPENED));
        assertEquals(0, success(repository.resolutionQuality(management)).awaitingConfirmationCount);
    }

    @Test
    public void reopenVerified_keepsConfirmedReviewAndAllowsNewWorkLog() {
        Complaint issue = createIssue(repository, resident, "Verified issue reopens");
        ResolutionReview review = propose(repository, issue);
        success(repository.reviewResolution(resident, review.id, true, "Initially fixed."));
        success(repository.reopenIssue(resident, issue.id, "The fault returned tomorrow."));
        IssueDetails details = details(repository, maintenance, issue.id);
        assertEquals(ResolutionReview.Status.CONFIRMED, details.resolutionReviews.get(0).status);
        WorkOrder current = details.workOrders.get(0);
        WorkLog log = success(repository.addWorkLog(maintenance, current.id, current.version,
                WorkLog.Type.WORK_PERFORMED, "Completed a second repair visit.", "Monitor", ""));
        assertEquals(current.id, log.workOrderId);
        assertEquals(2, details(repository, resident, issue.id).workLogs.size());
        ResolutionQuality.Metric reopen = metric(success(repository.resolutionQuality(management)), "reopen_rate");
        assertEquals(100.0, reopen.value, 0.001);
        assertEquals(1, reopen.numerator);
        assertEquals(1, reopen.denominator);
    }

    @Test
    public void recurrence_createsNewIssueAcceptedRelationAndTwoSidedTimeline() {
        Complaint previous = createIssue(repository, resident, "Original ceiling leak");
        ResolutionReview review = propose(repository, previous);
        success(repository.reviewResolution(resident, review.id, true, "Leak stopped."));
        String previousAssetId = details(repository, resident, previous.id).issue.assetId;
        Complaint recurrence = success(repository.reportRecurrence(resident, previous.id,
                "Ceiling leak after rain", "Water appeared again in the same ceiling corner.",
                "Plumbing", "High", "A-101", "A-101 bedroom", "Same location after heavy rain."));
        assertNotEquals(previous.id, recurrence.id);
        assertEquals(previousAssetId, recurrence.assetId);
        assertEquals(Complaint.OutcomeStatus.NOT_PROPOSED, recurrence.outcomeStatus);
        IssueDetails oldDetails = details(repository, resident, previous.id);
        IssueDetails newDetails = details(repository, resident, recurrence.id);
        assertEquals(1, oldDetails.recurrences.size());
        assertEquals(IssueRelation.Type.RECURRENCE, newDetails.relations.get(0).type);
        assertEquals(IssueRelation.ReviewStatus.ACCEPTED, newDetails.relations.get(0).reviewStatus);
        assertTrue(hasEvent(oldDetails.timeline, ActivityEvent.Type.RECURRENCE_REPORTED));
        assertTrue(hasEvent(newDetails.timeline, ActivityEvent.Type.ISSUE_CREATED));
        assertTrue(hasEvent(newDetails.timeline, ActivityEvent.Type.RESOLUTION_CONFIRMED) == false);
        assertEquals(100.0, metric(success(repository.resolutionQuality(management)),
                "recurrence_rate").value, 0.001);
    }

    @Test
    public void evidence_validatesMetadataAndHonorsStaffOnlyVisibility() {
        Complaint issue = createIssue(repository, resident, "Evidence metadata flow");
        WorkOrder assigned = assign(repository, issue);
        WorkLog log = addWorkLog(repository, assigned, "Replaced the damaged sensor.");
        String before = repository.snapshotJson();
        assertFailure(repository.addEvidence(maintenance, log.id, "Technician photo", "Logbook ref 14",
                "application/exe", 1200, EvidenceReference.AccessScope.STAFF_ONLY, 0, "", 0),
                OperationResult.Error.INVALID_INPUT);
        assertFailure(repository.addEvidence(maintenance, log.id, "Technician photo", "Logbook ref 14",
                "image/jpeg", EvidenceReference.MAX_SIZE_BYTES + 1,
                EvidenceReference.AccessScope.STAFF_ONLY, 0, "", 0), OperationResult.Error.INVALID_INPUT);
        assertEquals(before, repository.snapshotJson());

        EvidenceReference evidence = success(repository.addEvidence(maintenance, log.id,
                "Technician photo", "Paper logbook reference 14", "image/jpeg", 2048,
                EvidenceReference.AccessScope.STAFF_ONLY, 0, "Control panel", 0));
        assertEquals(EvidenceReference.StorageState.METADATA_ONLY, evidence.storageState);
        IssueDetails residentDetails = details(repository, resident, issue.id);
        assertEquals(0, residentDetails.evidence.size());
        assertFalse("staff-only evidence events must not leak their display name",
                hasEvent(residentDetails.timeline, ActivityEvent.Type.EVIDENCE_ADDED));
        for (ActivityEvent event : success(repository.activityFeed(resident, "", null, 0, 100))) {
            assertFalse(event.detail.contains("Technician photo"));
        }
        IssueDetails staffDetails = details(repository, maintenance, issue.id);
        assertEquals(1, staffDetails.evidence.size());
        ActivityEvent staffEvent = staffDetails.timeline.get(staffDetails.timeline.size() - 1);
        assertEquals(evidence.id, staffEvent.evidenceId);
        assertTrue(staffEvent.detail.contains("no binary file uploaded"));

        success(repository.addEvidence(maintenance, log.id, "Resident-visible logbook",
                "Shared logbook page 18", "application/pdf", 4096,
                EvidenceReference.AccessScope.ISSUE_PARTICIPANTS, 18, "Repair", 0));
        IssueDetails participantDetails = details(repository, resident, issue.id);
        assertEquals(1, participantDetails.evidence.size());
        int visibleEvidenceEvents = 0;
        for (ActivityEvent event : participantDetails.timeline) {
            if (event.type == ActivityEvent.Type.EVIDENCE_ADDED) visibleEvidenceEvents++;
        }
        assertEquals(1, visibleEvidenceEvents);
    }

    @Test
    public void activityFeed_supportsRoleScopeSearchFilterAndSafePagination() {
        Complaint mine = createIssue(repository, resident, "Searchable elevator problem");
        WorkOrder assigned = assign(repository, mine);
        addWorkLog(repository, assigned, "Adjusted the elevator door sensor.");
        createIssue(repository, otherResident, "Private other resident issue");

        List<ActivityEvent> managementFeed = success(repository.activityFeed(management,
                "elevator", null, 0, 20));
        assertEquals(2, managementFeed.size()); // work log and original create contain elevator
        assertTrue(managementFeed.get(0).sequence > managementFeed.get(1).sequence);
        List<ActivityEvent> residentFeed = success(repository.activityFeed(resident,
                "", null, 0, 20));
        for (ActivityEvent event : residentFeed) assertNotEquals("resident-2", event.actorId);
        List<ActivityEvent> onlyLogs = success(repository.activityFeed(management,
                "", ActivityEvent.Type.WORK_LOG_ADDED, 0, 20));
        assertEquals(1, onlyLogs.size());
        assertEquals(0, success(repository.activityFeed(management, "", null,
                Integer.MAX_VALUE, 20)).size());
        assertFailure(repository.activityFeed(management, "", null, -1, 20),
                OperationResult.Error.INVALID_INPUT);
        assertFailure(repository.activityFeed(management, "", null, 0, 201),
                OperationResult.Error.INVALID_INPUT);
    }

    @Test
    public void failedWrite_returnsStorageErrorAndRollsBackCandidate() {
        Complaint issue = createIssue(repository, resident, "Failed assignment write");
        Asset asset = createAsset(repository, "Write failure asset");
        String before = repository.snapshotJson();
        storage.failWrites = true;
        OperationResult<WorkOrder> result = repository.assignIssue(management, issue.id,
                maintenance.id, asset.id);
        assertFailure(result, OperationResult.Error.STORAGE_ERROR);
        assertEquals(before, repository.snapshotJson());
        assertEquals(0, details(repository, management, issue.id).workOrders.size());
        storage.failWrites = false;
        LocalIssueOperationsRepository reloaded = load(storage, "[]");
        assertEquals(0, details(reloaded, management, issue.id).workOrders.size());
    }

    @Test
    public void corruptOperationsStore_isRejectedAndNeverOverwritten() {
        storage.value = "{ definitely-not-json";
        int writes = storage.writes;
        OperationResult<LocalIssueOperationsRepository> result = LocalIssueOperationsRepository.load(
                storage, "[]", clock);
        assertFailure(result, OperationResult.Error.CORRUPT_DATA);
        assertEquals("{ definitely-not-json", storage.value);
        assertEquals(writes, storage.writes);
    }

    @Test
    public void futureSchema_isRejectedRatherThanDowngraded() {
        storage.value = new JSONObject().put("schemaVersion", 999).toString();
        assertFailure(LocalIssueOperationsRepository.load(storage, "[]", clock),
                OperationResult.Error.CORRUPT_DATA);
        assertTrue(storage.value.contains("999"));
    }

    @Test
    public void legacyMigration_preservesHistoryAndUnknownInstantsAndIsIdempotent() throws Exception {
        storage = new MemoryStorage();
        JSONArray history = new JSONArray().put("Yesterday · Open").put("Today · Resolved");
        JSONObject legacy = new JSONObject().put("id", "C-104").put("title", "Legacy elevator issue")
                .put("description", "Existing complaint with display-only timestamps.")
                .put("category", "Elevator").put("priority", "High").put("apartmentUnit", "C-204")
                .put("createdAt", "Yesterday").put("updatedAt", "Today").put("status", "Resolved")
                .put("history", history);
        String legacyJson = new JSONArray().put(legacy).toString();
        LocalIssueOperationsRepository migrated = load(storage, legacyJson);
        Actor legacyResident = new Actor("resident-1", Complaint.LEGACY_COMMUNITY_ID, CommunityRole.RESIDENT);
        Actor legacyManagement = new Actor("manager-1", Complaint.LEGACY_COMMUNITY_ID, CommunityRole.MANAGEMENT);
        Complaint issue = success(migrated.listIssues(legacyResident)).get(0);
        assertEquals("C-104", issue.id);
        assertEquals(0L, issue.createdAtEpochMs);
        assertEquals(Complaint.LEGACY_REPORTER_ID, issue.reporterId);
        assertEquals(Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED, issue.outcomeStatus);
        assertEquals(2, issue.history.size());
        IssueDetails details = details(migrated, legacyResident, issue.id);
        assertEquals(0L, details.timeline.get(0).occurredAtEpochMs);
        assertNull(metric(success(migrated.resolutionQuality(legacyManagement)),
                "median_first_response_hours").value);
        String snapshot = migrated.snapshotJson();
        int writes = storage.writes;
        LocalIssueOperationsRepository reloaded = load(storage, legacyJson);
        assertEquals(snapshot, reloaded.snapshotJson());
        assertEquals(writes, storage.writes);
        assertEquals(1, details(reloaded, legacyResident, issue.id).timeline.size());
    }

    @Test
    public void legacyUnownedIssue_reopenRequiresManagementAndDeniedWriteIsRolledBack() throws Exception {
        storage = new MemoryStorage();
        JSONObject legacy = new JSONObject().put("id", "C-legacy").put("title", "Legacy resolved issue")
                .put("description", "Ownership is unknown in the old display-only record.")
                .put("category", "Other").put("priority", "Normal").put("apartmentUnit", "A-1")
                .put("createdAt", "Yesterday").put("updatedAt", "Today").put("status", "Resolved")
                .put("history", new JSONArray().put("Today · Resolved"));
        LocalIssueOperationsRepository migrated = load(storage,
                new JSONArray().put(legacy).toString());
        Actor resident = new Actor("resident-1", Complaint.LEGACY_COMMUNITY_ID, CommunityRole.RESIDENT);
        Actor management = new Actor("manager-1", Complaint.LEGACY_COMMUNITY_ID, CommunityRole.MANAGEMENT);
        String before = migrated.snapshotJson();
        assertFailure(migrated.reopenIssue(resident, "C-legacy", "Not my record to reopen"),
                OperationResult.Error.NOT_AUTHORIZED);
        assertEquals(before, migrated.snapshotJson());
        Complaint reopened = success(migrated.reopenIssue(management, "C-legacy",
                "Management reviewed the unowned legacy record."));
        assertEquals(Complaint.OutcomeStatus.REOPENED, reopened.outcomeStatus);
    }

    @Test
    public void malformedLegacyRoot_isNotSilentlyReseeded() {
        storage = new MemoryStorage();
        OperationResult<LocalIssueOperationsRepository> result = LocalIssueOperationsRepository.load(
                storage, "not-valid-json", clock);
        assertFailure(result, OperationResult.Error.CORRUPT_DATA);
        assertEquals("", storage.value);
    }

    @Test
    public void legacyMigration_skipsOnlyInvalidEntryAndReportsWarning() throws Exception {
        storage = new MemoryStorage();
        JSONObject valid = new JSONObject().put("id", "C-1").put("title", "Valid legacy issue")
                .put("description", "Valid existing description").put("category", "Plumbing")
                .put("priority", "Normal").put("apartmentUnit", "A-1")
                .put("createdAt", "Oct 1").put("status", "Open");
        JSONObject invalid = new JSONObject().put("id", "C-2").put("title", "x");
        LocalIssueOperationsRepository migrated = load(storage,
                new JSONArray().put(valid).put(invalid).toString());
        Actor legacyManagement = new Actor("manager-1", Complaint.LEGACY_COMMUNITY_ID, CommunityRole.MANAGEMENT);
        assertEquals(1, success(migrated.listIssues(legacyManagement)).size());
        assertEquals(1, migrated.migrationWarnings().size());
        assertTrue(migrated.migrationWarnings().get(0).contains("index 1"));
    }

    @Test
    public void qualityMetrics_useActualDurationsAndExposeSourcesAndDenominators() {
        Complaint issue = createIssue(repository, resident, "Measured resolution duration");
        clock.advanceHours(2);
        WorkOrder assigned = assign(repository, issue);
        clock.advanceHours(3);
        addWorkLog(repository, assigned, "Completed the measured repair.");
        clock.advanceHours(4);
        WorkOrder current = details(repository, maintenance, issue.id).workOrders.get(0);
        ResolutionReview review = success(repository.proposeResolution(maintenance, current.id,
                current.version, "Repair complete and tested."));
        clock.advanceHours(5);
        success(repository.reviewResolution(resident, review.id, true, "Verified."));
        clock.advanceHours(10);
        ResolutionQuality quality = success(repository.resolutionQuality(management));
        assertEquals(2.0, metric(quality, "median_first_response_hours").value, 0.001);
        assertEquals(9.0, metric(quality, "median_proposed_resolution_hours").value, 0.001);
        assertEquals(14.0, metric(quality, "median_verified_resolution_hours").value, 0.001);
        ResolutionQuality.Metric verified = metric(quality, "median_verified_resolution_hours");
        assertEquals(1, verified.numerator);
        assertEquals(1, verified.denominator);
        assertEquals(issue.id, verified.sourceIssueIds.get(0));
        assertNull(metric(quality, "median_backlog_age_hours").value);
    }

    @Test
    public void initialStorageFailure_isReportedSeparatelyFromCorruption() {
        MemoryStorage fresh = new MemoryStorage();
        fresh.failWrites = true;
        assertFailure(LocalIssueOperationsRepository.load(fresh, "[]", clock),
                OperationResult.Error.STORAGE_ERROR);
        fresh.failWrites = false;
        fresh.failReads = true;
        assertFailure(LocalIssueOperationsRepository.load(fresh, "[]", clock),
                OperationResult.Error.STORAGE_ERROR);
    }

    private Complaint createIssue(LocalIssueOperationsRepository target, Actor actor, String title) {
        return success(target.createComplaint(actor, title,
                "A detailed fictional description for " + title + ".", "Electrical", "Normal",
                "A-101", "A-101 corridor"));
    }

    private Asset createAsset(LocalIssueOperationsRepository target, String name) {
        return success(target.createAsset(management, name, "Electrical equipment", "Basement plant room",
                Asset.Criticality.HIGH, 0L));
    }

    private WorkOrder assign(LocalIssueOperationsRepository target, Complaint issue) {
        Asset asset = createAsset(target, "Asset for " + issue.id);
        return success(target.assignIssue(management, issue.id, maintenance.id, asset.id));
    }

    private WorkLog addWorkLog(LocalIssueOperationsRepository target, WorkOrder workOrder, String details) {
        return success(target.addWorkLog(maintenance, workOrder.id, workOrder.version,
                WorkLog.Type.WORK_PERFORMED, details, "Monitor for 24 hours", "Replacement part"));
    }

    private ResolutionReview propose(LocalIssueOperationsRepository target, Complaint issue) {
        WorkOrder assigned = assign(target, issue);
        addWorkLog(target, assigned, "Completed the repair and performed a safety check.");
        WorkOrder current = details(target, maintenance, issue.id).workOrders.get(0);
        return success(target.proposeResolution(maintenance, current.id, current.version,
                "Repair completed; resident verification is still required."));
    }

    private IssueDetails details(LocalIssueOperationsRepository target, Actor actor, String issueId) {
        return success(target.issueDetails(actor, issueId));
    }

    private LocalIssueOperationsRepository load(MemoryStorage storage, String legacyJson) {
        return success(LocalIssueOperationsRepository.load(storage, legacyJson, clock));
    }

    private static boolean hasEvent(List<ActivityEvent> events, ActivityEvent.Type type) {
        for (ActivityEvent event : events) if (event.type == type) return true;
        return false;
    }

    private static ResolutionQuality.Metric metric(ResolutionQuality quality, String id) {
        for (ResolutionQuality.Metric metric : quality.metrics) if (metric.id.equals(id)) return metric;
        throw new AssertionError("Missing metric " + id);
    }

    private static <T> T success(OperationResult<T> result) {
        assertTrue("Expected success but got " + result.error + ": " + result.message, result.ok);
        return result.value;
    }

    private static void assertFailure(OperationResult<?> result, OperationResult.Error error) {
        assertFalse("Expected failure " + error + " but operation succeeded", result.ok);
        assertEquals(error, result.error);
        assertNull(result.value);
        assertFalse(result.message.trim().isEmpty());
    }

    private static final class MutableClock implements OperationsClock {
        private long now = START;
        @Override public long nowMillis() { return now; }
        void advanceHours(long hours) { now += hours * 3_600_000L; }
    }

    private static final class MemoryStorage implements LocalIssueOperationsRepository.Storage {
        private String value = "";
        private int writes = 0;
        private boolean failWrites;
        private boolean failReads;
        @Override public String read() throws Exception {
            if (failReads) throw new Exception("read failed");
            return value;
        }
        @Override public void write(String next) throws Exception {
            if (failWrites) throw new Exception("write failed");
            value = next;
            writes++;
        }
        int writesAfterInitialLoad() { return writes - 1; }
    }
}
