# NeoCatroid Design System

## 1. Atmosphere & Identity

NeoCatroid keeps the existing playful, category-coloured block language and adds
quiet material depth only when the user enables **Redrawn block mode**. The
signature is a soft rounded contour around the existing category surface: the
block remains recognisable, but reads as a single polished control.

## 2. Color

### Palette

| Role | Token | Value | Usage |
|------|-------|-------|-------|
| Category surface | existing `brick_*` drawables | category-defined | Existing block colour, never replaced |
| Redrawn border | `redrawn_brick_border` | `#30413D` | Two-dp dark contour |
| Redrawn highlight | `redrawn_brick_highlight` | `#66FFF4E8` | Subtle warm upper rim |
| Smooth toolbar | `smooth_toolbar` | `#8887B8` | Lavender-grey top bar from the Projects reference |
| Smooth background | `smooth_background` | `#F1F0FA` | Soft global screen background |
| Smooth surface | `smooth_card` | `#E8E7F4` | Project rows and cards |
| Smooth text | `smooth_text` | `#484761` | Text on light smooth surfaces |
| Text | existing `solid_white` and category text tokens | existing | Existing block labels and fields |

The redrawn mode is an additive treatment. It must not flatten category
colours into one global fill.

When enabled, the same preference activates the smooth interface palette only
on the Projects screen: lavender-grey toolbar, pale lavender background,
soft surface cards, and restrained borders. The rest of the application keeps
the normal grey palette; the block editor still receives the additive contour.

## 3. Typography

Block typography stays on the existing `BrickText` and `BrickEditText` styles.
The mode changes surfaces only; it does not alter text size, font, or label
wrapping.

## 4. Spacing & Layout

The existing block measurement and padding rules remain authoritative. The
redrawn contour uses `redrawn_brick_corner_radius` and
`redrawn_brick_border_width` resource tokens and never changes measured size.

## 5. Components

### BrickLayout

- **Structure**: existing shared Android `ViewGroup` containing a category icon,
  labels, formula fields, and controls.
- **Variants**: default legacy drawable; redrawn additive contour.
- **Spacing**: unchanged existing `BrickContainer` spacing.
- **States**: off preserves the legacy surface; on applies rounded clipping,
  border, and highlight; touch/selection semantics remain unchanged.
- **Accessibility**: the setting is an explicit CheckBoxPreference; contrast
  and text content are inherited from the current block styles.
- **Motion**: no decorative animation; the visual update is immediate after the
  preference changes.
- **Layout**: reusable flow container; the contour must not affect measurement.

### Smooth application surface

- **Structure**: reusable `SmoothMode` traversal applied by `BaseActivity`.
- **Variants**: normal application palette; smooth lavender palette.
- **States**: preference off restores the normal window/background contract;
  preference on updates only the Projects screen while other screens retain
  the normal grey palette.
- **Accessibility**: dark text tokens are used on the light smooth surfaces;
  the setting remains a standard CheckBoxPreference.

## 6. Motion & Interaction

The mode has no animation. A settings change invalidates attached block views
so the result is visible immediately when returning to the editor.

## 7. Depth & Surface

Strategy: mixed, but restrained. Existing category drawables provide the base
surface; redrawn mode adds a clipped rounded edge, a dark structural border,
and a low-alpha warm highlight. No shadow is added because blocks are dense
and shadows would reduce readability.

## 8. Accessibility Constraints & Accepted Debt

The mode must preserve the existing label contrast, hit targets, formula-field
behaviour, and measured dimensions. There is no accepted accessibility debt
introduced by this visual option.
