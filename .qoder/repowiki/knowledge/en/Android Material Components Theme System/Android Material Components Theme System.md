---
kind: frontend_style
name: Android Material Components Theme System
category: frontend_style
scope:
    - '**'
source_files:
    - catroid/src/main/res/values/styles.xml
    - catroid/src/main/res/values/colors.xml
    - catroid/src/main/res/values/dimens.xml
    - catroid/src/main/res/values/style.xml
---

The NeoCatroid Android app uses a centralized Android XML-based styling system built on top of Google's Material Components library. The entire visual identity is defined through Android resource files rather than CSS or Jetpack Compose.

**Core theme foundation:**
- Base theme `Catroid` extends `Theme.MaterialComponents.NoActionBar.Bridge`, establishing Material Design as the visual baseline across all activities and dialogs
- Custom dialog theme `Theme.NeoCatroid.Dialog` overrides Material alert dialogs for consistent appearance
- Dark-first color palette with `app_background_dark=#1C1C1E` and `app_background=#2C2C2E` as primary backgrounds, using `accent=#B0BEC5` (light gray) as the primary accent color

**Design token organization:**
- Colors are systematically organized in `colors.xml` with semantic naming: `toolbar_background`, `dialog_title_and_text_view`, `brick_color_*` (per-brick-category colors), `formula_editor_*` tokens, and `pocketpaint_*` tokens for the embedded Paintroid module
- Dimensions follow Material spacing conventions via `material_design_spacing_small/large/x_large` (8dp/16dp/32dp) plus domain-specific sizes like `brick_height_small/medium/big` (72dp/96dp/120dp)
- Typography uses custom attributes (`x_small/small/medium/large/x_large/xx_large`) mapped to sp values, with `sans-serif` font family applied globally

**Brick category visual system:**
- Each brick category (Motion=blue, Sound=violet, Look=green, Device=gold, etc.) has dedicated drawable backgrounds (`brick_1h_blue`, `brick_2h_violet`, `brick_categories_green`, etc.) defining small/medium/big height variants
- Category headers use matching background drawables (`brick_categories_*`) for instant visual recognition
- This creates a consistent, color-coded block-stacking interface where each programming concept has a distinct visual identity

**Component styling patterns:**
- `BrickContainer.*` styles define the base container for each brick type with consistent padding, gravity, and margin behavior
- `BrickText` styles provide bold, white text with single-line truncation for brick labels
- `FormulaEditorButton` hierarchy establishes a calculator-like interface with compute/delete/category/symbol/number button variants
- `CustomSeekBarStyle` applies accent-colored progress indicators throughout the app

**Flavor-specific theming:**
- Product flavors (catroid, pocketCodeBeta, embroideryDesigner, lunaAndCat, mindstorms, phiro, standalone) override specific resources like app icons and launcher images while inheriting the core Material theme
- PocketPaint sub-app maintains its own `PocketPaintTheme.Base` extending `Theme.MaterialComponents.DayNight.NoActionBar.Bridge` with separate dark/light palettes

**No web technologies used:**
- No CSS, SCSS, Tailwind, or web frameworks are present in the Android UI codebase
- The only HTML/CSS usage is in an embedded Scratch converter tool (`assets/catblocks/index.html`) which loads Bootstrap and MDBootstrap from CDN — this is unrelated to the main app UI
- All styling is declarative XML in Android resource files, with no runtime style manipulation or dynamic theme switching beyond Material's built-in day/night support