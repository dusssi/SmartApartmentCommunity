# Animation system

Implemented in `app/src/main/java/com/apartment/community/ui/Motion.java`. It uses only `ViewPropertyAnimator` (alpha, translation, scale). There are no infinite animations, no layout-changing animations, and no `ObjectAnimator` XML resources.

## Public API

| Method | Behaviour |
|---|---|
| `Motion.areEnabled(context)` | `ValueAnimator.areAnimatorsEnabled()` on API 26+; `ANIMATOR_DURATION_SCALE > 0` on API 24–25. |
| `Motion.enter(view, delayMs)` | Fade in from alpha 0 and slide up 12dp in 240 ms, decelerate. Cancels any running animation on the view first. |
| `Motion.staggerEnter(views)` | `enter` with a 40 ms step per item, capped at the first 8 items. |
| `Motion.attachPressFeedback(view)` | Scales to 0.98 on touch-down (90 ms) and back on release/cancel (160 ms). Returns `false` from the touch listener, so click, ripple and scroll still work. |

Constants: `DURATION_ENTER` 240 ms, `DURATION_PRESS_IN` 90 ms, `DURATION_PRESS_OUT` 160 ms, `STAGGER_STEP` 40 ms, `PRESS_SCALE` 0.98.

## Where it is used

| Pattern | Screens |
|---|---|
| Screen entrance (fade + 12dp slide, 240 ms) | Every main-shell route (`renderMain`), every auth page (`attachAuthPage`). |
| Staggered reveal | Role picker cards; dashboard and Work-hub action tiles (`addActionGrid`). |
| Press feedback | Action tiles, role cards, announcement and event cards, list rows (`SimpleListAdapter`). |
| List change animation | `DiffUtil` in `SimpleListAdapter.submitRows` with the default `RecyclerView` item animator. Search updates only animate rows that actually change. |
| Focus transitions | `TextInputLayout` focused stroke (1dp → 2dp, colour change) — provided by Material. |
| Navigation selected state | `BottomNavigationView` indicator — provided by Material. |

## Rules that are enforced

1. **No replay on in-place updates.** Status changes, read toggles and RSVP changes call `renderQuietly()` / `replaceCurrent()`, which set `animateNextRender = false`. Only route changes (navigate, root route, back) animate.
2. **Respect system settings.** When animations are disabled (developer option "Animator duration scale 0" or "Remove animations"), `enter` leaves views fully visible and `attachPressFeedback` snaps scale back to 1.
3. **No detached-view animation crashes.** Each call cancels the previous animation on the same view. Views removed by a re-render are simply discarded.
4. **Input is not blocked.** Press feedback never consumes touch events. Animations do not disable interaction.
5. **Transform and alpha only.** No `requestLayout` in animation paths.

## Not implemented

- Shared-element transitions between list and detail (the detail view is a new route; no shared-element transition).
- Custom dialog or bottom-sheet transitions. Existing `AlertDialog`s use the Material default.
- Button loading/success states. Actions in this demo are instant local updates, so there is no real work to show progress for.
- A splash animation or separate splash screen (see `UI_UX_AUDIT.md` A16).
- Explicit reduced-motion setting. Android exposes no reduced-motion API beyond the animator-duration setting, which is what `Motion.areEnabled` checks.

## Performance notes

- All animations are short (≤ 240 ms) and on at most one screen's worth of views (≤ 8 staggered items).
- Hardware layers are not forced; the framework may promote views during alpha animation.
- Frame timing has **not** been profiled on a Galaxy M33 or any other device (see `TESTING.md`).
