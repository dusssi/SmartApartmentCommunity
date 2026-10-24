package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.operations.model.ActivityEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Pure deterministic metrics; unknown/incomplete/negative durations are excluded, never zero-filled. */
final class ResolutionQualityCalculator {
    private ResolutionQualityCalculator() { }

    static ResolutionQuality calculate(List<Complaint> issues, List<ActivityEvent> activities,
                                       long now) {
        Set<String> visibleIds = new HashSet<>();
        for (Complaint issue : issues) visibleIds.add(issue.id);
        Set<String> proposedEver = new TreeSet<>();
        Set<String> reopened = new TreeSet<>();
        Set<String> recurrenceSources = new TreeSet<>();
        Map<String, Long> firstResponse = new HashMap<>();
        Map<String, Long> firstProposal = new HashMap<>();
        Map<String, Long> firstVerified = new HashMap<>();
        for (ActivityEvent event : activities) {
            if (event.type == ActivityEvent.Type.RESOLUTION_PROPOSED && visibleIds.contains(event.issueId)) {
                proposedEver.add(event.issueId);
                earliest(firstProposal, event.issueId, event.occurredAtEpochMs);
            }
            if ((event.type == ActivityEvent.Type.ISSUE_ASSIGNED || event.type == ActivityEvent.Type.WORK_LOG_ADDED)
                    && visibleIds.contains(event.issueId)) {
                earliest(firstResponse, event.issueId, event.occurredAtEpochMs);
            }
            if (event.type == ActivityEvent.Type.RESOLUTION_CONFIRMED && visibleIds.contains(event.issueId)) {
                earliest(firstVerified, event.issueId, event.occurredAtEpochMs);
            }
            if (event.type == ActivityEvent.Type.ISSUE_REOPENED && visibleIds.contains(event.issueId)) reopened.add(event.issueId);
            if (event.type == ActivityEvent.Type.RECURRENCE_REPORTED && visibleIds.contains(event.relatedIssueId)) {
                recurrenceSources.add(event.relatedIssueId);
            }
        }

        int staffResolved = 0;
        int verified = 0;
        int awaiting = 0;
        int disputed = 0;
        int legacy = 0;
        List<Complaint> backlog = new ArrayList<>();
        for (Complaint issue : issues) {
            if ("Resolved".equals(issue.status) && proposedEver.contains(issue.id)) staffResolved++;
            if (issue.outcomeStatus == Complaint.OutcomeStatus.VERIFIED) verified++;
            if (issue.outcomeStatus == Complaint.OutcomeStatus.AWAITING_CONFIRMATION) awaiting++;
            if (issue.outcomeStatus == Complaint.OutcomeStatus.DISPUTED) disputed++;
            if (issue.outcomeStatus == Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED) legacy++;
            if (!"Resolved".equals(issue.status)) backlog.add(issue);
        }

        List<ResolutionQuality.Metric> metrics = new ArrayList<>();
        metrics.add(new ResolutionQuality.Metric("open_backlog", "Open backlog",
                "Current issues where staff status is not Resolved", "issues",
                (double) backlog.size(), backlog.size(), issues.size(), idsOf(backlog)));
        List<Double> ages = new ArrayList<>();
        List<String> ageSources = new ArrayList<>();
        for (Complaint issue : backlog) {
            if (issue.createdAtEpochMs > 0 && now >= issue.createdAtEpochMs) {
                ages.add(hours(now - issue.createdAtEpochMs));
                ageSources.add(issue.id);
            }
        }
        metrics.add(new ResolutionQuality.Metric("median_backlog_age_hours", "Median backlog age",
                "Median(now - createdAt) for open issues with known, non-negative instants", "hours",
                median(ages), ageSources.size(), backlog.size(), ageSources));
        addDurationMetric(metrics, "median_first_response_hours", "Median first response",
                "Median(first assignment/work log - issue creation)", issues, firstResponse);
        addDurationMetric(metrics, "median_proposed_resolution_hours", "Median proposed resolution",
                "Median(first resolution proposal - issue creation)", issues, firstProposal);
        addDurationMetric(metrics, "median_verified_resolution_hours", "Median verified resolution",
                "Median(first resident confirmation - issue creation)", issues, firstVerified);
        metrics.add(rateMetric("reopen_rate", "Reopen rate",
                "Unique issues with a reopen event / unique issues with a resolution proposal",
                reopened, proposedEver));
        metrics.add(rateMetric("recurrence_rate", "Recurrence rate",
                "Unique prior issues referenced by recurrence / unique issues with a resolution proposal",
                recurrenceSources, proposedEver));
        return new ResolutionQuality(staffResolved, verified, awaiting, disputed, legacy, metrics);
    }

    private static void addDurationMetric(List<ResolutionQuality.Metric> target, String id,
                                          String label, String formula, List<Complaint> issues,
                                          Map<String, Long> eventTimes) {
        List<Double> durations = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        for (Complaint issue : issues) {
            Long event = eventTimes.get(issue.id);
            if (event != null && issue.createdAtEpochMs > 0 && event >= issue.createdAtEpochMs) {
                durations.add(hours(event - issue.createdAtEpochMs));
                sources.add(issue.id);
            }
        }
        target.add(new ResolutionQuality.Metric(id, label,
                formula + "; unknown, incomplete and negative durations excluded", "hours",
                median(durations), durations.size(), issues.size(), sources));
    }

    private static ResolutionQuality.Metric rateMetric(String id, String label, String formula,
                                                        Set<String> numerators, Set<String> denominator) {
        Set<String> validNumerators = new TreeSet<>(numerators);
        validNumerators.retainAll(denominator);
        Double value = denominator.isEmpty() ? null : 100.0 * validNumerators.size() / denominator.size();
        return new ResolutionQuality.Metric(id, label, formula, "percent", value,
                validNumerators.size(), denominator.size(), new ArrayList<>(validNumerators));
    }

    private static void earliest(Map<String, Long> values, String issueId, long timestamp) {
        if (timestamp <= 0) return;
        Long current = values.get(issueId);
        if (current == null || timestamp < current) values.put(issueId, timestamp);
    }

    private static Double median(List<Double> values) {
        if (values.isEmpty()) return null;
        Collections.sort(values);
        int middle = values.size() / 2;
        return values.size() % 2 == 0
                ? (values.get(middle - 1) + values.get(middle)) / 2.0 : values.get(middle);
    }

    private static double hours(long milliseconds) {
        return milliseconds / 3_600_000.0;
    }

    private static List<String> idsOf(List<Complaint> issues) {
        List<String> ids = new ArrayList<>();
        for (Complaint issue : issues) ids.add(issue.id);
        return ids;
    }
}
