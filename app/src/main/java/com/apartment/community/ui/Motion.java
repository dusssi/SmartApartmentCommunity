package com.apartment.community.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.animation.ValueAnimator;
import android.os.Build;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Lightweight, reusable motion helpers built only on ViewPropertyAnimator-style transform/alpha
 * animation. Design rules:
 * <ul>
 *   <li>Durations are short (120–260 ms) and never loop; there are no infinite animations.</li>
 *   <li>Motion is skipped when the user has disabled animations in system developer settings
 *       (animator duration scale = 0, or "Remove animations"). Android has no separate
 *       reduced-motion API, so this is the platform signal we honour.</li>
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
    /** Delay between consecutive staggered items. */
    public static final long STAGGER_STEP = 40L;
    /** Stagger stops after this many items so long lists do not wait to reveal. */
    public static final int STAGGER_MAX_ITEMS = 8;
    /** Scale applied while a card or tile is pressed. */
    public static final float PRESS_SCALE = 0.98f;

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
        float offset = view.getResources().getDimension(com.apartment.community.R.dimen.motion_enter_offset);
        view.setAlpha(0f);
        view.setTranslationY(offset);
        view.animate()
                .alpha(1f)
                .translationY(0f)
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
}
