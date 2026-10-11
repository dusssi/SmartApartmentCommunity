package com.apartment.community.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.apartment.community.R;
import com.apartment.community.ui.Motion;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** Reusable accessible card-row adapter used by the app's local demonstration lists. */
public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.RowHolder> {
    public interface OnRowClick { void onClick(ListRow row); }

    private final Context context;
    private final OnRowClick onRowClick;
    private final List<ListRow> rows = new ArrayList<>();

    public SimpleListAdapter(Context context, OnRowClick onRowClick) {
        this.context = context;
        this.onRowClick = onRowClick;
    }

    /**
     * Replaces the visible rows with a minimal diff. Typing in search therefore only inserts,
     * removes or rebinds the rows that changed, so the list does not replay after every keystroke
     * and the scroll position is preserved for unchanged rows.
     */
    public void submitRows(List<ListRow> nextRows) {
        final List<ListRow> previous = new ArrayList<>(rows);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return previous.size(); }
            @Override public int getNewListSize() { return nextRows.size(); }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return sameText(previous.get(oldPosition).id, nextRows.get(newPosition).id);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                ListRow a = previous.get(oldPosition);
                ListRow b = nextRows.get(newPosition);
                return sameText(a.title, b.title) && sameText(a.subtitle, b.subtitle)
                        && sameText(a.metadata, b.metadata) && sameText(a.badge, b.badge);
            }
        });
        rows.clear();
        rows.addAll(nextRows);
        result.dispatchUpdatesTo(this);
    }

    private static boolean sameText(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(context).inflate(R.layout.row_simple_item, parent, false);
        MaterialCardView card = root.findViewById(R.id.rowCard);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(2), dp(5), dp(2), dp(7));
        card.setLayoutParams(params);
        Motion.attachPressFeedback(card);
        return new RowHolder(card,
                root.findViewById(R.id.rowMarker),
                root.findViewById(R.id.rowTitle),
                root.findViewById(R.id.rowSubtitle),
                root.findViewById(R.id.rowMetadata),
                root.findViewById(R.id.rowBadge));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        ListRow row = rows.get(position);
        holder.title.setText(row.title);
        holder.subtitle.setText(row.subtitle);
        holder.metadata.setText(row.metadata);
        holder.metadata.setVisibility(row.metadata == null || row.metadata.trim().isEmpty() ? View.GONE : View.VISIBLE);
        holder.badge.setText(row.badge == null ? "" : row.badge);
        holder.badge.setVisibility(row.badge == null || row.badge.trim().isEmpty() ? View.GONE : View.VISIBLE);
        int tint = badgeColor(row.badge);
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(withAlpha(tint, 0.12f));
        chip.setCornerRadius(dp(20));
        holder.badge.setBackground(chip);
        holder.badge.setTextColor(tint);
        holder.marker.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tint));
        holder.card.setContentDescription(row.title + ". " + row.subtitle
                + (row.badge == null ? "" : ". Badge: " + row.badge) + ". Double tap to open.");
        holder.card.setOnClickListener(view -> {
            if (onRowClick != null) onRowClick.onClick(row);
        });
    }

    @Override public int getItemCount() { return rows.size(); }

    private int badgeColor(String value) {
        if (value == null) return ContextCompat.getColor(context, R.color.primary);
        String key = value.toLowerCase(java.util.Locale.US);
        if (key.contains("unread") || key.contains("high") || key.contains("important")
                || key.contains("open") || key.contains("expected")) {
            return ContextCompat.getColor(context, R.color.warning);
        }
        if (key.contains("resolved") || key.contains("completed") || key.contains("checked out")
                || "read".equals(key) || key.contains("attending") || key.contains("going")) {
            return ContextCompat.getColor(context, R.color.success);
        }
        if (key.contains("progress") || key.contains("scheduled") || key.contains("approved") || key.contains("checked in")) {
            return ContextCompat.getColor(context, R.color.info);
        }
        return ContextCompat.getColor(context, R.color.primary);
    }

    private int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(255 * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View marker;
        final TextView title;
        final TextView subtitle;
        final TextView metadata;
        final TextView badge;

        RowHolder(MaterialCardView card, View marker, TextView title, TextView subtitle,
                  TextView metadata, TextView badge) {
            super(card);
            this.card = card;
            this.marker = marker;
            this.title = title;
            this.subtitle = subtitle;
            this.metadata = metadata;
            this.badge = badge;
        }
    }
}
