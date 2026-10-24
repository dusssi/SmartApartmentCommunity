package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.model.CommunityRole;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.IssueRelation;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkOrder;
import com.apartment.community.util.DomainValidation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OperationModelTest {
    @Test
    public void workOrderTransitionMatrix_allowsRecoveryButNotSkippingVerification() {
        WorkOrder assigned = workOrder(WorkOrder.Status.ASSIGNED);
        assertTrue(assigned.status.canTransitionTo(WorkOrder.Status.ACCEPTED));
        assertTrue(assigned.status.canTransitionTo(WorkOrder.Status.IN_PROGRESS));
        assertFalse(assigned.status.canTransitionTo(WorkOrder.Status.COMPLETED));
        assertFalse(assigned.status.canTransitionTo(WorkOrder.Status.AWAITING_CONFIRMATION));
        assertFalse(WorkOrder.Status.COMPLETED.canTransitionTo(WorkOrder.Status.IN_PROGRESS));
        assertTrue(WorkOrder.Status.COMPLETED.canTransitionTo(WorkOrder.Status.REOPENED));
        assertTrue(WorkOrder.Status.REOPENED.canTransitionTo(WorkOrder.Status.IN_PROGRESS));
        assertFalse(WorkOrder.Status.CANCELLED.canTransitionTo(WorkOrder.Status.IN_PROGRESS));
    }

    @Test(expected = IllegalStateException.class)
    public void workOrderTransition_rejectsInvalidChange() {
        workOrder(WorkOrder.Status.ASSIGNED).transition(WorkOrder.Status.COMPLETED, 2000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void workOrderTransition_rejectsBackwardsTime() {
        WorkOrder assigned = workOrder(WorkOrder.Status.ASSIGNED);
        assigned.transition(WorkOrder.Status.ACCEPTED, 900);
    }

    @Test
    public void resolutionReview_separatesProposalAndResidentOutcome() {
        ResolutionReview proposal = review();
        assertEquals(ResolutionReview.Status.AWAITING_RESIDENT, proposal.status);
        ResolutionReview confirmed = proposal.review(true, "resident-1", 2000, "Works");
        assertEquals(ResolutionReview.Status.CONFIRMED, confirmed.status);
        assertEquals("resident-1", confirmed.reviewedBy);
        assertEquals(2000, confirmed.reviewedAtEpochMs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolutionReview_requiresUsefulDisputeReason() {
        review().review(false, "resident-1", 2000, " ");
    }

    @Test(expected = IllegalStateException.class)
    public void resolutionReview_cannotBeReviewedTwice() {
        ResolutionReview confirmed = review().review(true, "resident-1", 2000, "Works");
        confirmed.review(false, "resident-1", 3000, "Changed my mind");
    }

    @Test
    public void evidenceMetadata_enforcesTypeSizeAndPageBounds() {
        EvidenceReference evidence = evidence(1024, 0);
        assertEquals(EvidenceReference.StorageState.METADATA_ONLY, evidence.storageState);
        assertEquals("image/jpeg", evidence.mediaType);
        assertEquals(10L * 1024 * 1024, EvidenceReference.MAX_SIZE_BYTES);
        assertTrue(EvidenceReference.ALLOWED_MEDIA_TYPES.contains("application/pdf"));
        assertFalse(EvidenceReference.ALLOWED_MEDIA_TYPES.contains("application/exe"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void evidenceMetadata_rejectsExcessSize() {
        evidence(EvidenceReference.MAX_SIZE_BYTES + 1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void evidenceMetadata_rejectsNegativePage() {
        evidence(100, -1);
    }

    @Test
    public void acceptedRelation_requiresHumanReviewEvidence() {
        IssueRelation relation = new IssueRelation("REL-1", "community", "ISSUE-NEW", "ISSUE-OLD",
                IssueRelation.Type.RECURRENCE, IssueRelation.ReviewStatus.ACCEPTED,
                "Same asset and resident confirmed recurrence", "resident-1", 1000,
                "resident-1", 1000);
        assertEquals(IssueRelation.ReviewStatus.ACCEPTED, relation.reviewStatus);
        IssueRelation corrected = relation.reviewed(IssueRelation.ReviewStatus.CORRECTED,
                "manager-1", 2000, "Corrected after inspecting both source issues");
        assertEquals("manager-1", corrected.reviewedBy);
        assertEquals(2000, corrected.reviewedAtEpochMs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void relation_rejectsSelfLink() {
        new IssueRelation("REL-1", "community", "ISSUE-1", "ISSUE-1",
                IssueRelation.Type.RELATED, IssueRelation.ReviewStatus.SUGGESTED,
                "Invalid self link", "manager-1", 1000, "", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void acceptedRelation_rejectsMissingReviewer() {
        new IssueRelation("REL-1", "community", "ISSUE-1", "ISSUE-2",
                IssueRelation.Type.RELATED, IssueRelation.ReviewStatus.ACCEPTED,
                "Human reviewed relation", "manager-1", 1000, "", 0);
    }

    @Test
    public void legacyComplaint_preservesSourceCompatibilityAndMarksOutcomeUnknown() {
        Complaint complaint = new Complaint("C-1", "Legacy title", "Legacy description",
                "Plumbing", "Normal", "A-1", "Oct 1", "Resolved");
        assertEquals(Complaint.LEGACY_COMMUNITY_ID, complaint.communityId);
        assertEquals(Complaint.LEGACY_REPORTER_ID, complaint.reporterId);
        assertEquals(0L, complaint.createdAtEpochMs);
        assertEquals(Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED, complaint.outcomeStatus);
        assertEquals(1, complaint.history.size());
    }

    @Test
    public void assetAndActor_validationRejectsMissingScope() {
        Asset asset = new Asset("ASSET-1", "community", "North lift", "Elevator", "Tower A",
                Asset.Criticality.CRITICAL, Asset.Status.OPERATIONAL, 0, 1000);
        assertEquals(0, asset.installedAtEpochMs);
        Actor actor = new Actor("manager", "community", CommunityRole.MANAGEMENT);
        assertEquals("community", actor.communityId);
        assertEquals("trimmed", DomainValidation.required("  trimmed  ", "Value", 1, 20));
    }

    @Test(expected = IllegalArgumentException.class)
    public void actor_rejectsBlankCommunity() {
        new Actor("manager", " ", CommunityRole.MANAGEMENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void domainValidation_rejectsUnsupportedControlCharacters() {
        DomainValidation.required("bad\u0000value", "Value", 1, 30);
    }

    private static WorkOrder workOrder(WorkOrder.Status status) {
        return new WorkOrder("WO-1", "community", "ISSUE-1", "ASSET-1", "tech", "manager",
                status, 1000, 1000, 1);
    }

    private static ResolutionReview review() {
        return new ResolutionReview("REVIEW-1", "community", "ISSUE-1", "WO-1", "tech",
                1000, "Repair complete and tested", ResolutionReview.Status.AWAITING_RESIDENT,
                "", 0, "");
    }

    private static EvidenceReference evidence(long size, int page) {
        return new EvidenceReference("EVIDENCE-1", "community", "ISSUE-1", "LOG-1", "tech",
                "Inspection photo", "Logbook reference 1", "image/jpeg", size,
                EvidenceReference.AccessScope.STAFF_ONLY, EvidenceReference.StorageState.METADATA_ONLY,
                page, "Panel", 0, 1000);
    }
}
