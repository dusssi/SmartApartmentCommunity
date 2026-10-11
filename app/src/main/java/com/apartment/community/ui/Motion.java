package com.apartment.community.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.apartment.community.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Lightweight, reusable motion helpers built on ViewPropertyAnimator and Canvas drawables.
 * Design rules:
 * <ul>
 *   <li>Durations are short (90–420 ms) and restrained; no distracting particle or bounce effects.</li>
 *   <li>Motion is skipped when the user has disabled animations in system developer settings
 *       (animator duration scale = 0, or "Remove animations").</li>
 *   <li>Touch feedback never consumes the event, so clicks, long-presses and scrolling still work.</li>
 *   <li>Every animation is cancelled before a new one starts, so views recycled or detached
 *       mid-animation are not left in a half-transformed state.</li>
 * </ul>
 */
public final class Motion {
    /** Screen and card entrance duration. Within the 180–300 ms guideline. */
    public static final long DURATION_ENTER = 240L;
    /** Press-in duration; deliberately quick so taps feel immediate. */
    public static final long DURATION_PRESS_IN = 90L;
    /** Release duration. */
    public static final long DURATION_PRESS_OUT = 160L;
    /** Brief startup splash reveal duration. */
    public static final long DURATION_SPLASH = 420L;
    /** Splash fade-out transition duration. */
    public static final long DURATION_SPLASH_EXIT = 160L;
    /** Dialog entrance duration. */
    public static final long DURATION_DIALOG_ENTER = 180L;
    /** Brief button confirmation pulse duration. */
    public static final long DURATION_CONFIRM_PULSE = 200L;
    /** Delay between consecutive staggered items. */
    public static final long STAGGER_STEP = 40L;
    /** Stagger stops after this many items so long lists do not wait to reveal. */
    public static final int STAGGER_MAX_ITEMS = 8;
    /** Scale applied while a card, tile or button is pressed. */
    public static final float PRESS_SCALE = 0.98f;

    private static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator();
    private static final OvershootInterpolator SOFT_OVERSHOOT = new OvershootInterpolator(1.35f);

    private Motion() { }

    /** True when the user has animations enabled in system settings. */
    public static boolean areEnabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        ContentResolver resolver = context.getContentResolver();
        float scale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale > 0f;
    }

    /**
     * Fades a view in while it slides up a few dp. Does nothing when motion is disabled, leaving
     * the view fully visible at rest.
     */
    public static void enter(View view, long delayMs) {
        if (view == null) return;
        view.animate().cancel();
        if (!areEnabled(view.getContext())) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
        float offset = view.getResources().getDimension(R.dimen.motion_enter_offset);
        view.setAlpha(0f);
        view.setTranslationY(offset);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setInterpolator(DECELERATE)
                .setStartDelay(Math.max(0L, delayMs))
                .setDuration(DURATION_ENTER)
                .start();
    }

    /** Staggered reveal for a small set of items (cards, tiles, role options). */
    public static void staggerEnter(List<? extends View> views) {
        if (views == null) return;
        for (int i = 0; i < views.size(); i++) {
            int index = Math.min(i, STAGGER_MAX_ITEMS);
            enter(views.get(i), index * STAGGER_STEP);
        }
    }

    /**
     * Adds a subtle press-in scale to a clickable view. Returns {@code false} from the touch
     * listener so the normal click, ripple and scroll handling are unaffected.
     */
    public static void attachPressFeedback(@NonNull View view) {
        view.setOnTouchListener((target, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    animatePressed(target, true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animatePressed(target, false);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private static void animatePressed(View target, boolean pressed) {
        target.animate().cancel();
        if (!areEnabled(target.getContext())) {
            target.setScaleX(1f);
            target.setScaleY(1f);
            return;
        }
        float scale = pressed ? PRESS_SCALE : 1f;
        target.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(pressed ? DURATION_PRESS_IN : DURATION_PRESS_OUT)
                .start();
    }

    /**
     * Animates the startup splash overlay: subtle logo fade-in and scale-up, title/tagline reveal,
     * and a brief blue/teal accent bar sweep, then fades out and runs {@code onFinished}.
     * When reduced motion is enabled, finishes immediately without delay.
     */
    public static void animateSplash(@NonNull View splashRoot,
                                     @Nullable View logoView,
                                     @Nullable View titleView,
                                     @Nullable View taglineView,
                                     @Nullable View accentBar,
                                     @NonNull Runnable onFinished) {
        Context context = splashRoot.getContext();
        if (!areEnabled(context)) {
            onFinished.run();
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        if (logoView != null) {
            logoView.animate().cancel();
            logoView.setAlpha(0f);
            logoView.setScaleX(0.86f);
            logoView.setScaleY(0.86f);
            logoView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(SOFT_OVERSHOOT)
                    .setDuration(280L)
                    .start();
        }
        if (titleView != null) {
            titleView.animate().cancel();
            titleView.setAlpha(0f);
            titleView.setTranslationY(8f * density);
            titleView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setInterpolator(DECELERATE)
                    .setStartDelay(50L)
                    .setDuration(220L)
                    .start();
        }
        if (taglineView != null) {
            taglineView.animate().cancel();
            taglineView.setAlpha(0f);
            taglineView.setTranslationY(8f * density);
            taglineView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setInterpolator(DECELERATE)
                    .setStartDelay(80L)
                    .setDuration(220L)
                    .start();
        }
        if (accentBar != null) {
            accentBar.animate().cancel();
            accentBar.setTranslationX(-24f * density);
            accentBar.animate()
                    .translationX(52f * density)
                    .setInterpolator(DECELERATE)
                    .setDuration(DURATION_SPLASH)
                    .start();
        }
        splashRoot.animate().cancel();
        splashRoot.setAlpha(1f);
        splashRoot.animate()
                .alpha(0f)
                .setStartDelay(DURATION_SPLASH)
                .setDuration(DURATION_SPLASH_EXIT)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean completed = false;
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!completed) {
                            completed = true;
                            splashRoot.animate().setListener(null);
                            onFinished.run();
                        }
                    }
                })
                .start();
    }

    /** Subtle scale-and-fade entrance for confirmation and feedback dialog cards. */
    public static void animateDialogEnter(@Nullable View dialogCard) {
        if (dialogCard == null) return;
        dialogCard.animate().cancel();
        if (!areEnabled(dialogCard.getContext())) {
            dialogCard.setAlpha(1f);
            dialogCard.setScaleX(1f);
            dialogCard.setScaleY(1f);
            return;
        }
        dialogCard.setAlpha(0f);
        dialogCard.setScaleX(0.94f);
        dialogCard.setScaleY(0.94f);
        dialogCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(DECELERATE)
                .setDuration(DURATION_DIALOG_ENTER)
                .start();
    }

    /** Restrained scale-pop and fade reveal for the checkmark badge in success feedback dialogs. */
    public static void animateCheckmarkBadge(@Nullable View badgeContainer, @Nullable View checkIcon) {
        if (badgeContainer == null) return;
        badgeContainer.animate().cancel();
        if (!areEnabled(badgeContainer.getContext())) {
            badgeContainer.setAlpha(1f);
            badgeContainer.setScaleX(1f);
            badgeContainer.setScaleY(1f);
            if (checkIcon != null) {
                checkIcon.animate().cancel();
                checkIcon.setAlpha(1f);
                checkIcon.setScaleX(1f);
                checkIcon.setScaleY(1f);
            }
            return;
        }
        badgeContainer.setAlpha(0f);
        badgeContainer.setScaleX(0.72f);
        badgeContainer.setScaleY(0.72f);
        badgeContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(SOFT_OVERSHOOT)
                .setDuration(240L)
                .start();
        if (checkIcon != null) {
            checkIcon.animate().cancel();
            checkIcon.setAlpha(0f);
            checkIcon.setScaleX(0.55f);
            checkIcon.setScaleY(0.55f);
            checkIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(SOFT_OVERSHOOT)
                    .setStartDelay(50L)
                    .setDuration(200L)
                    .start();
        }
    }

    /** Subtle horizontal shake for invalid form submission feedback. */
    public static void shakeError(@Nullable View view) {
        if (view == null) return;
        view.animate().cancel();
        if (!areEnabled(view.getContext())) {
            view.setTranslationX(0f);
            return;
        }
        float delta = 6f * view.getResources().getDisplayMetrics().density;
        ValueAnimator shake = ValueAnimator.ofFloat(0f, -delta, delta, -delta * 0.6f, delta * 0.6f, 0f);
        shake.setDuration(200L);
        shake.addUpdateListener(anim -> view.setTranslationX((Float) anim.getAnimatedValue()));
        shake.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setTranslationX(0f);
            }
        });
        shake.start();
    }

    /**
     * Puts a {@link MaterialButton} into an active loading state with an inline progress indicator
     * and disables the button and any supplied form inputs to prevent duplicate submissions.
     */
    public static void setButtonLoading(@NonNull MaterialButton button,
                                        @NonNull CharSequence loadingLabel,
                                        @Nullable View... inputsToDisable) {
        button.animate().cancel();
        button.setScaleX(1f);
        button.setScaleY(1f);
        button.setEnabled(false);
        if (inputsToDisable != null) {
            for (View input : inputsToDisable) {
                if (input != null) input.setEnabled(false);
            }
        }
        Context context = button.getContext();
        int tint = button.getCurrentTextColor();
        InlineSpinnerDrawable spinner = new InlineSpinnerDrawable(context, tint);
        button.setTag(spinner);
        button.setIcon(spinner);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setIconPadding(Math.round(8f * context.getResources().getDisplayMetrics().density));
        button.setText(loadingLabel);
        spinner.start();
    }

    /**
     * Restores a {@link MaterialButton} and associated form inputs after an operation finishes
     * or fails validation/execution.
     */
    public static void restoreButton(@NonNull MaterialButton button,
                                     @NonNull CharSequence normalLabel,
                                     @Nullable View... inputsToRestore) {
        Object tag = button.getTag();
        if (tag instanceof InlineSpinnerDrawable) {
            ((InlineSpinnerDrawable) tag).stop();
            button.setTag(null);
        }
        button.animate().cancel();
        button.setScaleX(1f);
        button.setScaleY(1f);
        button.setIcon(null);
        button.setText(normalLabel);
        button.setEnabled(true);
        if (inputsToRestore != null) {
            for (View input : inputsToRestore) {
                if (input != null) input.setEnabled(true);
            }
        }
    }

    /**
     * Transitions a button from its loading state to a brief checkmark confirmation state,
     * restores the button and inputs so no control remains stuck, and invokes {@code onComplete}.
     * Never uses arbitrary delays to pretend a network call is still running.
     */
    public static void completeButtonWithConfirmation(@NonNull MaterialButton button,
                                                      @NonNull CharSequence confirmedLabel,
                                                      @NonNull CharSequence restoredLabel,
                                                      @NonNull Runnable onComplete,
                                                      @Nullable View... inputsToRestore) {
        Object tag = button.getTag();
        if (tag instanceof InlineSpinnerDrawable) {
            ((InlineSpinnerDrawable) tag).stop();
            button.setTag(null);
        }
        Context context = button.getContext();
        if (!areEnabled(context)) {
            restoreButton(button, restoredLabel, inputsToRestore);
            onComplete.run();
            return;
        }
        Drawable check = ContextCompat.getDrawable(context, R.drawable.ic_check);
        button.setIcon(check);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setText(confirmedLabel);
        button.animate().cancel();
        button.setScaleX(0.97f);
        button.setScaleY(0.97f);
        button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(SOFT_OVERSHOOT)
                .setDuration(DURATION_CONFIRM_PULSE)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean finished = false;
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!finished) {
                            finished = true;
                            button.animate().setListener(null);
                            restoreButton(button, restoredLabel, inputsToRestore);
                            onComplete.run();
                        }
                    }
                })
                .start();
    }

    /**
     * Lightweight inline circular arc spinner drawable for buttons. Uses zero external dependencies
     * and respects system reduced-motion settings.
     */
    private static final class InlineSpinnerDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcBounds = new RectF();
        private final int sizePx;
        private final boolean animate;
        private float startAngle = 0f;
        private ValueAnimator animator;

        InlineSpinnerDrawable(@NonNull Context context, int color) {
            float density = context.getResources().getDisplayMetrics().density;
            this.sizePx = Math.round(18f * density);
            this.animate = areEnabled(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.2f * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(color);
        }

        void start() {
            if (!animate) {
                invalidateSelf();
                return;
            }
            stop();
            animator = ValueAnimator.ofFloat(0f, 360f);
            animator.setDuration(750L);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                startAngle = (Float) a.getAnimatedValue();
                invalidateSelf();
            });
            animator.start();
        }

        void stop() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }

        @Override
        public int getIntrinsicWidth() {
            return sizePx;
        }

        @Override
        public int getIntrinsicHeight() {
            return sizePx;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            float inset = paint.getStrokeWidth();
            arcBounds.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            canvas.drawArc(arcBounds, startAngle, 250f, false, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
