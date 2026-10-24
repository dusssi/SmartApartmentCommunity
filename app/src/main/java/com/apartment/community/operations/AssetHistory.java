package com.apartment.community.operations;

import com.apartment.community.model.Complaint;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.WorkLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Asset metadata plus linked issues and append-only service events. */
public final class AssetHistory {
    public final Asset asset;
    public final List<Complaint> issues;
    public final List<WorkLog> workLogs;
    public final List<ActivityEvent> events;

    public AssetHistory(Asset asset, List<Complaint> issues, List<WorkLog> workLogs,
                        List<ActivityEvent> events) {
        this.asset = asset;
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
        this.workLogs = Collections.unmodifiableList(new ArrayList<>(workLogs));
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }
}
