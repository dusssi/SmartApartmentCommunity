package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.IssueRelation;
import com.apartment.community.operations.model.RecurrenceEvent;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Consistent read snapshot for an issue workspace. */
public final class IssueDetails {
    public final Complaint issue;
    public final List<WorkOrder> workOrders;
    public final List<WorkLog> workLogs;
    public final List<EvidenceReference> evidence;
    public final List<ResolutionReview> resolutionReviews;
    public final List<IssueRelation> relations;
    public final List<RecurrenceEvent> recurrences;
    public final List<ActivityEvent> timeline;

    public IssueDetails(Complaint issue, List<WorkOrder> workOrders, List<WorkLog> workLogs,
                        List<EvidenceReference> evidence, List<ResolutionReview> resolutionReviews,
                        List<IssueRelation> relations, List<RecurrenceEvent> recurrences,
                        List<ActivityEvent> timeline) {
        this.issue = issue;
        this.workOrders = immutable(workOrders);
        this.workLogs = immutable(workLogs);
        this.evidence = immutable(evidence);
        this.resolutionReviews = immutable(resolutionReviews);
        this.relations = immutable(relations);
        this.recurrences = immutable(recurrences);
        this.timeline = immutable(timeline);
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
