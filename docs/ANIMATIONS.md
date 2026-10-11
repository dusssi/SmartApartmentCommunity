# Animation & Feedback System

Implemented in `app/src/main/java/com/apartment/community/ui/Motion.java` and `app/src/main/java/com/apartment/community/ui/UiFeedback.java`. Built on `ViewPropertyAnimator`, `ValueAnimator`, and lightweight Canvas drawables with zero external animation dependencies.

## Public API — `Motion.java`

| Method | Behaviour |
|---|---|
| `Motion.areEnabled(context)` | `ValueAnimator.areAnimatorsEnabled()` on API 26+; `ANIMATOR_DURATION_SCALE > 0` on API 24–25. |
| `Motion.enter(view, delayMs)` | Fade in from alpha 0 and slide up 16dp (`@dimen/motion_enter_offset`) in 240 ms (`DecelerateInterpolator`). Cancels any running animation on the view first. |
| `Motion.staggerEnter(views)` | `enter` with a 40 ms step per item, capped at the first 8 items. |
| `Motion.attachPressFeedback(view)` | Scales to `0.98` on touch-down (90 ms) and back on release/cancel (160 ms). Returns `false` from the touch listener so click, ripple and scroll still work. |
| `Motion.animateSplash(...)` | Brief cold-start splash reveal (`420 ms` + `160 ms` exit): centered logo fade-in and scale-up (`0.86` → `1.0`), app name and tagline slide-in, and blue/teal accent bar sweep. Skips immediately (`0 ms`) when system animations are disabled. |
| `Motion.animateDialogEnter(card)` | Scales dialog card from `0.94` → `1.0` and fades in over `180 ms`. |
| `Motion.animateCheckmarkBadge(badge, icon)` | Scales circular success badge (`0.72` → `1.0`, `OvershootInterpolator`) and reveals checkmark icon (`200 ms`). |
| `Motion.shakeError(view)` | Subtle horizontal nudge (`±6dp`, `200 ms`) when form validation fails. |
| `Motion.setButtonLoading(button, label, inputs...)` | Attaches an inline rotating circular arc `InlineSpinnerDrawable` to a `MaterialButton`, updates label, and disables the button and form inputs to prevent duplicate submissions. |
| `Motion.restoreButton(button, label, inputs...)` | Stops the inline spinner, clears the button icon, restores the default label, and re-enables the button and form inputs. |
| `Motion.completeButtonWithConfirmation(...)` | Transitions a loading button to a brief checkmark confirmation state (`200 ms` scale pulse), restores the button and inputs so no control stays stuck, and runs the completion callback. |

## Reusable Dialogs — `UiFeedback.java`

| Method | Behaviour |
|---|---|
| `UiFeedback.showSuccess(activity, title, message, autoDismissMs, onComplete)` | Displays `layout/dialog_feedback.xml` with animated green checkmark badge, concise title and message, and accessible Continue action. |
| `UiFeedback.showError(activity, title, message, onRetry)` | Displays `layout/dialog_feedback.xml` with red alert badge, clear message, and optional `Retry` + `Cancel` actions. |
| `UiFeedback.showLogoutConfirm(activity, onConfirmLogout)` | Displays `layout/dialog_confirm.xml` with `"Are you sure you want to log out?"`, `Cancel` (leaves session unchanged), and `Logout` (clears session and returns to Sign In). |
| `UiFeedback.showConfirm(...)` | Reusable confirmation dialog for destructive or status-reset actions (cancelling RSVP, reopening resolved complaints/requests, resetting visitor status). |

## Rules Enforced

1. **No fake processing delays.** Synchronous local persistence operations execute immediately; feedback uses a brief confirmation animation (`completeButtonWithConfirmation` + `UiFeedback.showSuccess`) rather than pretending a network request is in flight.
2. **No replay on in-place updates.** Status changes, read toggles and RSVP changes call `renderQuietly()` / `replaceCurrent()`, which set `animateNextRender = false`.
3. **Respect system reduced-motion settings.** When animations are disabled (`Motion.areEnabled(context) == false`), splash overlay, dialog entrances, checkmark pulses, and screen transitions complete instantaneously (`0 ms`).
4. **Prevent duplicate submissions & stuck states.** `isSubmitting` and `Motion.setButtonLoading` disable the submit button and inputs during execution, and `Motion.restoreButton` / `Motion.completeButtonWithConfirmation` restore them on both success and failure.
