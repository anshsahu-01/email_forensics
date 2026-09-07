---
version: alpha
name: India.gov.in
description: "A minimal, text-first placeholder system observed from an access-denied response page: stark black-on-white typography set in a classic serif, with no chrome, elevation, or interactive color signals beyond the basic body copy and heading — evidence supports only foundational text and spacing tokens rather than a full visual system."
colors:
  primary: "#000000"
typography:
  h1:
    fontFamily: Times New Roman
    fontSize: 32px
    fontWeight: 700
    lineHeight: 1.5
    letterSpacing: normal
  body:
    fontFamily: Times New Roman
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: normal
spacing:
  sm: 16px
  md: 21.44px
components:
  body-text:
    textColor: "{colors.primary}"
    typography: body
  heading:
    textColor: "{colors.primary}"
    typography: h1
    padding: 16px
---

# India.gov.in

## Overview

India.gov.in returned an **Access Denied** response rather than the live portal — what was captured is a bare server-rendered error page, not the government portal's actual design system. There is no logo, no navigation, no color system, and no component library on this page; the only signals available are default browser typographic rendering (serif body copy and a bold heading) on a plain white canvas. Any "design system" derived here describes the fallback presentation of an error state, not the brand.

Density is minimal by necessity: a single heading and a paragraph of body text, left-aligned, stacked with default browser spacing. Hierarchy — to the extent it exists — is made by one lever only: font size and weight contrast between the {typography.h1} heading (32px/700) and the {typography.body} copy (16px/400). There is no color, elevation, iconography, or layout system to reinforce it.

Because this page is a blocked-request artifact (likely a WAF or server-level denial, not an authored page), treat every token below as a description of "what an unstyled HTML error document looks like in a browser," not as evidence of India.gov.in's actual visual identity. Any real rebuild of the portal will need a fresh capture once access is unblocked.

**Key Characteristics:**
- Zero brand chrome: no logo, nav, footer, or imagery present in the capture.
- Monochrome only — pure black text ({colors.primary} — #000000) on the browser's default white background.
- Single serif family, {typography.h1} and {typography.body} both set in Times New Roman, the browser default.
- Hierarchy carried entirely by size/weight contrast (32px/700 vs 16px/400), not color or layout.
- No elevation, no shadows, no borders, no radii — completely flat.
- No grid or multi-column layout; content reflows as plain text between viewports.
- Spacing is limited to two observed values ({spacing.sm} 16px, {spacing.md} 21.44px), consistent with default UA paragraph/heading margins rather than an authored scale.
- This is an error-page artifact, not a validated brand system — confidence throughout this document is low.

## Colors

The captured page exposes exactly one color in use, applied only to text — there is no surface, border, or interactive palette to document because none rendered.

### Text
- **Ink Black** ({colors.primary} — #000000): the sole color token, applied to both the {typography.h1} heading and {typography.body} paragraph text via the {components.body-text} and {components.heading} components. Used at 100% opacity against the browser's default white canvas.

### Surface
- No surface token was captured. The visible background is the browser/user-agent default white — it was not observed as an authored value and is not tokenized here.

### Hairlines & Borders
- None present. No borders, dividers, or rules were observed anywhere on the page.

### Gradients
- No gradients are used anywhere on this page — the page has no color system beyond flat black text.

### Dark Mode
- No dark-mode block was observed or can be inferred; this single-page error capture gives no evidence of theming behavior. Do not assume dark-mode support exists.

## Typography

### Font Family
- **Times New Roman** — the only family observed, used for both the heading and body copy ({typography.h1}, {typography.body}). This is the browser's default serif fallback, not a chosen brand typeface; there is no evidence of a webfont being loaded.

### Hierarchy

| Token | Size | Weight | Line Height | Letter Spacing | Use |
|---|---|---|---|---|---|
| {typography.h1} | 32px | 700 | 1.5 | normal | Page heading ("Access Denied" style message) |
| {typography.body} | 16px | 400 | 1.5 | normal | Body/error message copy |

### Principles
- Only two weights exist — 400 (regular) and 700 (bold) — with nothing in between; there is no medium (500) or light weight in evidence, so an agent should not introduce one.
- Letter-spacing is uniformly "normal" (0) at both levels — no tracking adjustments were observed, consistent with unstyled default rendering.
- Line-height is fixed at 1.5 for both heading and body, an unusually loose ratio for a 32px heading, again consistent with a browser default rather than a tuned type ramp.
- Only two type sizes exist in the entire ramp; there is no intermediate scale (no h2–h6, no caption/label sizes) to draw from.

### Note on Font Substitutes
Times New Roman here is the operating-system default serif, not a licensed brand font — it should not be treated as an intentional identity choice. If rebuilding a real India.gov.in interface, do not carry this typeface forward as "the brand font"; instead treat it purely as evidence of an unstyled fallback page, and default to a system UI sans-serif stack once real portal pages are captured.

## Layout

### Spacing System
Only two spacing values were observed: {spacing.sm} (16px), used as the {components.heading} padding, and {spacing.md} (21.44px), likely reflecting a default paragraph margin (roughly 1.34em at 16px body size). These read as browser user-agent defaults rather than an authored 4/8pt spacing scale — no consistent base unit or multiplier pattern is evident.

### Grid & Container
No grid, container max-width, or column system was observed. Content is a single unconstrained text flow with default document margins; there is no evidence of a max-width wrapper, breakpoint-driven column count, or gutter system.

### Whitespace Philosophy
Whitespace is entirely incidental — it comes from default heading/paragraph vertical margins ({spacing.sm}, {spacing.md}), not from a deliberate rhythm. There is no evidence of intentional whitespace-as-hierarchy strategy; the page is simply two blocks of text stacked with browser-default gaps.

## Elevation & Depth

| Level | Treatment | Use |
|---|---|---|
| Flat | No shadow, no border, no background layering | The entire page — heading and body text sit directly on the plain white canvas |

**Shadow philosophy.** There is no elevation system: this page has zero shadow, blur, or border-based depth cues of any kind. This is not a stylistic choice toward flatness so much as the total absence of a design layer — the response is raw unstyled HTML. Do not introduce shadows or z-axis layering when rebuilding from this evidence; the flatness observed here reflects an error page, not a documented "flat design" strategy.

## Shapes

### Border Radius Scale

| Token | Value | Use |
|---|---|---|
| — | none observed | No rounded corners, cards, buttons, or containers exist on this page |

No geometric shape language can be documented — there are no boxes, cards, buttons, or badges rendered on this page, only running text. Nothing pill-shaped or circular was observed. Any radius/shape system for the real portal must come from a fresh, unblocked capture.

## Components

No navigation, buttons, cards, forms, badges, tables, or footer exist in this capture — the page contains exactly two content elements, both text-based.

### Text
- **`heading`** — the error page's headline, set in {typography.h1} (32px/700/1.5 line-height, Times New Roman), colored {colors.primary} (#000000), with {components.heading} applying 16px of padding around it. Functions as the sole "title" element on the page.
- **`body-text`** — the explanatory error message, set in {typography.body} (16px/400/1.5 line-height, Times New Roman), colored {colors.primary} (#000000) via {components.body-text}. No links, lists, or interactive text were observed within it.

### Navigation, Buttons, Cards, Inputs, Badges, Tables, Footer
None of these component categories are present in the evidence. The captured response is a server-level "Access Denied" page with no header bar, no logo, no CTA button, no footer link columns, and no legal text block — landmark measurements for navbar/footer could not be extracted because no such landmarks exist in the DOM that was captured.

## Do's and Don'ts

### Do
- Do set all text in **Times New Roman** ({typography.h1}, {typography.body}) if reproducing this exact error-page artifact — don't substitute a different family for it.
- Do use only the two observed weights, 400 and 700 — regular for {typography.body}, bold for {typography.h1}.
- Do keep the {components.heading} padding at 16px ({spacing.sm}) if replicating this specific presentation.
- Do treat {colors.primary} (#000000) as the only sanctioned text color observed — don't introduce accent or brand colors not present in evidence.
- Do treat this page as a placeholder/error artifact, not the brand's design language, before applying any of these tokens to real product surfaces.

### Don't
- Don't add drop shadows or elevation — none exist in the source; the page conveys everything through flat black-on-white contrast.
- Don't introduce a second typeface — Times New Roman is the only family in evidence across both heading and body.
- Don't invent a color palette, surface tokens, or interactive states — only one flat black text color was ever observed.
- Don't add border radii, cards, or shaped containers — no geometric system exists in this capture.
- Don't assume this represents India.gov.in's real portal design — rebuild from a fresh capture of the live site once accessible rather than extending this error-page system.

## Responsive Behavior

Only two viewports (desktop and mobile) were captured, and both show the identical unstyled error page: plain left-aligned text that reflows naturally with viewport width. No layout stacking, column collapsing, or breakpoint-driven adaptation is visible, because there is no multi-element layout to adapt — just a heading and a paragraph of text. No touch-target sizing can be assessed since no interactive elements (buttons, links, inputs) appear in either viewport. This finding should be treated as confirming the page is content-agnostic to viewport width rather than a documented responsive strategy; confidence here is necessarily low given the minimal, non-representative source material.

## Iteration Guide

1. Do not extend this document as if it were India.gov.in's production design system — it documents an Access Denied error response, not the government portal. Re-capture the live site before building real UI.
2. If asked to reproduce this exact error state, keep typography confined to {typography.h1} and {typography.body}, both in Times New Roman, with no additional weights or sizes introduced.
3. Keep {colors.primary} (#000000) as the only color token in play; do not add surface, border, or accent colors that have no grounding in the evidence.
4. Do not add shadows, radii, or elevation layers when working within this token set — the absence of these systems is itself the accurate representation of the source.
5. Preserve the two spacing values as-is ({spacing.sm} 16px on {components.heading}, {spacing.md} ~21.44px implied paragraph spacing) rather than generalizing them into a broader spacing scale not supported by evidence.
6. Flag to stakeholders immediately that a real design system extraction requires successful access to India.gov.in — this artifact cannot inform navigation, forms, cards, or any interactive component work.
7. When more pages become available, rebuild colors, typography, layout, elevation, shapes, and components sections from scratch rather than patching this minimal placeholder.

## Known Gaps

- The target page returned **Access Denied**; the entire capture is a single blocked-request error document, not the actual India.gov.in portal — virtually nothing here reflects real brand identity.
- Only one page was captured (`https://www.india.gov.in`, status CAPTURED but content-blocked), so there is no multi-page evidence for navigation, footer, forms, or component variation.
- No colors beyond black text were observed — surface/background, border, and interactive/link colors are entirely unknown.
- No radii, shadows, or borders were observed at all — it's unclear whether this reflects the real site's flat aesthetic or simply the absence of any styling in the error response.
- No hover, focus, active, or disabled states could be observed — there are no interactive elements in the capture at all.
- No animation, transition, or motion behavior could be assessed.
- No authenticated or logged-in surfaces, forms, or government-service UI patterns are represented anywhere in this evidence.
- Landmark data (navbar height, footer structure, sticky headers) is empty because no such landmarks exist in the captured DOM.
- No dropped/grounded-out token values were reported by the extraction pipeline, meaning the sparseness above reflects genuinely minimal source content, not filtering.
