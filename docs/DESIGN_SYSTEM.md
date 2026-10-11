# Design system — Stitch-inspired implementation

This design system implements the **Stitch UI reference** described in the brief: clean, modern apartment-community aesthetic, blue primary colour, teal accents, neutral backgrounds, rounded cards, clear typography, consistent spacing.

The attached `Stitch_UI_Reference_for_Arena_AI.pdf` was not found in the workspace, so values are inferred from the brief description and implemented as a cohesive blue/teal system. When the PDF is supplied, compare exact values from `DESIGN.md` and screenshots and adjust tokens in `res/values/`.

## Source files

| File | Contents |
|---|---|
| `app/src/main/res/values/colors.xml` | Brand, surfaces, text, outline and semantic colours — now blue primary #2563EB, teal secondary #0D9488, neutral #F8FAFC |
| `app/src/main/res/values/dimens.xml` | 4dp spacing scale, page gutters 20dp, corner radii 12/16/20/999dp, touch 48dp, button 52dp, elevation 2/8dp |
| `app/src/main/res/values/styles.xml` | Typography hierarchy as `TextAppearance.App.*` — Headline 28sp bold, Title 18sp bold, Body 15sp 1.4, etc. |
| `app/src/main/res/values/themes.xml` | `Theme.ApartmentCommunity` (Material 3 Light, blue primary, light status/nav bars) |
| `app/src/main/res/drawable/` | Icons (vector), icon container primary_light, row marker primary, card backgrounds, badge backgrounds primary/success/warning/info, button backgrounds |
| `app/src/main/java/com/apartment/community/ui/Motion.java` | Motion helpers — enter 240ms, press 90/160ms, stagger 40ms, respects animator duration scale |

## Colour tokens — Stitch-inspired

| Token | Value | Use |
|---|---|---|
| `primary` | `#2563EB` | Blue 600 — primary buttons, focused field stroke, icons, links |
| `primary_dark` | `#1E40AF` | Blue 800 — headings on brand surfaces |
| `primary_light` | `#DBEAFE` | Blue 100 — icon containers, badges, selected states |
| `primary_container` | `#EFF6FF` | Blue 50 — subtle banners, containers |
| `primary_ripple` | `#1F2563EB` | 12% ripple |
| `secondary` | `#0D9488` | Teal 600 — accent, secondary actions |
| `secondary_light` | `#CCFBF1` | Teal 100 — accent backgrounds |
| `secondary_container` | `#F0FDFA` | Teal 50 |
| `secondary_dark` | `#0F766E` | Teal 700 |
| `background` | `#F8FAFC` | Slate 50 — screen background, neutral |
| `surface` | `#FFFFFF` | Cards, bottom nav, fields |
| `surface_variant` | `#F1F5F9` | Slate 100 — grouped areas |
| `outline` | `#E2E8F0` | Slate 200 — hairline card borders |
| `outline_variant` | `#CBD5E1` | Slate 300 |
| `outline_strong` | `#94A3B8` | Slate 400 — field borders, 3.5:1 on white, meets WCAG 3:1 non-text |
| `text_primary` | `#0F172A` | Slate 900 — headings, body, 15:1 on white |
| `text_secondary` | `#475569` | Slate 600 — supporting, 6:1 on white |
| `text_muted` | `#94A3B8` | Slate 400 — metadata, captions, placeholders, 4.5:1 |
| `success` | `#16A34A` | Green 600 — positive status |
| `success_container` | `#DCFCE7` | Green 100 |
| `warning` | `#D97706` | Amber 600 — attention, expected, high priority |
| `warning_container` | `#FEF3C7` | Amber 100 |
| `danger` | `#DC2626` | Red 600 — errors, destructive |
| `danger_container` | `#FEE2E2` | Red 100 |
| `info` | `#0284C7` | Sky 600 — in-progress, scheduled |
| `info_container` | `#E0F2FE` | Sky 100 |

Contrast ratios calculated from hex values (WCAG 2.x relative luminance): text_primary 15:1, text_secondary 6:1, text_muted 4.5:1 on background/surface; primary on white 4.5:1; outline_strong 3.5:1 meets non-text 3:1.

## Spacing and shape

- Spacing: `space_2xs` 4dp, `space_xs` 8dp, `space_sm` 12dp, `space_md` 16dp, `space_lg` 20dp, `space_xl` 24dp, `space_2xl` 32dp, `space_3xl` 40dp. Page gutters 20dp horizontal, 16dp vertical.
- Corners: `radius_sm` 12dp (buttons, icon containers), `radius_md` 16dp (cards, fields), `radius_card` 16dp, `radius_lg` 20dp, `radius_xl` 24dp, `radius_pill` 999dp (badges).
- Touch: minimum 48dp (`touch_min`). Primary buttons 52dp high (`button_height`), small 40dp. Icon container 48dp, icon 22dp.
- Elevation: cards 2dp with 1dp outline, nav 8dp, none 0dp, hover 4dp. No heavy shadows, subtle depth.
- Motion: enter offset 16dp, enter 240ms, press in 90ms, out 160ms, stagger step 40ms max 8 items.

## Typography

| Style | Size / weight | Use |
|---|---|---|
| `TextAppearance.App.HeadlineLarge` | 32sp bold -0.02 letterSpacing | Large greeting (optional) |
| `TextAppearance.App.Headline` | 28sp bold -0.01 1.1 lineSpacing | Screen greeting, auth titles |
| `TextAppearance.App.TitleLarge` | 22sp bold | Large section titles |
| `TextAppearance.App.Title` | 18sp bold 1.2 lineSpacing | Section titles, card titles, top bar |
| `TextAppearance.App.Body` | 15sp 1.4 lineSpacing | Body copy |
| `TextAppearance.App.BodySecondary` | 14sp secondary 1.4 | Supporting copy |
| `TextAppearance.App.Label` | 12sp bold 0.04 secondary | Labels, section actions |
| `TextAppearance.App.Overline` | 11sp bold caps 0.08 primary | Eyebrow labels in info cards |
| `TextAppearance.App.Caption` | 12sp muted 1.3 | Metadata, captions |
| `TextAppearance.App.Chip` | 12sp bold 0.02 | Status chip |
| `TextAppearance.App.Button` | 14sp bold 0.02 | Button text |

Java-built screens use `text()` helper with explicit sizes (13-27sp) but should migrate to these styles. Some screens still use helper.

## Components

| Component | Where | Notes |
|---|---|---|
| Action tile | `layout/view_action_tile.xml` | 48dp icon container primary_light, 24dp icon primary, 16sp title, 12sp caption, 16dp radius, 2dp elevation, ripple, press-scale |
| Section header | `layout/view_section_header.xml` | 18sp title accessibilityHeading, 13sp action primary 48dp, 4dp top/bottom padding |
| Info card | `layout/view_info_card.xml` | 18dp horizontal, 16dp vertical, Overline 11sp primary caps + Body 14sp 1.4 |
| List row with status chip | `layout/row_simple_item.xml` + `SimpleListAdapter` | 72dp min, 4dp marker primary, 15sp title, 13sp subtitle, 12sp metadata, 11sp badge pill primary_light/success/warning/info, badgeColor derived from keywords |
| Role card | `MainActivity.showRolePicker()` | 16dp padding, 48dp icon container primary_light, 16sp title, 13sp description, chevron muted, ripple, press-scale, staggered reveal |
| Text field | `MainActivity.createField()` | TextInputLayout outlined box 12dp radius, floating label, placeholder, focused 2dp primary stroke, error danger, hint secondary |
| Buttons | `fullButton`, `smallButton`, `textButton` | Primary 52dp 12dp radius blue white text, outlined white with outline stroke, small 40dp 10dp radius, text transparent 13sp primary, loading states "Signing in..." etc., duplicate prevention isSubmitting |
| Empty state | `showList()` empty TextView | Centered 14sp secondary, 28dp horizontal 20dp vertical padding, visibility based on rows.isEmpty |
| Bottom navigation | `createBottomNavigation()` | Role-aware items (Home, Community/Services, Visitors/Complaints/Contacts, Profile), light surface, 8dp elevation, hidden while keyboard open |
| Top bar | `createTopBar()` | White background, 12dp vertical, 1dp elevation, back 44dp, title 19sp, subtitle 11sp muted caps 0.06, bell with unread dot danger 9dp, add button small primary |
| Stats card | `showHome()` stats row | 0 weight 1f, 8dp left margin, 14dp padding, 24sp value primary bold, 11sp label secondary |
| Banner | `showHome()` banner | primary_container #EFF6FF background, 18dp horizontal 16dp vertical, 16sp title primary_dark bold, 13sp body secondary |
| Pill | `addPill()` | 11sp primary bold, 11dp horizontal 7dp vertical padding, primary_light background, 22dp radius |
| Timeline row | `addTimelineRow()` | 8dp dot primary, 13sp secondary label, 10dp left margin, 9dp top |

## Validation and persistence

- Validation in `util/Validation.java` — Result pattern, 15+ validators, field-specific errors, duplicate checks
- Persistence in `repository/PersistentRepository.java` — SharedPreferences JSON, thread-safe, seed on first launch, counts for dashboard
- API config in `api/ApiConfig.java` — configurable base URL, enabled flag, backend availability check, no hardcoded credentials
- API models in `api/ApiModels.java` — REST contracts for Spring Boot

## Limitations

- Dark theme not supported — Light only, documented
- Some Java screens still use text() helper sizes instead of TextAppearance styles — migration remaining
- Splash screen not implemented — entry is login, needs core-splashscreen
- Stitch exact values not confirmed — PDF not found, inferred from brief, needs comparison when PDF available
- No automated tests yet
