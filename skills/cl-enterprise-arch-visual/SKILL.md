---
name: architecture-diagram
description: Generates polished, presentation-grade architecture and concept diagrams as PNG images — the kind that go on a slide in front of executives. Use this skill whenever the user asks for a diagram, architecture picture, system flow, data flow, reference architecture, "how it works" visual, capability map, layered stack, process pipeline, comparison matrix, or wants an existing sketch or description "made professional" or "turned into an image". Trigger it even when the user just describes components and says "show me this" or "put this on a slide" without using the word diagram. Also use it when the user shares an example diagram image and asks for something in that style.
---

# Architecture Diagram Generator

Produces a consistent house style: dark-blue headline, pastel component
cards with matching hairline borders, white grouping panels, a bold blue
protocol pill for the centrepiece, curved connector lines, and a footer
band that states the takeaway. Output is a retina PNG that can be dropped
straight onto a slide.

The style comes from one stylesheet. Compose HTML using its classes and
the result will look right; hand-write bespoke CSS and it won't. That is
the whole trick.

## Workflow

1. **Plan the story.** Every good diagram answers one sentence. Write that
   sentence first — it becomes the footer band. Then pick a layout pattern
   (below) and list the columns/blocks and what goes in each.
2. **Write out the three asset files** — `theme.css`, `connect.js` and
   `render.py` — from the [Assets](#assets) section at the bottom of this
   file, into the working directory. Copy them verbatim; do not retype or
   "improve" them. Skip any that already exist.
3. **Write `diagram.html`** using the classes in the
   [Component catalogue](#component-catalogue). Compose from those blocks
   rather than writing new CSS.
4. **Render:** `python3 render.py diagram.html out.png`
   (2× retina by default; `--scale 3` for print, `--pdf` for a vector copy).
5. **Look at the PNG.** This step is not optional — `view` the rendered
   image. Layout bugs are invisible in HTML and obvious in the picture:
   text overflowing a card, a column running off the bottom, a connector
   pointing at the wrong box, one card twice the height of its neighbours.
   Fix and re-render until it looks deliberate.
6. **Hand it over.** Tell the user the PNG path so they can open it in the
   VS Code image preview. Keep `diagram.html` alongside it — that is the
   editable source for later tweaks.

## Layout patterns

Pick one; don't invent a fifth without reason.

**Flow (left → right).** Sources → processing layer → protocol/interface →
consumer. The default for architecture. See `assets/example-flow.html`.

**Layered stack (top → bottom).** Presentation / application / data /
infrastructure. Use full-width `.panel` rows stacked in a `.col`, each with
a `.panelhead` on the left and `.card`s in a `.row` on the right.

**Hub and spoke.** One `.pill` or `.hero` centred, satellites around it,
wired with `data-wire`. Good for "everything goes through X".

**Pipeline.** A single `.row` of stages separated by `<span class="arrow right"></span>`,
with a detail `.panel` underneath. Good for process and lifecycle stories.

## Design rules that keep it looking professional

**Colour carries meaning, not decoration.** Assign one accent family per
category and stay consistent — if databases are `c-blue` in one column they
are `c-blue` everywhere. Cap it at 5 accent families on a single diagram;
past that it reads as clip art. Blue (`--brand`) is reserved for the
centrepiece and connectors, so an ordinary card should rarely be `c-blue`
if a blue pill is already in the frame.

**One idea per card.** A card is a heading of 1–4 words plus a caption of
3–7 words. If a caption needs a comma-heavy list, it belongs in the footer
or a sub-panel, not the card.

**Density is the enemy.** Aim for 4–6 items per column, 4–5 rows in a
matrix. If there is more content than that, either split into two diagrams
or promote the detail into a second-level `.subpanel` that only summarises.

**Never override the type scale.** Sizes in `theme.css` are tuned to sit on
a 16:9 slide viewed from across a room. Shrinking font-size to cram in more
text is the single fastest way to make output look amateur — cut words
instead. The one acceptable exception is a small, deliberate step-down on a
repeated secondary card (as the API Adapter cards do in the example).

**Alignment reads as competence.** Parallel columns should start and end on
the same line. Wrap column contents in a `.panel grow` and add `.spread`
when you want children distributed evenly down the full height.

**British/US spelling and terminology should match the user's own.** If
they wrote "Authorisation", keep it.

## Content the user hasn't specified

Users usually give components, not copy. Fill the gaps in-house style
rather than leaving blanks:

- **Headline:** `Subject: What It Does` — e.g. "Payments Platform:
  Transaction Flow". Sentence case, no full stop.
- **Tagline:** three short declaratives — "One protocol. Consistent access.
  Unified security."
- **Column heads:** upper case, 1–2 words, with a lowercase qualifier line.
- **Footer:** the takeaway sentence from step 1, in a caps blue heading plus
  one explanatory line of ~30 words.

Invented labels should be plausible and generic. Don't invent specific
vendor names, numbers, or compliance claims the user hasn't mentioned —
those are the things that get a slide challenged in a meeting.

## Fonts

The stack is `Lato, Segoe UI, Carlito, Calibri, Trebuchet MS`. Lato and
Segoe UI are unlikely to be installed in a sandbox; Carlito is the
metric-compatible Calibri substitute that renders there and looks correct.
Do not swap in a different family — the humanist sans is a large part of
why the style reads as corporate-polished rather than generic-web.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Content spills past the bottom edge | Too many items. Cut, or move to `.canvas.ratio-4-3` / `.canvas.tall`. Don't shrink type. |
| Card fill is white when an accent was set | Accent class must be on the same element as `.card` (`class="card c-green"`), not a parent. |
| Connector lines missing | `connect.js` not next to the HTML, or the `data-wire` target id doesn't exist. |
| Connectors cross over cards | Reorder so wired pairs are roughly opposite each other, or set `data-wire-side="lr"`. |
| Columns are different heights | Add `grow` to the panel in each column. |
| Big empty gap below a panel's content | The row is taller than what's in it. Use `.panel.fit` with `.row.top`, add a third element to the row, or move content up from another section. |
| Icons render as empty boxes | The `<symbol>` sprite must be inline in the same HTML file; external `<use href="file.svg#id">` is blocked under `file://`. |
| `playwright` not installed | `pip install playwright && playwright install chromium`. On a machine with Chrome already, `playwright install chromium` alone is enough. |

---

# Component catalogue

Copy-paste markup for every block in `theme.css`.

### Page skeleton

```html
<!DOCTYPE html>
<html lang="en">
<head><meta charset="utf-8"><link rel="stylesheet" href="theme.css"></head>
<body>
  <!-- paste the icon sprite here -->
  <div class="canvas">
    <div class="head">…</div>
    <div class="stage">…</div>
    <div class="footer">…</div>
  </div>
  <script src="connect.js"></script>
</body>
</html>
```

Canvas sizes: default `canvas` is 1600×900 (16:9). Add `ratio-4-3`
(1400×1050), `ratio-1-1` (1200×1200) or `tall` (1200×1600) for denser
content. The renderer captures the `.canvas` element only.

---

### Headline

```html
<div class="head">
  <h1>Payments Platform: Transaction Flow</h1>
  <p class="tagline">One ledger. Real-time settlement. Full auditability.</p>
</div>
```

---

### Columns and panels

`.stage` is a flex row. Column widths: `.col.narrow` (250px fixed),
`.col.mid` (300px fixed), `.col.wide` (fills remaining space).

```html
<div class="stage">
  <div class="col narrow">
    <div class="colhead"><h2>DATA SOURCES</h2><p>Heterogeneous by nature</p></div>
    <div class="panel grow col spread" style="padding:14px">
      <!-- cards -->
    </div>
  </div>
</div>
```

`grow` makes the panel fill the column height so parallel columns align;
`spread` distributes children evenly down that height.

Panel with a heading and icon:

```html
<div class="panel grow">
  <div class="panelhead">
    <span style="color:var(--brand);display:flex"><svg width="26" height="26"><use href="#i-shield"/></svg></span>
    <span class="txt"><h3>UNIFIED API LAYER</h3><p>Normalise. Authorise. Protect.</p></span>
  </div>
  …
</div>
```

Variants: `.panel.soft` (faint grey interior), `.panel.flush` (tighter
padding). Nest a `.subpanel` inside a panel for a second level:

```html
<div class="subpanel">
  <div class="sptitle">Consistent Authorization &amp; Security<br>
    <span style="font-weight:400;font-size:15px">Enforced at the API Layer</span></div>
</div>
```

`<div class="rail"></div>` is a vertical dashed divider between two
sub-columns inside a panel.

---

### Cards

The workhorse. Accent class goes on the card itself.

```html
<div class="card c-blue">
  <span class="ic"><svg><use href="#i-database"/></svg></span>
  <span class="txt"><h4>Database</h4><p>SQL / NoSQL data sources</p></span>
</div>
```

- `.card.tight` — reduced padding for repeated secondary cards
- `.card.plain` — white fill, keeps the neutral border
- `.card.stackv` — icon above centred text, for a row of equal stages

An emoji works in place of the `<svg>` if a suitable icon isn't in the
sprite: `<span class="ic">📊</span>`.

---

### Feature cards and hero

Feature cards carry a circular icon badge — use them for capability lists.

```html
<div class="featurecard c-green">
  <span class="badge"><svg><use href="#i-pencil"/></svg></span>
  <span class="txt"><h4>Act &amp; Automate</h4><p>Update, create and orchestrate workflows</p></span>
</div>
```

Hero = large circular focal icon, for the actor a diagram is about:

```html
<div class="hero c-teal"><svg><use href="#i-robot"/></svg></div>
```

---

### Matrix table

Keep to 4–5 data rows and 4–5 columns.

```html
<div class="subpanel">
  <table class="matrix">
    <caption>UNIFIED ACCESS MATRIX</caption>
    <thead><tr><th>Identity / Role</th><th>File Systems</th><th>Database</th><th>APIs</th></tr></thead>
    <tbody>
      <tr><th>Admin</th><td class="yes">✓</td><td class="yes">✓</td><td class="no">✕</td></tr>
      <tr><th>Analyst</th><td class="no">✕</td><td class="yes">✓</td><td class="no">✕</td></tr>
    </tbody>
  </table>
</div>
```

`✓`/`✕` in `.yes`/`.no` render as crisp green/red glyphs. `✅`/`❌` emoji
also work if a chunkier look is wanted, but they ignore the text colour.

---

### Checklist rows

For control lists — auth, compliance, guarantees.

```html
<div class="checklist">
  <div class="checkrow"><span class="ic"><svg><use href="#i-shield"/></svg></span>
    <span><b>Authentication:</b> MFA</span></div>
  <div class="checkrow"><span class="ic"><svg><use href="#i-shield"/></svg></span>
    <span><b>Authorisation:</b> RBAC / ABAC</span></div>
</div>
```

---

### Protocol pill and arrows

The centrepiece. One per diagram — its impact comes from being the only
saturated blue object.

```html
<div class="pillblock">
  <div class="row center" style="gap:12px">
    <span style="color:var(--brand);display:flex"><svg width="38" height="38"><use href="#i-shield"/></svg></span>
    <div class="kicker" style="text-align:left">ONE STANDARD.<br>ONE PROTOCOL.<br>EVERY ACCESS.</div>
  </div>
  <div class="pillrow">
    <span class="arrow left"></span>
    <span class="pill">MCP PROTOCOL</span>
    <span class="arrow right"></span>
  </div>
  <div class="note">Secure, standardised communication<br>for data access and operations</div>
</div>
```

Arrows are standalone too — `<span class="arrow right"></span>` between
pipeline stages. `.pill.sm` is the smaller variant for inline stage labels.

---

### Connector wires

`connect.js` draws a curved line from any element with `data-wire` to the
element ids listed. Give the source an `id` as well if it is also a target.

```html
<div class="card c-green" id="src-fs" data-wire="adapter-1">…</div>
<div class="card plain"   id="adapter-1">…</div>
```

Optional attributes on the source element:

| Attribute | Default | Purpose |
|---|---|---|
| `data-wire-color` | `#C3D2E6` | line colour — use the category accent to colour-code paths |
| `data-wire-width` | `1.6` | stroke width |
| `data-wire-arrow` | `false` | `"true"` adds an arrowhead at the target |
| `data-wire-side` | `auto` | force edges: `lr`, `rl`, `tb`, `bt` |

Multiple targets: `data-wire="a,b,c"`. Lines sit behind cards, so a wire
passing under a box looks intentional rather than broken.

---

### Footer band

States the takeaway. Every diagram should have one.

```html
<div class="footer">
  <span class="ic"><svg><use href="#i-shield"/></svg></span>
  <div>
    <h3>UNIFIED SECURITY. UNIFIED ACCESS. UNIFIED EXPERIENCE.</h3>
    <p>The API layer enforces consistent authorisation through a unified access
       matrix, enabling the agent to reach every source through one protocol.</p>
  </div>
</div>
```

---

### Utilities

`center` · `caps` · `small` · `strong` · `accent` · `grow` · `spacer` ·
`row` · `row center` · `col`

Legend and tag chips:

```html
<div class="legend">
  <span><i class="dot" style="background:#4C9A4A"></i> Permitted</span>
  <span><i class="dot" style="background:#D2483F"></i> Denied</span>
</div>
<span class="tag c-orange">Beta</span>
```

---

### Icon sprite

Paste inside `<body>`, before `.canvas`. Trim to the symbols used.

```html
<svg style="display:none" xmlns="http://www.w3.org/2000/svg">
  <symbol id="i-shield" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2 4 5v6.5c0 4.8 3.4 9.1 8 10.5 4.6-1.4 8-5.7 8-10.5V5l-8-3Z"/><path fill="#fff" d="M11 11h2v4h-2z"/><path fill="#fff" d="M12 7.6a2.2 2.2 0 0 0-2.2 2.2v1h1.5v-1a.7.7 0 0 1 1.4 0v1h1.5v-1A2.2 2.2 0 0 0 12 7.6Z"/></symbol>
  <symbol id="i-folder" viewBox="0 0 24 24"><path fill="currentColor" d="M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6Z"/></symbol>
  <symbol id="i-database" viewBox="0 0 24 24"><ellipse cx="12" cy="6" rx="8" ry="3" fill="currentColor"/><path fill="currentColor" d="M4 9.5c0 1.7 3.6 3 8 3s8-1.3 8-3V13c0 1.7-3.6 3-8 3s-8-1.3-8-3V9.5Z"/><path fill="currentColor" d="M4 16c0 1.7 3.6 3 8 3s8-1.3 8-3v3c0 1.7-3.6 3-8 3s-8-1.3-8-3v-3Z"/></symbol>
  <symbol id="i-braces" viewBox="0 0 24 24"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M9 4C6.5 4 7.5 10 4.5 12c3 2 2 8 4.5 8M15 4c2.5 0 1.5 6 4.5 8-3 2-2 8-4.5 8"/></symbol>
  <symbol id="i-cloud" viewBox="0 0 24 24"><path fill="currentColor" d="M6.5 19a4.5 4.5 0 0 1-.5-8.97 6 6 0 0 1 11.6 1.6A3.9 3.9 0 0 1 17.5 19h-11Z"/></symbol>
  <symbol id="i-doc" viewBox="0 0 24 24"><path fill="currentColor" d="M6 2h8l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z"/><path fill="#fff" d="M8 12h8v1.6H8zm0 3.4h6V17H8z"/></symbol>
  <symbol id="i-gear" viewBox="0 0 24 24"><path fill="currentColor" d="m19.4 13-.1-1 2-1.5-2-3.4-2.3.9a7.4 7.4 0 0 0-1.7-1L14.9 4h-4l-.4 2.4c-.6.3-1.2.6-1.7 1L6.5 6.6l-2 3.5L6.6 12l-.1 1 .1 1-2 1.5 2 3.4 2.3-.9c.5.4 1.1.8 1.7 1l.4 2.5h4l.4-2.4c.6-.3 1.2-.6 1.7-1l2.3.9 2-3.5-2-1.5.1-1Zm-6.5 3.2a3.2 3.2 0 1 1 0-6.4 3.2 3.2 0 0 1 0 6.4Z"/></symbol>
  <symbol id="i-robot" viewBox="0 0 24 24"><rect x="4" y="7" width="16" height="12" rx="3" fill="currentColor"/><circle cx="9" cy="12.5" r="1.8" fill="#fff"/><circle cx="15" cy="12.5" r="1.8" fill="#fff"/><path fill="currentColor" d="M11 2.5h2V6h-2z"/><circle cx="12" cy="2.5" r="1.6" fill="currentColor"/></symbol>
  <symbol id="i-search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="2.4"/><path stroke="currentColor" stroke-width="2.6" stroke-linecap="round" d="m16 16 4.5 4.5"/></symbol>
  <symbol id="i-pencil" viewBox="0 0 24 24"><path fill="currentColor" d="M4 17.2 15.3 5.9l2.8 2.8L6.8 20H4v-2.8Zm13-13.3 2.2-2.2 2.8 2.8-2.2 2.2-2.8-2.8Z"/></symbol>
  <symbol id="i-chat" viewBox="0 0 24 24"><path fill="currentColor" d="M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9l-5 4V5Z"/><circle cx="9" cy="9.5" r="1.2" fill="#fff"/><circle cx="12.8" cy="9.5" r="1.2" fill="#fff"/><circle cx="16.6" cy="9.5" r="1.2" fill="#fff"/></symbol>
  <symbol id="i-lock" viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="11" rx="2.4" fill="currentColor"/><path fill="none" stroke="currentColor" stroke-width="2.2" d="M8.2 10V7.6a3.8 3.8 0 0 1 7.6 0V10"/></symbol>
  <symbol id="i-users" viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.4" fill="currentColor"/><path fill="currentColor" d="M2.6 19.4c0-3.3 2.9-5.4 6.4-5.4s6.4 2.1 6.4 5.4v.6H2.6v-.6Z"/><circle cx="17.4" cy="9" r="2.6" fill="currentColor" opacity=".55"/><path fill="currentColor" opacity=".55" d="M15 14.2c3.2-.6 6.4 1 6.4 4.2v1.6H18c0-2.4-1.2-4.4-3-5.8Z"/></symbol>
  <symbol id="i-chart" viewBox="0 0 24 24"><rect x="4" y="12" width="4" height="8" rx="1" fill="currentColor"/><rect x="10" y="7" width="4" height="13" rx="1" fill="currentColor"/><rect x="16" y="3" width="4" height="17" rx="1" fill="currentColor"/></symbol>
  <symbol id="i-plug" viewBox="0 0 24 24"><path fill="currentColor" d="M8 2h2v5H8zm6 0h2v5h-2z"/><path fill="currentColor" d="M6 8h12v3a6 6 0 0 1-4.6 5.8V22h-2.8v-5.2A6 6 0 0 1 6 11V8Z"/></symbol>
  <symbol id="i-bolt" viewBox="0 0 24 24"><path fill="currentColor" d="M13.5 2 5 13.4h5.3L9.6 22 19 10.2h-5.6L13.5 2Z"/></symbol>
  <symbol id="i-server" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="6" rx="1.6" fill="currentColor"/><rect x="3" y="14" width="18" height="6" rx="1.6" fill="currentColor"/><circle cx="7" cy="7" r="1.1" fill="#fff"/><circle cx="7" cy="17" r="1.1" fill="#fff"/></symbol>
</svg>
```

Icon names: `i-shield · i-folder · i-database · i-braces · i-cloud · i-doc ·
i-gear · i-robot · i-search · i-pencil · i-chat · i-lock · i-users ·
i-chart · i-plug · i-bolt · i-server`

---

### Accent families

| Class | Fill | Border | Icon | Suggested use |
|---|---|---|---|---|
| `c-green` | `#EDF7ED` | `#BADCB7` | `#4C9A4A` | files, success, storage |
| `c-blue` | `#EAF2FC` | `#B4CDEC` | `#2F6FC4` | databases, core services |
| `c-orange` | `#FDF4E6` | `#EFCD98` | `#CF8A22` | APIs, integrations |
| `c-purple` | `#F2EDFB` | `#C8B7E8` | `#7B5EC4` | SaaS, third parties |
| `c-teal` | `#E9F4F8` | `#AED2E0` | `#3C8AA8` | documents, analytics |
| `c-slate` | `#F3F6FA` | `#D4DCE8` | `#5B6B92` | neutral / infrastructure |
| `c-rose` | `#FCEEEC` | `#EDBDB6` | `#C4564A` | risk, denied, legacy |

Each sets `--bg`, `--bd` and `--ac`, so the same class colours a card fill,
its border and its icon in one go.


---

# Assets

Write these three files into the working directory before rendering.
They are the skill — the house style lives entirely in `theme.css`, so
copy them byte for byte rather than paraphrasing.

### `theme.css`

```css
/* ============================================================
   Enterprise Architecture Diagram — Design System
   16:9 slide-style technical diagrams.
   Every visual decision lives here. Author HTML, not CSS.
   ============================================================ */

:root {
  /* --- Type ---------------------------------------------- */
  --font: "Lato", "Segoe UI", "Carlito", "Calibri", "Trebuchet MS",
          "Liberation Sans", Arial, sans-serif;

  /* --- Core blues ---------------------------------------- */
  --title:      #1F4E9C;   /* main headline                   */
  --heading:    #2F5FA8;   /* column / panel headings         */
  --brand:      #1A5FC8;   /* pill, arrows, active accents    */
  --brand-dark: #14479A;

  /* --- Neutrals ------------------------------------------ */
  --ink:      #26344A;     /* card titles                     */
  --body:     #465469;     /* body copy                       */
  --muted:    #6B7B92;     /* captions, sub-labels            */
  --rule:     #D9E2EF;     /* table + divider lines           */
  --panel-bd: #E4EAF3;     /* panel outline                   */
  --canvas:   #FFFFFF;
  --panel:    #FFFFFF;
  --wash:     #F7F9FC;     /* faint panel interior            */

  /* --- Semantic states ----------------------------------- */
  --yes: #3F9A4F;
  --no:  #D2483F;

  /* --- Geometry ------------------------------------------ */
  --r-lg: 14px;
  --r-md: 10px;
  --r-sm: 7px;
  --shadow: 0 1px 3px rgba(31, 78, 156, .06);
}

/* Defaults first, so an accent class always overrides them. */
.card, .panel, .featurecard, .hero, .tag { --bg:#FFFFFF; --bd:var(--panel-bd); --ac:var(--heading); }

/* --- Accent families. Apply to .card / .panel as .c-NAME --- */
.c-green  { --bg:#EDF7ED; --bd:#BADCB7; --ac:#4C9A4A; }
.c-blue   { --bg:#EAF2FC; --bd:#B4CDEC; --ac:#2F6FC4; }
.c-orange { --bg:#FDF4E6; --bd:#EFCD98; --ac:#CF8A22; }
.c-purple { --bg:#F2EDFB; --bd:#C8B7E8; --ac:#7B5EC4; }
.c-teal   { --bg:#E9F4F8; --bd:#AED2E0; --ac:#3C8AA8; }
.c-slate  { --bg:#F3F6FA; --bd:#D4DCE8; --ac:#5B6B92; }
.c-rose   { --bg:#FCEEEC; --bd:#EDBDB6; --ac:#C4564A; }

/* Spread children evenly down a full-height column so parallel
   columns line up with each other. */
.col.spread { justify-content: space-between; }

/* ============================================================
   Canvas
   ============================================================ */
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; background: #E9EDF3; }

.canvas {
  width: 1600px;
  height: 900px;
  background: var(--canvas);
  font-family: var(--font);
  color: var(--body);
  padding: 30px 38px 26px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
/* Portrait / square variants: <div class="canvas ratio-4-3"> */
.canvas.ratio-4-3 { width: 1400px; height: 1050px; }
.canvas.ratio-1-1 { width: 1200px; height: 1200px; }
.canvas.tall      { width: 1200px; height: 1600px; }

/* ============================================================
   Headline
   ============================================================ */
.head { text-align: center; margin-bottom: 18px; flex: 0 0 auto; }
.head h1 {
  margin: 0;
  font-size: 40px;
  font-weight: 700;
  color: var(--title);
  letter-spacing: -.2px;
  line-height: 1.15;
}
.head .tagline {
  margin: 6px 0 0;
  font-size: 21px;
  font-weight: 400;
  color: #4A6285;
}

/* ============================================================
   Layout
   ============================================================ */
.stage { flex: 1 1 auto; display: flex; gap: 20px; min-height: 0; }
.stage > * { min-width: 0; }

.col        { display: flex; flex-direction: column; gap: 12px; }
.col.narrow { flex: 0 0 250px; }
.col.wide   { flex: 1 1 auto; }
.col.mid    { flex: 0 0 300px; }

.row  { display: flex; gap: 12px; }
.row.center { align-items: center; }
.grow { flex: 1 1 auto; }
.spacer { flex: 1 1 auto; }

/* Column heading (e.g. "DATA SOURCES / Heterogeneous by nature") */
.colhead { text-align: center; margin-bottom: 4px; }
.colhead h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--heading);
  letter-spacing: .4px;
}
.colhead p { margin: 2px 0 0; font-size: 14px; color: var(--muted); }

/* ============================================================
   Panel — the light-bordered container that groups things
   ============================================================ */
.panel {
  background: var(--panel);
  border: 1px solid var(--bd);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.panel.soft  { background: var(--wash); }
.panel.flush { padding: 12px; }
.panel > .panelhead { display: flex; align-items: center; gap: 9px; margin-bottom: 2px; }
.panel > .panelhead h3 {
  margin: 0; font-size: 20px; font-weight: 700;
  color: var(--heading); letter-spacing: .3px;
}
.panel > .panelhead p { margin: 2px 0 0; font-size: 13px; color: var(--muted); }
.panel > .panelhead .txt { display: flex; flex-direction: column; }

/* Sub-panel: a nested box inside a panel */
.subpanel {
  border: 1px solid var(--rule);
  border-radius: var(--r-md);
  background: #FFF;
  padding: 13px 15px;
}
.subpanel .sptitle {
  text-align: center; color: var(--heading);
  font-size: 17px; font-weight: 600; line-height: 1.35;
}

/* Vertical dashed rail (separates adapter column from logic column) */
.rail { width: 1px; border-left: 1px dashed #CBD6E6; align-self: stretch; margin: 4px 6px; }

/* ============================================================
   Card — the pastel source/component box
   ============================================================ */
.card {
  background: var(--bg);
  border: 1px solid var(--bd);
  border-radius: var(--r-md);
  padding: 11px 13px;
  display: flex;
  align-items: center;
  gap: 11px;
  box-shadow: var(--shadow);
}
.card .ic {
  flex: 0 0 auto;
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  font-size: 21px; line-height: 1;
  color: var(--ac);
}
.card .ic svg { width: 27px; height: 27px; display: block; }
.card .txt { min-width: 0; }
.card h4 {
  margin: 0; font-size: 16px; font-weight: 600; color: var(--ink);
  line-height: 1.25;
}
.card p { margin: 2px 0 0; font-size: 12.5px; color: var(--muted); line-height: 1.35; }
.card.tight  { padding: 9px 11px; }
.card.plain  { background: #FFF; }
.card.stackv { flex-direction: column; text-align: center; gap: 6px; }

/* Feature card: circular icon badge on the left, used in agent columns */
.featurecard {
  background: #FFF;
  border: 1px solid var(--bd);
  border-radius: var(--r-md);
  padding: 11px 13px;
  display: flex; align-items: center; gap: 12px;
  box-shadow: var(--shadow);
}
.featurecard .badge {
  flex: 0 0 auto;
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--bg); border: 1px solid var(--bd);
  display: flex; align-items: center; justify-content: center;
  font-size: 17px; color: var(--ac);
}
.featurecard .badge svg { width: 19px; height: 19px; }
.featurecard h4 { margin: 0; font-size: 16px; font-weight: 600; color: var(--ink); }
.featurecard p  { margin: 2px 0 0; font-size: 12.5px; color: var(--muted); line-height: 1.35; }

/* Hero avatar — large circular focal icon */
.hero {
  width: 92px; height: 92px; border-radius: 50%;
  margin: 2px auto 6px;
  background: var(--bg); border: 1px solid var(--bd);
  display: flex; align-items: center; justify-content: center;
  font-size: 46px; color: var(--ac);
}
.hero svg { width: 50px; height: 50px; }

/* ============================================================
   Matrix / table
   ============================================================ */
.matrix { width: 100%; border-collapse: collapse; font-size: 14px; }
.matrix caption {
  color: var(--heading); font-size: 16px; font-weight: 600;
  padding-bottom: 8px;
}
.matrix th, .matrix td {
  border: 1px solid var(--rule);
  padding: 6px 8px;
  text-align: center;
  color: var(--body);
}
.matrix thead th { font-weight: 600; color: var(--heading); background: #FBFCFE; }
.matrix tbody th { font-weight: 500; text-align: left; color: var(--ink); }
.matrix .yes { color: var(--yes); font-size: 15px; font-weight: 700; }
.matrix .no  { color: var(--no);  font-size: 15px; font-weight: 700; }

/* ============================================================
   Checklist rows (shield + label: value)
   ============================================================ */
.checklist { display: flex; flex-direction: column; gap: 11px; }
.checkrow { display: flex; align-items: center; gap: 11px; font-size: 15.5px; color: var(--body); }
.checkrow .ic { color: var(--brand); display: flex; }
.checkrow .ic svg { width: 21px; height: 21px; }
.checkrow b { font-weight: 600; color: var(--ink); }

/* ============================================================
   Protocol pill + arrows (the centrepiece)
   ============================================================ */
.pillblock { display: flex; flex-direction: column; align-items: center; gap: 10px; text-align: center; }
.pillblock .kicker {
  font-size: 20px; font-weight: 700; color: var(--brand-dark);
  line-height: 1.3; letter-spacing: .2px;
}
.pillblock .note { font-size: 15px; color: var(--body); line-height: 1.45; }

.pillrow { display: flex; align-items: center; gap: 10px; width: 100%; justify-content: center; }
.pill {
  background: var(--brand);
  color: #FFF;
  font-size: 24px; font-weight: 700; letter-spacing: .4px;
  padding: 13px 26px;
  border-radius: var(--r-sm);
  box-shadow: 0 2px 6px rgba(26, 95, 200, .25);
  white-space: nowrap;
}
.pill.sm { font-size: 17px; padding: 9px 18px; }

/* Arrow heads, both directions */
.arrow { flex: 1 1 auto; height: 16px; position: relative; min-width: 34px; }
.arrow::before {
  content: ""; position: absolute; top: 50%; left: 0; right: 0;
  height: 4px; transform: translateY(-50%); background: var(--brand);
}
.arrow::after {
  content: ""; position: absolute; top: 50%; width: 0; height: 0;
  transform: translateY(-50%);
  border-top: 9px solid transparent; border-bottom: 9px solid transparent;
}
.arrow.right::after { right: 0; border-left: 14px solid var(--brand); }
.arrow.left::after  { left: 0;  border-right: 14px solid var(--brand); }
.arrow.right::before { right: 10px; }
.arrow.left::before  { left: 10px; }

/* ============================================================
   Footer band
   ============================================================ */
.footer {
  flex: 0 0 auto;
  margin-top: 16px;
  padding: 12px 16px;
  border-top: 1px solid var(--panel-bd);
  display: flex; align-items: center; gap: 13px;
}
.footer .ic { color: var(--brand); display: flex; }
.footer .ic svg { width: 32px; height: 32px; }
.footer h3 {
  margin: 0; font-size: 21px; font-weight: 700;
  color: var(--heading); letter-spacing: .5px;
}
.footer p { margin: 3px 0 0; font-size: 13px; color: var(--muted); line-height: 1.4; }

/* ============================================================
   Misc utilities
   ============================================================ */
.center   { text-align: center; }
.caps     { text-transform: uppercase; letter-spacing: .6px; }
.small    { font-size: 12.5px; }
.strong   { font-weight: 600; color: var(--ink); }
.accent   { color: var(--brand); }
.legend   { display: flex; gap: 18px; justify-content: center; font-size: 13px; color: var(--muted); }
.legend span { display: flex; align-items: center; gap: 6px; }
.dot { width: 11px; height: 11px; border-radius: 50%; display: inline-block; }
.tag {
  display: inline-block; font-size: 11.5px; font-weight: 600;
  padding: 2px 8px; border-radius: 20px;
  background: var(--bg); border: 1px solid var(--bd); color: var(--ac);
}

/* Auto-drawn connector layer (see connect.js) */
#wires { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.canvas > *:not(#wires) { position: relative; z-index: 1; }

/* Panels that should hug their content instead of stretching to fill
   a tall row. Pair with .row.top on the parent row. */
.panel.fit { flex: 0 0 auto; }
.row.top   { align-items: flex-start; }
```

### `connect.js`

```javascript
/* ============================================================
   connect.js — draws smooth connector lines between elements.

   Usage: give the source element an id and a data-wire listing
   one or more target ids:

     <div class="card" id="src-db" data-wire="adapter-2">
     <div class="card" id="hub" data-wire="a,b,c" data-wire-color="#CF8A22">

   Optional attributes on the SOURCE element:
     data-wire-color  line colour   (default #C3D2E6)
     data-wire-width  line width px (default 1.6)
     data-wire-arrow  "true" to add an arrowhead at the target
     data-wire-side   "auto" (default) | "lr" | "rl" | "tb" | "bt"
                      which edges to leave from / arrive at

   Lines are drawn into an SVG that sits behind all content, so
   they tuck neatly under the cards.
   ============================================================ */
(function () {
  function draw() {
    var canvas = document.querySelector('.canvas');
    if (!canvas) { window.__wiresReady = true; return; }

    var old = document.getElementById('wires');
    if (old) old.remove();

    var NS = 'http://www.w3.org/2000/svg';
    var svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('id', 'wires');
    svg.setAttribute('width', canvas.offsetWidth);
    svg.setAttribute('height', canvas.offsetHeight);
    var defs = document.createElementNS(NS, 'defs');
    svg.appendChild(defs);

    var base = canvas.getBoundingClientRect();
    var markers = {};

    function marker(color) {
      var key = 'm' + color.replace(/[^a-z0-9]/gi, '');
      if (markers[key]) return key;
      var m = document.createElementNS(NS, 'marker');
      m.setAttribute('id', key);
      m.setAttribute('viewBox', '0 0 10 10');
      m.setAttribute('refX', '9'); m.setAttribute('refY', '5');
      m.setAttribute('markerWidth', '6'); m.setAttribute('markerHeight', '6');
      m.setAttribute('orient', 'auto-start-reverse');
      var p = document.createElementNS(NS, 'path');
      p.setAttribute('d', 'M 0 1 L 10 5 L 0 9 z');
      p.setAttribute('fill', color);
      m.appendChild(p); defs.appendChild(m);
      markers[key] = true;
      return key;
    }

    function anchors(a, b, side) {
      var ar = a.getBoundingClientRect(), br = b.getBoundingClientRect();
      var A = { x: ar.left - base.left, y: ar.top - base.top, w: ar.width, h: ar.height };
      var B = { x: br.left - base.left, y: br.top - base.top, w: br.width, h: br.height };
      if (side === 'auto') {
        var dx = (B.x + B.w / 2) - (A.x + A.w / 2);
        var dy = (B.y + B.h / 2) - (A.y + A.h / 2);
        side = Math.abs(dx) >= Math.abs(dy) ? (dx >= 0 ? 'lr' : 'rl')
                                            : (dy >= 0 ? 'tb' : 'bt');
      }
      var p;
      if (side === 'lr') p = [{ x: A.x + A.w, y: A.y + A.h / 2 }, { x: B.x, y: B.y + B.h / 2 }, 'h'];
      else if (side === 'rl') p = [{ x: A.x, y: A.y + A.h / 2 }, { x: B.x + B.w, y: B.y + B.h / 2 }, 'h'];
      else if (side === 'tb') p = [{ x: A.x + A.w / 2, y: A.y + A.h }, { x: B.x + B.w / 2, y: B.y }, 'v'];
      else p = [{ x: A.x + A.w / 2, y: A.y }, { x: B.x + B.w / 2, y: B.y + B.h }, 'v'];
      return p;
    }

    document.querySelectorAll('[data-wire]').forEach(function (src) {
      var color = src.getAttribute('data-wire-color') || '#C3D2E6';
      var width = src.getAttribute('data-wire-width') || '1.6';
      var arrow = src.getAttribute('data-wire-arrow') === 'true';
      var side = src.getAttribute('data-wire-side') || 'auto';

      src.getAttribute('data-wire').split(',').forEach(function (id) {
        var dst = document.getElementById(id.trim());
        if (!dst) return;
        var a = anchors(src, dst, side), s = a[0], e = a[1], axis = a[2];
        var d;
        if (axis === 'h') {
          var mx = (s.x + e.x) / 2;
          d = 'M ' + s.x + ' ' + s.y + ' C ' + mx + ' ' + s.y + ', ' + mx + ' ' + e.y + ', ' + e.x + ' ' + e.y;
        } else {
          var my = (s.y + e.y) / 2;
          d = 'M ' + s.x + ' ' + s.y + ' C ' + s.x + ' ' + my + ', ' + e.x + ' ' + my + ', ' + e.x + ' ' + e.y;
        }
        var path = document.createElementNS(NS, 'path');
        path.setAttribute('d', d);
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke', color);
        path.setAttribute('stroke-width', width);
        path.setAttribute('stroke-linecap', 'round');
        if (arrow) path.setAttribute('marker-end', 'url(#' + marker(color) + ')');
        svg.appendChild(path);
      });
    });

    canvas.insertBefore(svg, canvas.firstChild);
    window.__wiresReady = true;
  }

  if (document.readyState === 'complete') {
    (document.fonts ? document.fonts.ready.then(draw) : draw());
  } else {
    window.addEventListener('load', function () {
      (document.fonts ? document.fonts.ready.then(draw) : draw());
    });
  }
})();
```

### `render.py`

```python
#!/usr/bin/env python3
"""
Render a diagram HTML file to a crisp PNG (and optionally PDF).

    python3 render.py diagram.html out.png            # 2x retina PNG
    python3 render.py diagram.html out.png --scale 3  # 3x, for print
    python3 render.py diagram.html out.png --pdf      # also write out.pdf

The script screenshots the `.canvas` element only, so there is never a
stray margin or scrollbar in the output. Exits non-zero on failure.
"""
import argparse
import os
import sys
import pathlib


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("html")
    ap.add_argument("png")
    ap.add_argument("--scale", type=float, default=2.0,
                    help="device pixel ratio; 2 = retina (default), 3 = print")
    ap.add_argument("--pdf", action="store_true",
                    help="also emit a vector PDF next to the PNG")
    ap.add_argument("--selector", default=".canvas",
                    help="element to capture (default .canvas)")
    args = ap.parse_args()

    src = pathlib.Path(args.html).resolve()
    if not src.exists():
        sys.exit(f"No such file: {src}")
    out = pathlib.Path(args.png).resolve()
    out.parent.mkdir(parents=True, exist_ok=True)

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        sys.exit("playwright is not installed. Run: pip install playwright "
                 "--break-system-packages && playwright install chromium")

    with sync_playwright() as p:
        browser = p.chromium.launch(args=["--force-color-profile=srgb",
                                          "--font-render-hinting=none"])
        page = browser.new_page(viewport={"width": 1700, "height": 1000},
                                device_scale_factor=args.scale)
        page.goto(src.as_uri(), wait_until="load")

        # Let webfonts settle and connector wires finish drawing.
        page.wait_for_timeout(250)
        try:
            page.wait_for_function("window.__wiresReady === true", timeout=3000)
        except Exception:
            pass  # connect.js is optional
        page.evaluate("document.fonts ? document.fonts.ready : true")
        page.wait_for_timeout(150)

        el = page.query_selector(args.selector)
        if el is None:
            browser.close()
            sys.exit(f"Selector {args.selector!r} not found — the HTML needs a "
                     f"<div class=\"canvas\"> wrapper.")

        box = el.bounding_box()
        el.screenshot(path=str(out))

        if args.pdf:
            pdf_path = out.with_suffix(".pdf")
            page.pdf(path=str(pdf_path),
                     width=f"{box['width']}px",
                     height=f"{box['height']}px",
                     print_background=True, margin={"top": "0", "bottom": "0",
                                                    "left": "0", "right": "0"})
            print(f"PDF  -> {pdf_path}")

        browser.close()

    size = os.path.getsize(out) / 1024
    print(f"PNG  -> {out}  ({int(box['width']*args.scale)}x"
          f"{int(box['height']*args.scale)}px, {size:.0f} KB)")


if __name__ == "__main__":
    main()
```
