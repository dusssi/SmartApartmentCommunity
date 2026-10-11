# Design system

These tokens implement the **provisional** direction from the Phase 2 brief. No Google Stitch exports were available, so values are not claimed to match Stitch. Replace them when the references are supplied; all screens read from these files.

## Source files

| File | Contents |
|---|---|
| `app/src/main/res/values/colors.xml` | Brand, surfaces, text, outline and semantic colours (existing names kept). |
| `app/src/main/res/values/dimens.xml` | 4dp spacing scale, page gutters, corner radii, touch/icon sizes, elevation, motion distance. |
| `app/src/main/res/values/styles.xml` | Typography hierarchy as `TextAppearance.App.*`. |
| `app/src/main/res/values/themes.xml` | `Theme.ApartmentCommunity` (Material 3 light, no action bar). |
| `app/src/main/res/drawable/` | Icons (vector), icon container, row marker, card/shape helpers. |
| `app/src/main/java/com/apartment/community/ui/Motion.java` | Motion helpers (see `ANIMATIONS.md`). |

## Colour tokens

| Token | Value | Use |
|---|---|---|
| `primary` | `#0E5C47` | Brand emerald: primary buttons, focused field stroke, icons. |
| `primary_dark` | `#0A4535` | Headings on brand surfaces, status-bar text colour family. |
| `primary_light` | `#E3F0EA` | Icon containers, badges, selected states. |
| `primary_ripple` | `#220E5C47` | Ripple/highlight. |
| `secondary` | `#C4863A` | Unread dot and a single warm accent. |
| `background` | `#F7F5F0` | Warm off-white screen background. |
| `surface` | `#FFFFFF` | Cards, bottom navigation, fields. |
| `surface_variant` | `#F0EDE5` | Reserved for grouped areas. |
| `outline` | `#E0DACE` | Hairline card borders (decorative; cards are also separated by elevation). |
| `outline_strong` | `#8C8579` | Text-field borders (3.65:1 on white, above the WCAG 3:1 non-text guideline). |
| `text_primary` | `#1E2622` | Charcoal body and headings. |
| `text_secondary` | `#56605A` | Supporting text. |
| `text_muted` | `#66706A` | Metadata, captions, placeholders. |
| `success` / `_container` | `#266B48` / `#E4F2EA` | Positive status. |
| `warning` / `_container` | `#8F5E10` / `#FBF0DC` | Open/unread/attention, demo disclaimer. |
| `danger` / `_container` | `#B3413B` / `#F9E4E2` | Errors, destructive actions (logout). |
| `info` / `_container` | `#2F6690` / `#E2EDF6` | In-progress/scheduled status. |

Contrast ratios were **calculated from the hex values** (WCAG 2.x relative luminance), not measured on a device or with an accessibility scanner. Body text pairs measured: `text_primary` 14.2–15.5:1, `text_secondary` 6.0–6.5:1, `text_muted` 4.7–5.1:1 (on `background`/`surface`); `primary` on white 7.95:1; warning on its container 4.9:1; success on its container 5.5:1; info on its container 5.2:1; field border `outline_strong` 3.65:1.

## Spacing and shape

- Spacing: `space_2xs` 4dp, `space_xs` 8dp, `space_sm` 12dp, `space_md` 16dp, `space_lg` 20dp, `space_xl` 24dp, `space_2xl` 32dp. Page gutters: 20dp.
- Corners: cards 16dp (`radius_card`), fields and primary buttons 14dp (`radius_md`), icon containers 10dp (`radius_sm`), chips pill-shaped.
- Touch: minimum 48dp (`touch_min`). Primary buttons are 50dp high.
- Elevation: cards 1dp with a 1dp outline; navigation 4dp. No gradients and no heavy shadows.

## Typography

| Style | Size / weight | Use |
|---|---|---|
| `TextAppearance.App.Headline` | 26sp bold | Screen greeting and auth titles. |
| `TextAppearance.App.Title` | 17sp bold | Section titles, card titles. |
| `TextAppearance.App.Body` | 15sp | Body copy. |
| `TextAppearance.App.BodySecondary` | 14sp | Supporting copy. |
| `TextAppearance.App.Label` | 12sp bold | Labels and section actions. |
| `TextAppearance.App.Overline` | 11sp bold caps | Eyebrow labels in info cards. |
| `TextAppearance.App.Caption` | 12sp muted | Metadata. |
| `TextAppearance.App.Chip` | 12sp bold | Status chip text. |

Java-built screens still set sizes through the `text()` helper (for example 13–18sp values). Migrating them to the styles is listed as remaining work.

## Components

| Component | Where | Notes |
|---|---|---|
| Action tile | `layout/view_action_tile.xml` | Icon, title, caption; ripple and press-scale. |
| Section header | `layout/view_section_header.xml` | Title is an accessibility heading; optional 48dp text action. |
| Info card | `layout/view_info_card.xml` | Overline + body. |
| List row with status chip | `layout/row_simple_item.xml` + `SimpleListAdapter` | Status colour is derived from keywords (`badgeColor`). |
| Role card | `MainActivity.showRolePicker()` | Icon container, title, description, chevron. |
| Text field | `MainActivity.createField()` | `TextInputLayout` outlined box with floating label, placeholder, focused/error stroke states. |
| Buttons | `fullButton`, `smallButton`, `textButton` | Material buttons with the tokens above. |
| Empty state | `showList()` empty text | Centred explanatory copy. |
| Bottom navigation | `createBottomNavigation()` | Role-aware items; hidden while the keyboard is open. |

## Limitations

- Dark theme is **not** supported. The app uses the Light theme; the earlier DayNight parent with fixed light colours is removed.
- Not all Java screens use the text appearances yet.
- Detail, profile-edit and form layouts were not visually redesigned in this change.
- No Stitch fidelity claims; see `UI_UX_AUDIT.md`.
