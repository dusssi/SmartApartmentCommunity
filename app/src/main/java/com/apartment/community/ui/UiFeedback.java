package com.apartment.community.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.apartment.community.R;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reusable, accessible confirmation and feedback dialogs built on Material components and
 * lightweight custom XML layouts ({@code dialog_feedback.xml} and {@code dialog_confirm.xml}).
 */
public final class UiFeedback {

    private UiFeedback() { }

    private static boolean isActivityAlive(@Nullable Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    /**
     * Displays a polished success feedback dialog with an animated checkmark badge.
     *
     * @param activity      host activity
     * @param title         concise success heading (e.g. "Login successful", "Complaint submitted")
     * @param message       supporting description
     * @param autoDismissMs optional auto-dismiss delay in ms (0 or negative disables auto-dismiss)
     * @param onComplete    callback invoked once when the dialog is dismissed
     */
    public static void showSuccess(@NonNull Activity activity,
                                   @NonNull String title,
                                   @NonNull String message,
                                   long autoDismissMs,
                                   @Nullable Runnable onComplete) {
        if (!isActivityAlive(activity)) {
            if (onComplete != null) onComplete.run();
            return;
        }
        if (!Motion.areEnabled(activity) && autoDismissMs > 0 && autoDismissMs <= 700L) {
            // When reduced motion is enabled and a transient confirmation is requested, proceed directly.
            if (onComplete != null) onComplete.run();
            return;
        }

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_feedback, null, false);
        View card = root.findViewById(R.id.feedbackDialogCard);
        FrameLayout badge = root.findViewById(R.id.feedbackIconBadge);
        ImageView icon = root.findViewById(R.id.feedbackIcon);
        TextView titleView = root.findViewById(R.id.feedbackTitle);
        TextView messageView = root.findViewById(R.id.feedbackMessage);
        MaterialButton secondaryBtn = root.findViewById(R.id.feedbackSecondaryButton);
        MaterialButton primaryBtn = root.findViewById(R.id.feedbackPrimaryButton);

        badge.setBackgroundResource(R.drawable.bg_feedback_success_circle);
        icon.setImageResource(R.drawable.ic_check);
        icon.setColorFilter(ContextCompat.getColor(activity, R.color.success));
        titleView.setText(title);
        messageView.setText(message);
        secondaryBtn.setVisibility(View.GONE);
        primaryBtn.setText(R.string.action_continue);
        primaryBtn.setContentDescription(title + ". " + activity.getString(R.string.action_continue));
        primaryBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary)));
        primaryBtn.setTextColor(ContextCompat.getColor(activity, R.color.white));
        Motion.attachPressFeedback(primaryBtn);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        AtomicBoolean callbackFired = new AtomicBoolean(false);
        Runnable finishOnce = () -> {
            if (callbackFired.compareAndSet(false, true) && onComplete != null) {
                onComplete.run();
            }
        };

        primaryBtn.setOnClickListener(v -> {
            if (dialog.isShowing() && isActivityAlive(activity)) {
                dialog.dismiss();
            }
            finishOnce.run();
        });
        dialog.setOnDismissListener(d -> finishOnce.run());

        dialog.show();
        Motion.animateDialogEnter(card);
        Motion.animateCheckmarkBadge(badge, icon);

        if (autoDismissMs > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (dialog.isShowing() && isActivityAlive(activity)) {
                    dialog.dismiss();
                } else {
                    finishOnce.run();
                }
            }, autoDismissMs);
        }
    }

    /**
     * Displays a clear, non-sensitive error dialog with an optional Retry action.
     */
    public static void showError(@NonNull Activity activity,
                                 @NonNull String title,
                                 @NonNull String message,
                                 @Nullable Runnable onRetry) {
        if (!isActivityAlive(activity)) return;

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_feedback, null, false);
        View card = root.findViewById(R.id.feedbackDialogCard);
        FrameLayout badge = root.findViewById(R.id.feedbackIconBadge);
        ImageView icon = root.findViewById(R.id.feedbackIcon);
        TextView titleView = root.findViewById(R.id.feedbackTitle);
        TextView messageView = root.findViewById(R.id.feedbackMessage);
        MaterialButton secondaryBtn = root.findViewById(R.id.feedbackSecondaryButton);
        MaterialButton primaryBtn = root.findViewById(R.id.feedbackPrimaryButton);

        badge.setBackgroundResource(R.drawable.bg_feedback_error_circle);
        icon.setImageResource(R.drawable.ic_error_outline);
        icon.setColorFilter(ContextCompat.getColor(activity, R.color.danger));
        titleView.setText(title);
        messageView.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (onRetry != null) {
            secondaryBtn.setVisibility(View.VISIBLE);
            secondaryBtn.setText(R.string.action_cancel);
            secondaryBtn.setContentDescription(activity.getString(R.string.action_cancel));
            Motion.attachPressFeedback(secondaryBtn);
            secondaryBtn.setOnClickListener(v -> dialog.dismiss());

            primaryBtn.setText(R.string.action_retry);
            primaryBtn.setContentDescription(activity.getString(R.string.action_retry));
            primaryBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary)));
            primaryBtn.setTextColor(ContextCompat.getColor(activity, R.color.white));
            Motion.attachPressFeedback(primaryBtn);
            primaryBtn.setOnClickListener(v -> {
                dialog.dismiss();
                onRetry.run();
            });
        } else {
            secondaryBtn.setVisibility(View.GONE);
            primaryBtn.setText(R.string.action_continue);
            primaryBtn.setContentDescription(activity.getString(R.string.action_continue));
            primaryBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary)));
            primaryBtn.setTextColor(ContextCompat.getColor(activity, R.color.white));
            Motion.attachPressFeedback(primaryBtn);
            primaryBtn.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
        Motion.animateDialogEnter(card);
    }

    /**
     * Displays the polished logout confirmation dialog with Cancel and Logout actions.
     */
    public static void showLogoutConfirm(@NonNull Activity activity,
                                         @NonNull Runnable onConfirmLogout) {
        showConfirm(
                activity,
                activity.getString(R.string.logout_confirm_title),
                activity.getString(R.string.logout_confirm_message),
                activity.getString(R.string.action_logout),
                activity.getString(R.string.action_cancel),
                true,
                onConfirmLogout,
                null
        );
    }

    /**
     * Displays a reusable confirmation dialog for logout or destructive/irreversible actions.
     */
    public static void showConfirm(@NonNull Activity activity,
                                   @NonNull String title,
                                   @NonNull String message,
                                   @NonNull String confirmLabel,
                                   @NonNull String cancelLabel,
                                   boolean destructive,
                                   @NonNull Runnable onConfirm,
                                   @Nullable Runnable onCancel) {
        if (!isActivityAlive(activity)) return;

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_confirm, null, false);
        View card = root.findViewById(R.id.confirmDialogCard);
        FrameLayout badge = root.findViewById(R.id.confirmIconBadge);
        ImageView icon = root.findViewById(R.id.confirmIcon);
        TextView titleView = root.findViewById(R.id.confirmTitle);
        TextView messageView = root.findViewById(R.id.confirmMessage);
        MaterialButton cancelBtn = root.findViewById(R.id.confirmCancelButton);
        MaterialButton confirmBtn = root.findViewById(R.id.confirmActionButton);

        if (destructive) {
            badge.setBackgroundResource(R.drawable.bg_feedback_error_circle);
            icon.setImageResource(R.drawable.ic_logout);
            icon.setColorFilter(ContextCompat.getColor(activity, R.color.danger));
            confirmBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.danger)));
        } else {
            badge.setBackgroundResource(R.drawable.bg_feedback_info_circle);
            icon.setImageResource(R.drawable.ic_error_outline);
            icon.setColorFilter(ContextCompat.getColor(activity, R.color.primary));
            confirmBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary)));
        }
        confirmBtn.setTextColor(ContextCompat.getColor(activity, R.color.white));

        titleView.setText(title);
        messageView.setText(message);
        cancelBtn.setText(cancelLabel);
        cancelBtn.setContentDescription(cancelLabel);
        confirmBtn.setText(confirmLabel);
        confirmBtn.setContentDescription(confirmLabel);

        Motion.attachPressFeedback(cancelBtn);
        Motion.attachPressFeedback(confirmBtn);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        cancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (onCancel != null) onCancel.run();
        });
        confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        dialog.show();
        Motion.animateDialogEnter(card);
    }
}
