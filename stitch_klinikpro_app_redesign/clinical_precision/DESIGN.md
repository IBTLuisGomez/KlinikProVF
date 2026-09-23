---
name: Clinical Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#41474f'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#717880'
  outline-variant: '#c1c7d0'
  surface-tint: '#1d6393'
  primary: '#00436a'
  on-primary: '#ffffff'
  primary-container: '#0f5b8b'
  on-primary-container: '#a2d1ff'
  inverse-primary: '#95ccff'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#004368'
  on-tertiary: '#ffffff'
  tertiary-container: '#005b8b'
  on-tertiary-container: '#9fd1ff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cde5ff'
  primary-fixed-dim: '#95ccff'
  on-primary-fixed: '#001d32'
  on-primary-fixed-variant: '#004a75'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#cce5ff'
  tertiary-fixed-dim: '#93ccff'
  on-tertiary-fixed: '#001d31'
  on-tertiary-fixed-variant: '#004b73'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: '0'
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.005em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  margin: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system establishes an authoritative, dependable, and calm digital environment engineered for clinical practitioners, hospital administrators, and specialized care teams. The aesthetic merges the sterile precision of modern medical instrumentation with the human-centric approachability of high-end SaaS workflows. 

Rooted in a **Corporate / Modern Minimalist** ethos with surgical tactile refinements:
- **Tone:** Methodical, frictionless, reassuring, and technologically sharp.
- **Audience:** Clinicians, nursing supervisors, practice managers, and diagnostic operators who require immediate, low-cognitive-load information retrieval under high-stress conditions.
- **Visual Stance:** Utilitarian clarity balanced by soft, structured geometry. Critical patient telemetry and workflow statuses pop against quiet slate backgrounds, reducing optical fatigue over extended shifts.

## Colors

The palette balances authoritative medical blue tones with functional semantic indicators built to pass WCAG 2.1 AAA accessibility thresholds for critical diagnostic data.

### Foundation & Surfaces
- **Canvas / App Background:** `#F8FAFC` (Slate 50) establishes a low-glare, non-clinical white canvas.
- **Surface Level 1 (Panels / Drawers):** `#F1F5F9` (Slate 100) separates functional operational tiers.
- **Surface Level 0 (Cards / Modals / Worksheets):** `#FFFFFF` pure white ensures optimal contrast for tabular patient charts.
- **Border / Divider Tone:** `#E2E8F0` (Slate 200) for structural framing and subtle cell demarcations.

### Brand & Accents
- **Primary Deep Clinical Teal (`#0F5B8B`):** Anchors global navigation, primary command surfaces, and active state indicators.
- **Tertiary Medical Cyan (`#0284C7`):** Focus triggers, active tabs, secondary links, and procedural data highlights.
- **Secondary Emerald Mint (`#10B981`):** Represents vital stability, system readiness, completed consultations, and normal range telemetry.

### Clinical Semantics & Status Architecture
- **Urgent / Critical (`#EF4444` base, `#FEF2F2` tinted container):** Life-critical alerts, high triage urgency, abnormal lab values.
- **Attention / Waiting (`#F59E0B` base, `#FFFBEB` tinted container):** Pending lab cultures, delayed consults, triage tier 3.
- **Attended / In-Progress (`#0284C7` base, `#F0F9FF` tinted container):** Active observation, rounding in progress.
- **Resolved / Normal (`#10B981` base, `#ECFDF5` tinted container):** Discharged, negative pathology, routine vitals.

## Typography

The typographic hierarchy implements **Plus Jakarta Sans** for structural and section titles to project an approachable yet precise persona, while **Inter** is deployed for dense analytical tables, narrative medical records, and field indicators to safeguard legibility at smaller viewports.

### Numerical Data & Clinical Notation
- All tabular data, numerical vital signs, dosage calculations, and timestamps must use `font-variant-numeric: tabular-nums lining-nums` to ensure absolute alignment across clinical rosters.
- Micro-labels (`label-sm`) require subtle positive tracking (+0.04em) and uppercase casing to ensure immediate clarity when identifying telemetry tags (e.g., `BPM`, `SPO2`, `STAT`).

## Layout & Spacing

A high-density 12-column fluid grid system engineered for multi-monitor workstations, tablets during floor rounds, and handheld triage units.

### Form Factor Adaptations
- **Desktop (>= 1280px):** 12-column grid, `margin`: `2rem`, `gutter`: `1.5rem`. Enables persistent patient summary rails (3 columns) alongside multi-tab treatment charts (9 columns).
- **Tablet / Rounding Device (768px - 1279px):** 8-column grid, `margin`: `1.5rem`, `gutter`: `1rem`. Secondary sidebars collapse into persistent slide-over sheets.
- **Mobile Handheld (< 768px):** 4-column grid, `margin`: `1rem`, `gutter`: `0.75rem`. Stacked visual prioritization with pinned quick-action bottom bars.

### Density Tiers
- **Clinical Dense (Electronic Health Records & Tables):** Enforce `space-xs` (4px) to `space-sm` (8px) padding for high-throughput data intake.
- **Standard (Portals, Settings, Onboarding):** Use `space-md` (16px) to `space-lg` (24px) for comfortable form scanning.

## Elevation & Depth

Visual hierarchy leverages crisp hairline containment accented by ultra-diffused, cool-tinted ambient dropshadows. Floating elements stay anchored without creating muddy layers over clinical forms.

### Elevation Levels
- **Level 0 (Flat Baseline):** Pure `#FFFFFF` surfaces bounded by a crisp `1px solid #E2E8F0` border. No shadow. Used for resting cards, standard table layouts, and embedded panels.
- **Level 1 (Interactive / Hover):** `#FFFFFF` with `box-shadow: 0 4px 12px -2px rgba(15, 91, 139, 0.06), 0 2px 4px -1px rgba(15, 91, 139, 0.04)` and `#CBD5E1` border transition. Used for hoverable patient rows, selectable cards, and inline dropdown menus.
- **Level 2 (Overlays / Drawers / Flyouts):** `box-shadow: 0 12px 28px -4px rgba(15, 23, 42, 0.08), 0 4px 8px -2px rgba(15, 23, 42, 0.04)`. Used for patient quick-view drawers and contextual action popovers.
- **Level 3 (Modals / Emergency Interrupts):** `box-shadow: 0 24px 48px -12px rgba(15, 23, 42, 0.18)`. Framed by a subtle backdrop blur (`backdrop-filter: blur(4px)`) over an alpha slate scrim (`rgba(15, 23, 42, 0.45)`).

## Shapes

The design system implements balanced, modern geometry to soften standard clinical austerity without sacrificing density.

- **Base Radius (`0.5rem` / 8px):** Standard inputs, small badges, action dropdowns, and status tags.
- **Large Radius (`rounded-lg` - `1rem` / 16px):** Core diagnostic cards, vitals summary pods, interactive charts, and contextual drawers.
- **Extra Large Radius (`rounded-xl` - `1.5rem` / 24px):** Primary modals, patient intake banners, and system onboarding containers.
- **Full Radius (Pill):** Categorical chips, priority filters, and presence indicator dots.

## Components

### Buttons
- **Primary:** Solid `#0F5B8B` background, `#FFFFFF` text, `0.5rem` corner radius, medium font weight (`label-md`). Hover elevates to `#0A456B`. Active states introduce an internal inset ring.
- **Secondary:** Surface `#FFFFFF`, border `1px solid #CBD5E1`, text `#0F5B8B`. Hover shifts background to `#F8FAFC` and border to `#0F5B8B`.
- **Tertiary / Destructive:** High-triage actions leverage `#EF4444` background with white text, or ghost variants with `#EF4444` text and `#FEF2F2` hover states.

### Status Badges
Pill-shaped indicators featuring a 6px status beacon dot paired with uppercase micro-copy (`label-sm`):
- **Urgent:** Crimson background `#FEF2F2`, text `#991B1B`, border `#FEE2E2`.
- **Waiting:** Amber background `#FFFBEB`, text `#92400E`, border `#FEF3C7`.
- **Attended:** Sky background `#F0F9FF`, text `#075985`, border `#E0F2FE`.
- **Completed:** Mint background `#ECFDF5`, text `#065F46`, border `#D1FAE5`.

### Input Fields
- Standard height of 40px for desktop workflows, 44px for touch tablets.
- Background: `#FFFFFF`, border: `1px solid #CBD5E1`, radius: `0.5rem`.
- Focus state: Border transitions to `#0284C7` accompanied by an accessible glow ring: `box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15)`.

### Cards & Patient Data Containers
- Resting state: Crisp `#FFFFFF` surface enclosed by `1px solid #E2E8F0`, rounded to `1rem` (`rounded-lg`).
- Header bands are partitioned by a horizontal rule (`#F1F5F9`) with dedicated slots for clinical tags, patient MRNs, and time-elapsed stamps.

### Checkboxes & Radios
- Checkboxes utilize a `0.25rem` radius; radios use full circles.
- Unchecked: `1.5px solid #94A3B8` on `#FFFFFF`.
- Checked: `#0F5B8B` background with crisp white iconography, scaling cleanly down to 16px.

### Clinical Telemetry Tiles (Domain Component)
- Compact pods dedicated to vital stats (e.g., BP, Pulse, Temp). Displays value in `headline-lg` tabular figures, unit in `label-sm` slate text, and integrated directional trend arrows indicating deltas relative to baseline measurements.