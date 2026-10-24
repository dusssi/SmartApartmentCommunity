package com.apartment.community.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Transparent local aggregates; every metric retains its source issue IDs and sample size. */
public final class ResolutionQuality {
    public static final class Metric {
        public final String id;
        public final String label;
        public final String formula;
        public final String unit;
        public final Double value;
        public final long numerator;
        public final long denominator;
        public final List<String> sourceIssueIds;

        public Metric(String id, String label, String formula, String unit, Double value,
                      long numerator, long denominator, List<String> sourceIssueIds) {
            this.id = id;
            this.label = label;
            this.formula = formula;
            this.unit = unit;
            this.value = value;
            this.numerator = numerator;
            this.denominator = denominator;
            this.sourceIssueIds = Collections.unmodifiableList(new ArrayList<>(sourceIssueIds));
        }

        public boolean hasValue() { return value != null && !value.isNaN() && !value.isInfinite(); }
    }

    public final int staffMarkedResolvedCount;
    public final int residentVerifiedCount;
    public final int awaitingConfirmationCount;
    public final int disputedCount;
    public final int legacyUnverifiedCount;
    public final List<Metric> metrics;

    public ResolutionQuality(int staffMarkedResolvedCount, int residentVerifiedCount,
                             int awaitingConfirmationCount, int disputedCount,
                             int legacyUnverifiedCount, List<Metric> metrics) {
        this.staffMarkedResolvedCount = staffMarkedResolvedCount;
        this.residentVerifiedCount = residentVerifiedCount;
        this.awaitingConfirmationCount = awaitingConfirmationCount;
        this.disputedCount = disputedCount;
        this.legacyUnverifiedCount = legacyUnverifiedCount;
        this.metrics = Collections.unmodifiableList(new ArrayList<>(metrics));
    }
}
