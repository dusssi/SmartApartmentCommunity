package com.apartment.community.feature.operations;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.apartment.community.R;
import com.apartment.community.model.Complaint;
import com.apartment.community.model.CommunityRole;
import com.apartment.community.operations.AssetHistory;
import com.apartment.community.operations.IssueDetails;
import com.apartment.community.operations.OperationResult;
import com.apartment.community.operations.ResolutionQuality;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;
import com.apartment.community.repository.PersistentRepository;
import com.apartment.community.security.AccountStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Native Java/XML feature host for the local Issue-to-Outcome workspace. */
public final class OperationsActivity extends AppCompatActivity {
    public static final String SECTION_ISSUE = "OPS_ISSUE";
    public static final String SECTION_FEED = "OPS_FEED";
    public static final String SECTION_ASSETS = "OPS_ASSETS";
    public static final String SECTION_ASSET_HISTORY = "OPS_ASSET_HISTORY";
    public static final String SECTION_QUALITY = "OPS_QUALITY";

    private static final String EXTRA_SECTION = "operations.section";
    private static final String EXTRA_RECORD_ID = "operations.record_id";
    private static final int REQUEST_CHILD_OPERATIONS = 4103;

    private PersistentRepository repository;
    private FrameLayout root;
    private TextView screenTitle;
    private TextView screenSubtitle;
    private String section;
    private String recordId;
    private String feedQuery = "";
    private ActivityEvent.Type feedType;

    private static final class Field {
        final TextInputLayout layout;
        final TextInputEditText input;
        Field(TextInputLayout layout, TextInputEditText input) {
            this.layout = layout;
            this.input = input;
        }
        String value() { return input.getText() == null ? "" : input.getText().toString().trim(); }
        void error(String message) { layout.setError(message); }
    }

    public static Intent createIntent(Context context, String section, String recordId) {
        Intent intent = new Intent(context, OperationsActivity.class);
        intent.putExtra(EXTRA_SECTION, section);
        intent.putExtra(EXTRA_RECORD_ID, recordId == null ? "" : recordId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operations);
        repository = PersistentRepository.get(this);
        if (!repository.isAuthenticated() || repository.getCurrentUser().role == null) {
            finish();
            return;
        }
        root = findViewById(R.id.operationsContent);
        screenTitle = findViewById(R.id.operationsTitle);
        screenSubtitle = findViewById(R.id.operationsSubtitle);
        findViewById(R.id.operationsBack).setOnClickListener(view -> finish());
        section = getIntent().getStringExtra(EXTRA_SECTION);
        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (section == null) section = SECTION_FEED;
        if (recordId == null) recordId = "";
        if (savedInstanceState != null) {
            feedQuery = savedInstanceState.getString("feed_query", "");
            String type = savedInstanceState.getString("feed_type", "");
            if (!type.isEmpty()) {
                try { feedType = ActivityEvent.Type.valueOf(type); } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) render();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("feed_query", feedQuery);
        outState.putString("feed_type", feedType == null ? "" : feedType.name());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHILD_OPERATIONS && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
        }
    }

    private void render() {
        root.removeAllViews();
        updateHeading();
        if (!repository.isIssueOperationsAvailable()) {
            showError(repository.getIssueOperationsError());
            return;
        }
        switch (section) {
            case SECTION_ISSUE:
                showIssue();
                break;
            case SECTION_ASSETS:
                showAssets();
                break;
            case SECTION_ASSET_HISTORY:
                showAssetHistory();
                break;
            case SECTION_QUALITY:
                showQuality();
                break;
            case SECTION_FEED:
            default:
                showFeed();
                break;
        }
    }

    private void updateHeading() {
        switch (section) {
            case SECTION_ISSUE:
                screenTitle.setText("Issue resolution");
                screenSubtitle.setText("Work, evidence and resident outcome");
                break;
            case SECTION_ASSETS:
                screenTitle.setText("Asset reliability");
                screenSubtitle.setText("Community assets and service history");
                break;
            case SECTION_ASSET_HISTORY:
                screenTitle.setText("Asset service history");
                screenSubtitle.setText("Linked incidents and repairs");
                break;
            case SECTION_QUALITY:
                screenTitle.setText("Resolution quality");
                screenSubtitle.setText("Proposed versus resident-verified outcomes");
                break;
            case SECTION_FEED:
            default:
                screenTitle.setText("Community activity");
                screenSubtitle.setText("Role-scoped operational event history");
                break;
        }
    }

    private void showIssue() {
        OperationResult<IssueDetails> result = repository.getIssueDetails(recordId);
        if (!result.ok) { showError(result.message); return; }
        IssueDetails details = result.value;
        LinearLayout content = content();
        addPill(content, details.issue.status + " · " + details.issue.outcomeStatus.displayLabel());
        addText(content, details.issue.title, 26, R.color.operations_navy, true, 12);
        addInfo(content, "ISSUE " + details.issue.id,
                details.issue.category + " · " + details.issue.priority + " priority\n"
                        + details.issue.location + "\nReported " + displayTime(details.issue.createdAtEpochMs,
                        details.issue.createdAt));
        addInfo(content, "DESCRIPTION", details.issue.description);
        MaterialCardView outcome = infoCard("OUTCOME — NOT JUST A STATUS",
                details.issue.outcomeStatus.displayLabel() + "\nA staff proposal is not resident verification. "
                        + "Every review, dispute and reopen remains in the timeline below.");
        outcome.setCardBackgroundColor(color(R.color.operations_outcome_surface));
        addTopMargin(content, outcome, 10);
        if (repository.getCurrentUser().role == CommunityRole.RESIDENT
                && Complaint.LEGACY_REPORTER_ID.equals(details.issue.reporterId)) {
            addInfo(content, "LEGACY UNOWNED RECORD",
                    "Ownership cannot be inferred safely. Management must reopen this record or link a recurrence; "
                            + "another resident cannot mutate it.");
        }
        if (!details.issue.assetId.isEmpty()
                && repository.getCurrentUser().role != CommunityRole.RESIDENT) {
            MaterialButton asset = button("View linked asset history", false);
            asset.setOnClickListener(view -> openChild(SECTION_ASSET_HISTORY, details.issue.assetId));
            addTopMargin(content, asset, 10);
        }
        addIssueActions(content, details);
        addHeader(content, "Resolution timeline", 24);
        addText(content, details.timeline.size() + " event(s) · Oldest first · Source IDs preserved",
                12, R.color.text_secondary, false, 2);
        if (details.timeline.isEmpty()) {
            addInfo(content, "NO TIMELINE EVENTS", "No operational events are available for this issue.");
        } else {
            for (ActivityEvent event : details.timeline) addTimeline(content, event);
        }
    }

    private void addIssueActions(LinearLayout content, IssueDetails details) {
        CommunityRole role = repository.getCurrentUser().role;
        WorkOrder active = activeWorkOrder(details);
        ResolutionReview awaiting = awaitingReview(details);
        List<View> actions = new ArrayList<>();
        if (role == CommunityRole.MANAGEMENT) {
            if (active == null && !"Resolved".equals(details.issue.status)) {
                MaterialButton assign = button("Assign work", true);
                assign.setOnClickListener(view -> showAssignDialog(details.issue));
                actions.add(assign);
            }
            if (active != null) addWorkActions(actions, details, active);
            addResidentHistoryActions(actions, details);
        } else if (role == CommunityRole.MAINTENANCE && active != null) {
            addWorkActions(actions, details, active);
        } else if (role == CommunityRole.RESIDENT) {
            if (awaiting != null) {
                MaterialButton confirm = button("Confirm resolution", true);
                confirm.setOnClickListener(view -> showReviewDialog(awaiting, true));
                actions.add(confirm);
                MaterialButton dispute = button("Dispute resolution", false);
                dispute.setOnClickListener(view -> showReviewDialog(awaiting, false));
                actions.add(dispute);
            }
            if (repository.getCurrentUser().id.equals(details.issue.reporterId)) {
                addResidentHistoryActions(actions, details);
            }
        }
        if (!actions.isEmpty()) {
            addHeader(content, "Available actions", 22);
            for (View action : actions) addTopMargin(content, action, 9);
        }
    }

    private void addResidentHistoryActions(List<View> actions, IssueDetails details) {
        Complaint.OutcomeStatus outcome = details.issue.outcomeStatus;
        if (outcome == Complaint.OutcomeStatus.AWAITING_CONFIRMATION
                || outcome == Complaint.OutcomeStatus.VERIFIED
                || outcome == Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED) {
            MaterialButton reopen = button("Reopen issue", false);
            reopen.setOnClickListener(view -> showReopenDialog(details.issue));
            actions.add(reopen);
        }
        MaterialButton recurrence = button("Report recurrence", false);
        recurrence.setOnClickListener(view -> showRecurrenceDialog(details.issue));
        actions.add(recurrence);
    }

    private void addWorkActions(List<View> actions, IssueDetails details, WorkOrder workOrder) {
        if (workOrder.status == WorkOrder.Status.ASSIGNED) {
            MaterialButton accept = button("Accept assigned work", true);
            accept.setOnClickListener(view -> changeWorkStatus(workOrder, WorkOrder.Status.ACCEPTED));
            actions.add(accept);
        } else if (workOrder.status == WorkOrder.Status.ACCEPTED
                || workOrder.status == WorkOrder.Status.BLOCKED
                || workOrder.status == WorkOrder.Status.REOPENED) {
            MaterialButton start = button("Start / resume work", true);
            start.setOnClickListener(view -> changeWorkStatus(workOrder, WorkOrder.Status.IN_PROGRESS));
            actions.add(start);
        } else if (workOrder.status == WorkOrder.Status.IN_PROGRESS) {
            MaterialButton block = button("Mark work blocked", false);
            block.setOnClickListener(view -> changeWorkStatus(workOrder, WorkOrder.Status.BLOCKED));
            actions.add(block);
        }
        if (canLog(workOrder.status)) {
            MaterialButton log = button("Add inspection / work log", true);
            log.setOnClickListener(view -> showWorkLogDialog(workOrder));
            actions.add(log);
        }
        if (!details.workLogs.isEmpty() && workOrder.status != WorkOrder.Status.COMPLETED
                && workOrder.status != WorkOrder.Status.CANCELLED) {
            MaterialButton evidence = button("Add evidence metadata", false);
            evidence.setOnClickListener(view -> showEvidenceDialog(details, workOrder));
            actions.add(evidence);
        }
        if (workOrder.status == WorkOrder.Status.IN_PROGRESS) {
            MaterialButton propose = button("Propose resolution", true);
            propose.setOnClickListener(view -> showProposalDialog(workOrder));
            actions.add(propose);
        }
    }

    private boolean canLog(WorkOrder.Status status) {
        return status == WorkOrder.Status.ASSIGNED || status == WorkOrder.Status.ACCEPTED
                || status == WorkOrder.Status.IN_PROGRESS || status == WorkOrder.Status.BLOCKED
                || status == WorkOrder.Status.REOPENED;
    }

    private void showFeed() {
        LinearLayout content = content();
        Field search = field("Search activity", "Issue ID, actor, asset or event detail", false);
        search.input.setText(feedQuery);
        search.input.setInputType(InputType.TYPE_CLASS_TEXT);
        addTopMargin(content, search.layout, 12);
        List<String> filterLabels = new ArrayList<>();
        final List<ActivityEvent.Type> filterTypes = new ArrayList<>();
        filterLabels.add("All event types");
        filterTypes.add(null);
        for (ActivityEvent.Type type : ActivityEvent.Type.values()) {
            filterLabels.add(pretty(type.name()));
            filterTypes.add(type);
        }
        Spinner filter = spinner(content, "Event type", filterLabels);
        int selected = filterTypes.indexOf(feedType);
        filter.setSelection(Math.max(0, selected));
        MaterialButton apply = button("Apply activity filters", true);
        addTopMargin(content, apply, 10);
        MaterialButton clear = button("Clear filters", false);
        addTopMargin(content, clear, 8);
        apply.setOnClickListener(view -> {
            feedQuery = search.value();
            feedType = filterTypes.get(filter.getSelectedItemPosition());
            render();
        });
        clear.setOnClickListener(view -> {
            feedQuery = "";
            feedType = null;
            render();
        });

        OperationResult<List<ActivityEvent>> result = repository.getActivityFeed(feedQuery, feedType, 0, 200);
        if (!result.ok) { addInfo(content, "ACTIVITY UNAVAILABLE", result.message); return; }
        addHeader(content, result.value.size() + " event(s)", 20);
        if (result.value.size() == 200) {
            addText(content, "Showing the first 200 matching events. Refine the filters to narrow the result.",
                    12, R.color.text_secondary, false, 2);
        }
        if (result.value.isEmpty()) {
            addInfo(content, "NO MATCHING ACTIVITY",
                    "No role-visible events match these filters. Clear search or choose another event type.");
            return;
        }
        for (ActivityEvent event : result.value) {
                View timeline = addTimeline(content, event);
                if (!event.issueId.isEmpty()) {
                    timeline.setContentDescription(event.title + ". " + event.detail + ". Double tap to open issue.");
                    timeline.setOnClickListener(view -> openChild(SECTION_ISSUE, event.issueId));
                } else if (!event.assetId.isEmpty()) {
                    timeline.setOnClickListener(view -> openChild(SECTION_ASSET_HISTORY, event.assetId));
                }
        }
    }

    private void showAssets() {
        LinearLayout content = content();
        if (repository.getCurrentUser().role == CommunityRole.MANAGEMENT) {
            MaterialButton add = button("Add community asset", true);
            addTopMargin(content, add, 12);
            add.setOnClickListener(view -> showAssetDialog());
        }
        OperationResult<List<Asset>> result = repository.getOperationalAssets();
        if (!result.ok) { addInfo(content, "ASSETS UNAVAILABLE", result.message); return; }
        addHeader(content, "Asset register", 20);
        if (result.value.isEmpty()) {
            addInfo(content, "NO ASSETS YET",
                    repository.getCurrentUser().role == CommunityRole.MANAGEMENT
                            ? "Add the first fictional community asset, then link it during work assignment."
                            : "No community assets are registered yet. Management can add them.");
            return;
        }
        for (Asset asset : result.value) {
            MaterialCardView card = infoCard(asset.name,
                    asset.type + " · " + asset.location + "\n" + pretty(asset.criticality.name())
                            + " criticality · " + pretty(asset.status.name()));
            card.setContentDescription(asset.name + ". " + asset.type + ". Double tap for service history.");
            card.setOnClickListener(view -> openChild(SECTION_ASSET_HISTORY, asset.id));
            addTopMargin(content, card, 9);
        }
    }

    private void showAssetHistory() {
        OperationResult<AssetHistory> result = repository.getAssetHistory(recordId);
        if (!result.ok) { showError(result.message); return; }
        AssetHistory history = result.value;
        LinearLayout content = content();
        addText(content, history.asset.name, 26, R.color.operations_navy, true, 12);
        addInfo(content, "ASSET " + history.asset.id,
                history.asset.type + " · " + history.asset.location + "\n"
                        + pretty(history.asset.criticality.name()) + " criticality · "
                        + pretty(history.asset.status.name()) + "\n"
                        + history.issues.size() + " linked issue(s) · " + history.workLogs.size() + " work log(s)");
        addHeader(content, "Incidents and service events", 22);
        if (history.events.isEmpty()) {
            addInfo(content, "NO SERVICE HISTORY", "This asset has no linked incidents or maintenance events yet.");
            return;
        }
        for (ActivityEvent event : history.events) {
            View item = addTimeline(content, event);
            if (!event.issueId.isEmpty()) item.setOnClickListener(view -> openChild(SECTION_ISSUE, event.issueId));
        }
    }

    private void showQuality() {
        OperationResult<ResolutionQuality> result = repository.getResolutionQuality();
        if (!result.ok) { showError(result.message); return; }
        ResolutionQuality quality = result.value;
        LinearLayout content = content();
        addHeader(content, "Outcome verification", 12);
        addMetric(content, "Staff-marked resolved", String.valueOf(quality.staffMarkedResolvedCount),
                "Current resolved issues with a recorded staff resolution proposal.", null);
        addMetric(content, "Resident verified", String.valueOf(quality.residentVerifiedCount),
                "Current issues explicitly confirmed by the reporting resident.", null);
        addMetric(content, "Awaiting / disputed", quality.awaitingConfirmationCount + " / " + quality.disputedCount,
                "Awaiting confirmation remains unresolved from the resident's perspective.", null);
        if (quality.legacyUnverifiedCount > 0) {
            addInfo(content, "LEGACY DATA QUALITY", quality.legacyUnverifiedCount
                    + " legacy resolved issue(s) have no resolution proposal or resident verification. "
                    + "They are not counted as verified outcomes.");
        }
        addHeader(content, "Transparent operational metrics", 24);
        addText(content, "Median durations use known UTC instants only. No value (—) means no eligible sample; "
                + "it does not mean zero.", 13, R.color.text_secondary, false, 2);
        for (ResolutionQuality.Metric metric : quality.metrics) {
            addMetric(content, metric.label, metricValue(metric), metric.formula + "\nSample / denominator: "
                    + metric.numerator + " / " + metric.denominator, metric);
        }
    }

    private void addMetric(LinearLayout content, String label, String value, String detail,
                           @Nullable ResolutionQuality.Metric metric) {
        View card = LayoutInflater.from(this).inflate(R.layout.view_operations_metric, content, false);
        ((TextView) card.findViewById(R.id.metricLabel)).setText(label);
        ((TextView) card.findViewById(R.id.metricValue)).setText(value);
        ((TextView) card.findViewById(R.id.metricDetail)).setText(detail);
        addTopMargin(content, card, 9);
        if (metric != null && !metric.sourceIssueIds.isEmpty()) {
            MaterialButton sources = button("View source issues (" + metric.sourceIssueIds.size() + ")", false);
            sources.setOnClickListener(view -> showSourceIssues(metric));
            addTopMargin(content, sources, 6);
        }
    }

    private void showSourceIssues(ResolutionQuality.Metric metric) {
        final String[] ids = metric.sourceIssueIds.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(metric.label + " · source records")
                .setItems(ids, (dialog, which) -> openChild(SECTION_ISSUE, ids[which]))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAssignDialog(Complaint issue) {
        List<AccountStore.Account> assignees = repository.listMaintenanceAssignees();
        if (assignees.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Maintenance assignee available")
                    .setMessage("Create or grant a local Maintenance account first. Assignment is not simulated "
                            + "and no hardcoded technician will be used.")
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }
        OperationResult<List<Asset>> assetResult = repository.getOperationalAssets();
        if (!assetResult.ok) { showError(assetResult.message); return; }
        LinearLayout form = dialogContent();
        List<String> people = new ArrayList<>();
        for (AccountStore.Account account : assignees) people.add(account.name + " · " + account.email);
        Spinner assignee = spinner(form, "Maintenance assignee", people);
        List<String> assetLabels = new ArrayList<>();
        assetLabels.add("No linked asset");
        for (Asset asset : assetResult.value) assetLabels.add(asset.name + " · " + asset.location);
        Spinner asset = spinner(form, "Optional asset", assetLabels);
        int assetIndex = 0;
        for (int i = 0; i < assetResult.value.size(); i++) {
            if (assetResult.value.get(i).id.equals(issue.assetId)) assetIndex = i + 1;
        }
        asset.setSelection(assetIndex);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Assign " + issue.id)
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Assign work", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            AccountStore.Account selected = assignees.get(assignee.getSelectedItemPosition());
            String assetId = asset.getSelectedItemPosition() == 0 ? ""
                    : assetResult.value.get(asset.getSelectedItemPosition() - 1).id;
            complete(dialog, repository.assignIssue(issue.id, selected.id, assetId), "Work assigned");
        }));
        dialog.show();
    }

    private void showWorkLogDialog(WorkOrder workOrder) {
        LinearLayout form = dialogContent();
        List<String> types = new ArrayList<>();
        final WorkLog.Type[] values = WorkLog.Type.values();
        for (WorkLog.Type type : values) types.add(pretty(type.name()));
        Spinner type = spinner(form, "Log type", types);
        type.setSelection(1); // Work performed, while inspection remains explicitly available.
        Field details = addField(form, "Inspection / work details", "What was observed or performed?", true);
        Field next = addField(form, "Next step (optional)", "Follow-up, monitoring or blocker", false);
        Field materials = addField(form, "Materials (optional)", "Known parts or materials", false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add work log")
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Save work log", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!require(details, 3)) return;
            complete(dialog, repository.addOperationalWorkLog(workOrder.id, workOrder.version,
                    values[type.getSelectedItemPosition()], details.value(), next.value(), materials.value()),
                    "Work log saved");
        }));
        dialog.show();
    }

    private void showEvidenceDialog(IssueDetails details, WorkOrder workOrder) {
        LinearLayout form = dialogContent();
        addText(form, "Metadata only — no file is uploaded or securely stored. Use a truthful local reference.",
                13, R.color.text_secondary, false, 2);
        List<String> logLabels = new ArrayList<>();
        for (WorkLog log : details.workLogs) logLabels.add(pretty(log.type.name()) + " · " + log.id);
        Spinner log = spinner(form, "Related work log", logLabels);
        Field name = addField(form, "Evidence name", "For example, control-panel photo", false);
        Field reference = addField(form, "Local reference", "Logbook/page/reference, not a download URL", false);
        List<String> media = new ArrayList<>();
        media.add("image/jpeg"); media.add("image/png"); media.add("application/pdf"); media.add("text/plain");
        Spinner mediaType = spinner(form, "Media type", media);
        Field size = addField(form, "Declared size in bytes", "1 to 10485760", false);
        size.input.setInputType(InputType.TYPE_CLASS_NUMBER);
        List<String> scopes = new ArrayList<>();
        scopes.add("Issue participants"); scopes.add("Staff only");
        Spinner scope = spinner(form, "Access scope", scopes);
        Field page = addField(form, "Page (optional)", "0 when not applicable", false);
        page.input.setInputType(InputType.TYPE_CLASS_NUMBER);
        page.input.setText("0");
        Field sectionName = addField(form, "Section (optional)", "Document or logbook section", false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add evidence metadata")
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Save metadata", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!require(name, 2) || !require(reference, 1)) return;
            Long bytes = positiveLong(size, "Enter a valid size up to 10 MiB.");
            if (bytes == null) return;
            Integer pageNumber = nonNegativeInt(page);
            if (pageNumber == null) return;
            WorkLog selected = details.workLogs.get(log.getSelectedItemPosition());
            complete(dialog, repository.addOperationalEvidence(selected.id, name.value(), reference.value(),
                    media.get(mediaType.getSelectedItemPosition()), bytes,
                    scope.getSelectedItemPosition() == 0 ? EvidenceReference.AccessScope.ISSUE_PARTICIPANTS
                            : EvidenceReference.AccessScope.STAFF_ONLY,
                    pageNumber, sectionName.value(), 0L), "Evidence metadata saved");
        }));
        dialog.show();
    }

    private void showProposalDialog(WorkOrder workOrder) {
        LinearLayout form = dialogContent();
        Field summary = addField(form, "Proposed resolution", "Work completed and checks performed", true);
        addText(form, "This marks staff work as resolved, but the outcome remains awaiting resident confirmation.",
                13, R.color.text_secondary, false, 4);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Propose resolution")
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Propose resolution", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!require(summary, 3)) return;
            complete(dialog, repository.proposeOperationalResolution(workOrder.id, workOrder.version,
                    summary.value()), "Resolution proposed; resident confirmation pending");
        }));
        dialog.show();
    }

    private void showReviewDialog(ResolutionReview review, boolean confirmed) {
        LinearLayout form = dialogContent();
        addText(form, "Proposal: " + review.summary, 14, R.color.text_primary, false, 2);
        Field note = addField(form, confirmed ? "Confirmation note (optional)" : "What is still wrong?",
                confirmed ? "Optional resident confirmation note" : "Describe the continuing or repeated problem", true);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(confirmed ? "Confirm resolution" : "Dispute resolution")
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(confirmed ? "Confirm outcome" : "Submit dispute", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!confirmed && !require(note, 3)) return;
            complete(dialog, repository.reviewOperationalResolution(review.id, confirmed, note.value()),
                    confirmed ? "Resident-verified outcome saved" : "Dispute saved; work reopened");
        }));
        dialog.show();
    }

    private void showReopenDialog(Complaint issue) {
        LinearLayout form = dialogContent();
        Field reason = addField(form, "Reason for reopening", "What remains or has happened again?", true);
        addText(form, "Previous work and reviews will remain in history.", 13, R.color.text_secondary, false, 4);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reopen " + issue.id)
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Reopen issue", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!require(reason, 3)) return;
            complete(dialog, repository.reopenOperationalIssue(issue.id, reason.value()), "Issue reopened");
        }));
        dialog.show();
    }

    private void showRecurrenceDialog(Complaint previous) {
        LinearLayout form = dialogContent();
        Field title = addField(form, "New issue title", "Describe the new incident", false);
        title.input.setText("Recurring: " + previous.title);
        Field description = addField(form, "New incident description", "What is happening now?", true);
        List<String> categories = new ArrayList<>();
        categories.add(previous.category);
        for (String value : new String[]{"Plumbing", "Electrical", "Elevator", "Facilities", "Cleaning", "Other"}) {
            if (!categories.contains(value)) categories.add(value);
        }
        Spinner category = spinner(form, "Category", categories);
        List<String> priorities = new ArrayList<>();
        priorities.add("Normal"); priorities.add("High"); priorities.add("Low");
        Spinner priority = spinner(form, "Priority", priorities);
        Field unit = addField(form, "Apartment / area", "Apartment or common area", false);
        unit.input.setText(previous.apartmentUnit);
        Field location = addField(form, "Location", "Where is the new incident?", false);
        location.input.setText(previous.location);
        Field notes = addField(form, "Why is this a recurrence?", "Link evidence to the prior incident", true);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Report recurrence of " + previous.id)
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Create linked issue", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!require(title, 3) || !require(description, 8) || !require(unit, 1)
                    || !require(location, 1) || !require(notes, 3)) return;
            complete(dialog, repository.reportOperationalRecurrence(previous.id, title.value(), description.value(),
                    category.getSelectedItem().toString(), priority.getSelectedItem().toString(), unit.value(),
                    location.value(), notes.value()), "Recurring issue created and linked");
        }));
        dialog.show();
    }

    private void showAssetDialog() {
        LinearLayout form = dialogContent();
        Field name = addField(form, "Asset name", "For example, North tower lift", false);
        Field type = addField(form, "Asset type", "Elevator, pump, electrical panel…", false);
        Field location = addField(form, "Location", "Tower, floor or plant room", false);
        List<String> criticalities = new ArrayList<>();
        final Asset.Criticality[] values = Asset.Criticality.values();
        for (Asset.Criticality criticality : values) criticalities.add(pretty(criticality.name()));
        Spinner criticality = spinner(form, "Criticality", criticalities);
        criticality.setSelection(1);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add fictional community asset")
                .setView(wrapDialog(form))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Add asset", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!require(name, 2) || !require(type, 2) || !require(location, 2)) return;
            complete(dialog, repository.addOperationalAsset(name.value(), type.value(), location.value(),
                    values[criticality.getSelectedItemPosition()], 0L), "Asset added");
        }));
        dialog.show();
    }

    private void changeWorkStatus(WorkOrder workOrder, WorkOrder.Status target) {
        OperationResult<WorkOrder> result = repository.changeWorkOrderStatus(workOrder.id,
                workOrder.version, target);
        if (!result.ok) {
            new AlertDialog.Builder(this).setTitle("Status not changed").setMessage(result.message)
                    .setPositiveButton("Close", null).show();
            return;
        }
        setResult(RESULT_OK);
        Toast.makeText(this, "Work status updated", Toast.LENGTH_SHORT).show();
        render();
    }

    private void complete(AlertDialog dialog, OperationResult<?> result, String successMessage) {
        if (!result.ok) {
            TextView error = dialog.findViewById(R.id.operations_form_error);
            if (error != null) {
                error.setText(result.message);
                error.setVisibility(View.VISIBLE);
                error.announceForAccessibility(result.message);
            }
            return;
        }
        dialog.dismiss();
        setResult(RESULT_OK);
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
        render();
    }

    private boolean require(Field field, int minLength) {
        if (field.value().length() < minLength) {
            field.error("Enter at least " + minLength + " characters.");
            field.input.requestFocus();
            return false;
        }
        field.error(null);
        return true;
    }

    private Long positiveLong(Field field, String error) {
        try {
            long value = Long.parseLong(field.value());
            if (value <= 0 || value > EvidenceReference.MAX_SIZE_BYTES) throw new NumberFormatException();
            field.error(null);
            return value;
        } catch (NumberFormatException exception) {
            field.error(error);
            field.input.requestFocus();
            return null;
        }
    }

    private Integer nonNegativeInt(Field field) {
        try {
            int value = Integer.parseInt(field.value().isEmpty() ? "0" : field.value());
            if (value < 0) throw new NumberFormatException();
            field.error(null);
            return value;
        } catch (NumberFormatException exception) {
            field.error("Enter zero or a positive page number.");
            field.input.requestFocus();
            return null;
        }
    }

    private WorkOrder activeWorkOrder(IssueDetails details) {
        WorkOrder result = null;
        for (WorkOrder workOrder : details.workOrders) if (workOrder.isActive()) result = workOrder;
        return result;
    }

    private ResolutionReview awaitingReview(IssueDetails details) {
        for (ResolutionReview review : details.resolutionReviews) {
            if (review.status == ResolutionReview.Status.AWAITING_RESIDENT) return review;
        }
        return null;
    }

    private void openChild(String nextSection, String id) {
        startActivityForResult(createIntent(this, nextSection, id), REQUEST_CHILD_OPERATIONS);
    }

    private String metricValue(ResolutionQuality.Metric metric) {
        if (!metric.hasValue()) return "—";
        if ("issues".equals(metric.unit)) return String.valueOf(metric.value.intValue());
        if ("percent".equals(metric.unit)) return String.format(Locale.getDefault(), "%.1f%%", metric.value);
        return String.format(Locale.getDefault(), "%.1f h", metric.value);
    }

    private View addTimeline(LinearLayout content, ActivityEvent event) {
        View item = LayoutInflater.from(this).inflate(R.layout.view_operations_timeline_item, content, false);
        ((TextView) item.findViewById(R.id.timelineTitle)).setText(event.title);
        ((TextView) item.findViewById(R.id.timelineMetadata)).setText(
                pretty(event.actorRole.name()) + " · " + event.actorId + " · "
                        + displayTime(event.occurredAtEpochMs, "Legacy · time unavailable")
                        + "\n" + event.type.name() + " · " + event.id);
        TextView detail = item.findViewById(R.id.timelineDetail);
        detail.setText(event.detail);
        detail.setVisibility(event.detail.isEmpty() ? View.GONE : View.VISIBLE);
        addTopMargin(content, item, 8);
        return item;
    }

    private String displayTime(long epochMs, String unknown) {
        if (epochMs <= 0) return unknown;
        SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy · h:mm a z", Locale.getDefault());
        return format.format(new Date(epochMs));
    }

    private LinearLayout content() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(14), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        View banner = LayoutInflater.from(this).inflate(R.layout.view_operations_banner, content, false);
        content.addView(banner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!repository.getIssueMigrationWarnings().isEmpty()) {
            addInfo(content, "MIGRATION NOTICE", repository.getIssueMigrationWarnings().size()
                    + " legacy record(s) need review. Original complaint JSON remains stored; "
                    + "valid records and history were preserved.");
        }
        return content;
    }

    private void showError(String message) {
        root.removeAllViews();
        LinearLayout content = content();
        addInfo(content, "LOCAL DATA UNAVAILABLE", message == null || message.trim().isEmpty()
                ? "Issue operations could not be loaded. Existing data was not replaced." : message);
        MaterialButton retry = button("Retry local storage", true);
        addTopMargin(content, retry, 12);
        retry.setOnClickListener(view -> {
            repository.retryIssueOperations();
            render();
        });
    }

    private LinearLayout dialogContent() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(12), dp(24), dp(4));
        TextView error = new TextView(this);
        error.setId(R.id.operations_form_error);
        error.setTextSize(14);
        error.setTextColor(color(R.color.danger));
        error.setVisibility(View.GONE);
        error.setPadding(0, dp(4), 0, dp(8));
        form.addView(error);
        return form;
    }

    private View wrapDialog(View content) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        return scroll;
    }

    private Field addField(LinearLayout parent, String label, String hint, boolean multiline) {
        Field field = field(label, hint, multiline);
        addTopMargin(parent, field.layout, 10);
        return field;
    }

    private Field field(String label, String hint, boolean multiline) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setHint(label);
        layout.setPlaceholderText(hint);
        layout.setBoxCornerRadii(dp(12), dp(12), dp(12), dp(12));
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setTextSize(16);
        input.setInputType(multiline ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES : InputType.TYPE_CLASS_TEXT);
        if (multiline) {
            input.setMinLines(3);
            input.setGravity(Gravity.TOP | Gravity.START);
        } else {
            input.setSingleLine(true);
        }
        layout.addView(input, new TextInputLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new Field(layout, input);
    }

    private Spinner spinner(LinearLayout parent, String label, List<String> values) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setTextColor(color(R.color.text_secondary));
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addTopMargin(parent, labelView, 12);
        Spinner spinner = new Spinner(this);
        spinner.setMinimumHeight(dp(48));
        spinner.setContentDescription(label);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        addTopMargin(parent, spinner, 4);
        return spinner;
    }

    private MaterialCardView infoCard(String heading, String body) {
        MaterialCardView card = (MaterialCardView) LayoutInflater.from(this)
                .inflate(R.layout.view_info_card, null, false);
        ((TextView) card.findViewById(R.id.infoHeading)).setText(heading);
        ((TextView) card.findViewById(R.id.infoBody)).setText(body);
        return card;
    }

    private void addInfo(LinearLayout parent, String heading, String body) {
        addTopMargin(parent, infoCard(heading, body), 10);
    }

    private void addHeader(LinearLayout parent, String value, int margin) {
        TextView header = new TextView(this);
        header.setText(value);
        header.setTextSize(19);
        header.setTextColor(color(R.color.operations_navy));
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addTopMargin(parent, header, margin);
    }

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold, int margin) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(color));
        view.setLineSpacing(dp(2), 1f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addTopMargin(parent, view, margin);
    }

    private void addPill(LinearLayout parent, String value) {
        TextView pill = new TextView(this);
        pill.setText(value);
        pill.setTextSize(12);
        pill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.setTextColor(color(R.color.operations_indigo));
        pill.setBackgroundColor(color(R.color.operations_outcome_surface));
        pill.setPadding(dp(12), dp(8), dp(12), dp(8));
        addTopMargin(parent, pill, 12);
    }

    private MaterialButton button(String label, boolean primary) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(52));
        button.setMinimumHeight(dp(52));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(12));
        button.setBackgroundTintList(ColorStateList.valueOf(color(primary ? R.color.operations_indigo : R.color.surface)));
        button.setTextColor(color(primary ? R.color.white : R.color.operations_indigo));
        if (!primary) {
            button.setStrokeWidth(dp(1));
            button.setStrokeColor(ColorStateList.valueOf(color(R.color.outline_strong)));
        }
        return button;
    }

    private void addTopMargin(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(marginDp);
        parent.addView(child, params);
    }

    private int color(int resource) { return ContextCompat.getColor(this, resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static String pretty(String enumName) {
        String[] words = enumName.toLowerCase(Locale.US).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
