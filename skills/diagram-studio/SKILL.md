---
name: diagram-studio
description: Builds polished, presentation-grade diagrams as PNG images in a consistent corporate house style — the kind that go on a slide in front of executives. Covers system and architecture diagrams, process flows and pipelines, timelines and roadmaps, org charts and decision trees, 2x2 quadrants and matrices, swimlanes, funnels, customer journeys, comparison and pros/cons layouts, KPI summaries, capability maps and layered stacks. Use this skill whenever the user asks for a diagram, chart of boxes and arrows, flow, visual, "how it works" picture, roadmap, or wants an existing sketch, list or description "made professional", "turned into an image" or "put on a slide". Trigger it even when the user just describes components, phases or options and says "show me this" without using the word diagram, and when they share an example image and ask for something in that style.
---

# Diagram Studio

One house style, many diagram types: dark-blue headline, pastel cards with
matching hairline borders, white grouping panels, restrained blue accents,
curved connectors, and a footer band that states the takeaway. Output is a
retina PNG that drops straight onto a slide.

The style lives entirely in `theme.css`. Compose HTML from its classes and
the result looks right; hand-write bespoke CSS and it won't. That is the
whole trick — this is a component kit, not a template for one picture.

## Workflow

1. **Decide what the diagram argues.** Every good diagram answers one
   sentence. Write that sentence first — it becomes the footer band. If the
   sentence is hard to write, the user probably wants two diagrams.
2. **Pick the type** from the table below, then read the matching recipe in
   [Layout recipes](#layout-recipes).
3. **Write out the assets** — `theme.css`, `connect.js` and `render.py` —
   from the [Assets](#assets) section at the bottom of this file, into the
   working directory. Copy them verbatim; skip any that already exist.
4. **Write `diagram.html`** from the blocks in the
   [Component catalogue](#component-catalogue). Compose from those classes
   rather than writing new CSS.
5. **Render:** `python3 render.py diagram.html out.png`
   (2× retina by default; `--scale 3` for print, `--pdf` for vector).
6. **Look at the PNG.** Not optional — `view` the rendered image. Layout
   bugs are invisible in HTML and obvious in the picture: text overflowing a
   card, a column running off the bottom, a connector pointing at the wrong
   box, one card twice the height of its neighbours. Fix and re-render.
7. **Hand it over.** Tell the user the PNG path so they can open it in the
   VS Code image preview. Keep `diagram.html` beside it as editable source.

## Choosing the type

| The user is describing… | Type | Core components |
|---|---|---|
| Systems, sources, layers, integrations | **Flow** (left→right) | `col` + `panel` + `card`, `data-wire` connectors, `pill` |
| Tiers stacked on each other | **Layered stack** | full-width `panel` rows, each `panelhead` + `card`s |
| Stages of a process, a lifecycle | **Pipeline / steps** | `steps linked`, or `card stackv` + `arrow right` |
| Dates, phases, quarters, a plan | **Timeline / roadmap** | `timeline`, `timeline vert`, `barrow gantt` |
| Two dimensions, prioritisation | **Quadrant** | `quadwrap` + `quad` |
| Who does what, handoffs | **Swimlanes** | `lanehead` + `swim` + `lane` + `taskbox` |
| Reporting lines, decision branches | **Tree** | `treerow` + `node`, wired `data-wire-side="tb"` |
| Drop-off through stages | **Funnel** | `funnel` + `fstage` with decreasing widths |
| Options weighed against each other | **Comparison** | `compare` + `side`, or a `matrix` table |
| Capabilities, coverage, a portfolio | **Capability map** | grid of `card`s grouped in `panel`s |
| Numbers that carry the story | **KPI summary** | `stats` + `stat`, `bars` + `barrow` |
| Everything routed through one thing | **Hub and spoke** | central `pill`/`hero`, satellites, `data-wire` |

Mixing two types on one canvas is fine and often better than two slides — a
pipeline row above a swimlane, or a timeline above KPI tiles. Keep it to two.

## Design rules that keep it looking professional

**Colour carries meaning, not decoration.** Assign one accent family per
category and hold it across the whole diagram — if databases are `c-blue`
in one column they are `c-blue` everywhere. Cap it at 5 accent families;
past that it reads as clip art. Saturated blue (`--brand`) is reserved for
the one focal element and the connectors, so it stays emphatic.

**One idea per block.** A card is 1–4 words of heading plus 3–7 words of
caption. If a caption needs a comma-heavy list, it belongs in a sub-panel
or the footer.

**Density is the enemy.** 4–6 items per column, 4–5 rows in a matrix, 5–7
stages in a pipeline, 6 nodes on a timeline. Beyond that, split into two
diagrams or summarise the detail one level up.

**Never override the type scale.** Sizes in `theme.css` are tuned for a
16:9 slide viewed from across a room. Shrinking font-size to cram in more
text is the fastest way to look amateur — cut words instead. The only
acceptable exception is a deliberate step-down on a repeated secondary card.

**Alignment reads as competence.** Parallel columns should start and end on
the same line. Wrap column contents in `.panel grow`, add `.spread` to
distribute children down the full height, or use `.panel fit` with
`.row top` when a panel should hug its content instead of stretching.

**Match the user's own spelling and vocabulary.** If they wrote
"Authorisation" or called a team a "squad", keep it.

## Content the user hasn't specified

Users give components, not copy. Fill gaps in house style rather than
leaving blanks:

- **Headline:** `Subject: What It Does` — "Payments Platform: Transaction
  Flow", "FY26 Roadmap: Three Horizons". Sentence case, no full stop.
- **Tagline:** three short declaratives — "One ledger. Real-time
  settlement. Full auditability."
- **Section heads:** upper case, 1–3 words, with a lowercase qualifier line.
- **Footer:** the sentence from step 1 as a caps blue heading, plus one
  explanatory line of about 30 words.

Invented labels should be plausible and generic. Don't invent specific
vendor names, metrics, dates or compliance claims the user hasn't given —
those are exactly what gets a slide challenged in a meeting. If a number
would strengthen the diagram, ask for it or leave the slot out.

## Fonts

The stack is `Lato, Segoe UI, Carlito, Calibri, Trebuchet MS`. Lato and
Segoe UI often aren't installed in a sandbox; Carlito is the
metric-compatible Calibri substitute that renders there and looks correct.
Don't swap in another family — the humanist sans is a large part of why
this reads as corporate-polished rather than generic-web.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Content spills past the bottom edge | Too many items. Cut, or switch to `.canvas.ratio-4-3` / `.canvas.tall`. Don't shrink type. |
| Big empty gap below a panel's content | The row is taller than what's in it. Use `.panel.fit` with `.row.top`, or move content up from another section. |
| Card fill is white when an accent was set | The accent class must be on the same element as `.card` (`class="card c-green"`), not a parent. |
| Columns are different heights | Add `grow` to the panel in each column. |
| Connector lines missing | `connect.js` isn't next to the HTML, or the `data-wire` target id doesn't exist. |
| Connectors cross over cards | Reorder so wired pairs sit roughly opposite each other, or set `data-wire-side="lr"` / `"tb"`. |
| Icons render as empty boxes | The `<symbol>` sprite must be inline in the same HTML file; external `<use href="file.svg#id">` is blocked under `file://`. |
| `playwright` not installed | `pip install playwright && playwright install chromium` |

---

# Layout recipes

One recipe per diagram type: the skeleton, how many items it takes before it
breaks, and the mistake that usually spoils it.

### Flow

Left-to-right: sources → processing → interface → consumer. The default for
anything with "architecture" in the name.

```html
<div class="stage">
  <div class="col narrow">
    <div class="colhead"><h2>SOURCES</h2><p>Where data starts</p></div>
    <div class="panel grow col spread" style="padding:14px">…cards…</div>
  </div>
  <div class="col wide"><div class="panel grow">…processing…</div></div>
  <div class="col mid" style="justify-content:center">…pillblock…</div>
  <div class="col narrow">…consumer…</div>
</div>
```

Capacity: 3–4 columns, 4–6 cards each. Wire sources to their handlers with
`data-wire` so the eye follows the path.

Common mistake: giving every column equal width. Vary them — `narrow` for
lists of things, `wide` for the column doing the work. Unequal widths signal
which column matters.

Worked example: `assets/example-architecture.html`.

---

### Layered stack

Tiers sitting on each other — presentation over application over data over
infrastructure. Read bottom-up or top-down, so label the direction.

```html
<div class="stage" style="flex-direction:column;gap:14px">
  <div class="panel fit c-blue">
    <div class="panelhead"><span class="txt"><h3>PRESENTATION</h3><p>What users touch</p></span></div>
    <div class="row"><div class="card c-blue grow">…</div><div class="card c-blue grow">…</div></div>
  </div>
  <!-- repeat per layer, one accent family per layer -->
</div>
```

Capacity: 3–5 layers, 3–5 cards per layer.

Common mistake: layers of wildly different heights. Keep the card count per
row within one of each other, or the stack looks accidental.

---

### Pipeline / steps

A process with a direction. Two forms — numbered circles for methodology,
boxed stages with arrows for data movement.

```html
<!-- numbered -->
<div class="steps linked">
  <div class="step c-blue solid"><span class="num">1</span><h4>Discover</h4><p>Interviews and data review</p></div>
  <div class="step c-blue"><span class="num">2</span><h4>Design</h4><p>Options and trade-offs</p></div>
</div>

<!-- boxed with arrows -->
<div class="row center">
  <div class="card c-orange stackv grow">…</div>
  <span class="arrow right" style="flex:0 0 46px"></span>
  <div class="card c-blue stackv grow">…</div>
</div>
```

`.step.solid` fills the circle with brand blue — use it on the current or
first stage only.

Capacity: 4–7 stages. At 8 the labels collide; split into two rows or move
to a vertical timeline.

Common mistake: stage names that aren't parallel in form. Use all verbs
("Ingest, Standardise, Store") or all nouns, never a mix.

---

### Timeline / roadmap

```html
<div class="timeline">
  <div class="track">
    <div class="tlnode c-green"><span class="dot"></span>
      <div class="when">Q1 2026</div><h4>Foundations</h4><p>Data contracts agreed</p></div>
    <div class="tlnode c-blue"><span class="dot"></span>
      <div class="when">Q2 2026</div><h4>Pilot live</h4><p>Two source systems</p></div>
  </div>
</div>
```

Add `.vert` for a vertical timeline (`<div class="timeline vert">`) when
there are more than six entries or the descriptions run long — it scales far
better than cramming the horizontal axis.

Capacity: horizontal 4–6 nodes; vertical 6–10.

Common mistake: unevenly spaced time. The track spaces nodes equally
regardless of actual dates, so if the gaps are meaningfully different (two
weeks then two years), say so in the labels or use a Gantt instead.

---

### Gantt

Bars positioned in time, for when duration and overlap matter.

```html
<div class="lanehead"><span>Q1</span><span>Q2</span><span>Q3</span><span>Q4</span></div>
<div class="bars">
  <div class="barrow gantt c-blue"><span class="lbl">Discovery</span>
    <span class="track"><span class="fill" style="left:0%;width:22%"></span></span><span class="val">6 wks</span></div>
  <div class="barrow gantt c-green"><span class="lbl">Build</span>
    <span class="track"><span class="fill" style="left:20%;width:45%"></span></span><span class="val">Q1–Q3</span></div>
</div>
```

Set both `left` and `width` as percentages of the total span. Capacity: 4–8
rows.

Common mistake: bar positions that don't line up with the column headers.
Work out the percentages from the same denominator as the header count.

---

### Quadrant

Two axes, four cells. For prioritisation, positioning, risk.

```html
<div class="quadwrap" style="height:300px">
  <div class="ylab">IMPACT →</div>
  <div class="quad">
    <div class="cell c-green"><h4>Quick wins</h4><ul><li>Self-serve reporting</li></ul></div>
    <div class="cell c-blue"><h4>Major projects</h4><ul><li>Identity resolution</li></ul></div>
    <div class="cell c-slate"><h4>Fill-ins</h4><ul><li>Doc refresh</li></ul></div>
    <div class="cell c-rose"><h4>Reconsider</h4><ul><li>Custom BI portal</li></ul></div>
  </div>
  <div class="xlab">EFFORT →</div>
</div>
```

Cells run top-left, top-right, bottom-left, bottom-right — so the top row is
the high-Y row. Give `.quadwrap` an explicit height.

Capacity: 2–4 bullets per cell.

Common mistake: unlabelled axes, or axis labels that aren't directional. Use
an arrow and name the increasing end.

---

### Swimlanes

Who does what, across stages. The strongest layout for handoffs.

```html
<div class="lanehead"><span>Request</span><span>Assess</span><span>Build</span><span>Verify</span></div>
<div class="swim">
  <div class="lane"><span class="lanelabel c-blue">Requester</span>
    <div class="lanebody">
      <div class="taskbox">Submit intake form</div>
      <div class="taskbox empty">—</div>
      <div class="taskbox empty">—</div>
      <div class="taskbox">Sign off output</div>
    </div></div>
</div>
```

Every lane needs the same number of `.taskbox` children as there are
headers — use `.taskbox.empty` for the gaps. That is what makes the columns
line up, and the empties are informative: they show where a role is idle.

Capacity: 3–5 lanes, 4–6 stages.

Common mistake: dropping the empty boxes, which shifts every later task into
the wrong column.

---

### Tree

Org charts, decision trees, taxonomies. Put `.tree` on the stage — it
centres the rows, spreads them down the full height and scales the nodes.

```html
<div class="stage tree">
  <div class="treerow">
    <div class="node c-blue" id="lead" data-wire="a,b" data-wire-side="tb" data-wire-arrow="true">
      <h4>Platform Lead</h4><p>Accountable for roadmap</p></div>
  </div>
  <div class="treerow" style="gap:46px">
    <div class="node c-green" id="a" data-wire="a1,a2" data-wire-side="tb"><h4>Ingest</h4><p>4 engineers</p></div>
    <div class="node c-teal"  id="b" data-wire="b1,b2" data-wire-side="tb"><h4>Governance</h4><p>3 analysts</p></div>
  </div>
  <div class="treerow leaf">
    <div class="node c-slate" id="a1"><h4>Connectors</h4></div>
    <div class="node c-slate" id="a2"><h4>Orchestration</h4></div>
    <div class="node c-slate" id="b1"><h4>Data quality</h4></div>
    <div class="node c-slate" id="b2"><h4>Access policy</h4></div>
  </div>
</div>
```

Always wire parent → children with `data-wire-side="tb"`. `.treerow.leaf`
shrinks the bottom row so leaves read as detail, not peers. Widen the gap on
middle rows (`style="gap:46px"`) so each parent sits above its own children —
that visual grouping is what makes a tree readable without extra lines.

Capacity: 3 levels, 6 leaves. Beyond that use `.canvas.ratio-4-3`.

Common mistake: wiring child → parent, which draws the curves upside down.

---

### Funnel

```html
<div class="funnel">
  <div class="fstage c-blue"   style="width:100%"><h4>Visitors</h4><span class="val">120k</span></div>
  <div class="fstage c-teal"   style="width:82%"><h4>Signups</h4><span class="val">34k</span></div>
  <div class="fstage c-green"  style="width:62%"><h4>Activated</h4><span class="val">11k</span></div>
  <div class="fstage c-orange" style="width:42%"><h4>Paying</h4><span class="val">2.4k</span></div>
</div>
```

Widths are visual, not proportional — a true-to-scale funnel with a 2%
conversion ends in an unreadable sliver. Step down 15–20% per stage and let
the numbers carry the real magnitude.

Capacity: 3–5 stages.

---

### Comparison

Two or three options side by side.

```html
<div class="compare">
  <div class="side c-green"><h4>Build in-house</h4>
    <ul><li>Full control of roadmap</li><li>Slower to first value</li></ul></div>
  <div class="side c-blue"><h4>Buy platform</h4>
    <ul><li>Live in eight weeks</li><li>Harder to customise</li></ul></div>
</div>
```

For many options against many criteria, use the `matrix` table instead —
comparison columns stop working past three options.

Common mistake: unbalanced bullet counts, which reads as a thumb on the
scale. Give each side the same number, including its downsides.

---

### Capability map

A portfolio or coverage picture: grouped tiles, no flow.

```html
<div class="stage" style="flex-direction:column;gap:14px">
  <div class="panel fit">
    <div class="panelhead"><span class="txt"><h3>CUSTOMER</h3></span></div>
    <div class="row">
      <div class="card c-blue grow tight">…</div>
      <div class="card c-blue grow tight">…</div>
      <div class="card c-slate grow tight">…</div>
    </div>
  </div>
</div>
```

Use `.tag` chips or `c-slate` fills to mark maturity — colour one family for
"in place", slate for "planned". Add a `.legend` so the coding is readable.

Capacity: 3–4 groups, 4–6 tiles each.

---

### KPI summary

Numbers as the story. Usually a strip of `stat` tiles above supporting bars.

```html
<div class="stats">
  <div class="stat c-blue"><div class="big">6.2M</div><h4>Records / day</h4><p>Peak throughput</p></div>
  <div class="stat c-green"><div class="big">99.7%</div><h4>Match rate</h4><p>Identity resolution</p></div>
</div>

<div class="bars">
  <div class="barrow c-green"><span class="lbl">Marketing</span>
    <span class="track"><span class="fill" style="width:88%"></span></span><span class="val">88%</span></div>
</div>
```

Add a `.callout` under the bars for the "so what" — the one line a reader
should take away.

Capacity: 3–5 stat tiles, 4–6 bars.

Common mistake: tiles without context. A number needs a unit and a
qualifier ("p95", "peak", "vs Q3"), or it invites the wrong question.

---

### Hub and spoke

Everything routed through one thing.

```html
<div class="stage" style="align-items:center;justify-content:center;gap:40px">
  <div class="col" style="gap:20px">
    <div class="card c-green" id="s1" data-wire="hub">…</div>
    <div class="card c-blue"  id="s2" data-wire="hub">…</div>
  </div>
  <div class="pillblock"><span class="pill" id="hub">EVENT BUS</span></div>
  <div class="col" style="gap:20px">
    <div class="card c-teal" id="c1">…</div>
  </div>
</div>
```

Wire spokes into the hub from the left, and the hub out to consumers on the
right (`data-wire="c1,c2"` on the hub). Set `data-wire-arrow="true"` when
direction is the point.

Capacity: 6–8 spokes before the wires tangle.


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

### Steps and timelines

```html
<div class="steps linked">
  <div class="step c-blue solid"><span class="num">1</span><h4>Discover</h4><p>Interviews and data review</p></div>
  <div class="step c-blue"><span class="num">2</span><h4>Design</h4><p>Options and trade-offs</p></div>
</div>
```

`.linked` draws the rule behind the circles; `.solid` fills one circle with
brand blue to mark the current or first stage.

```html
<div class="timeline">
  <div class="track">
    <div class="tlnode c-green"><span class="dot"></span>
      <div class="when">Q1 2026</div><h4>Foundations</h4><p>Data contracts agreed</p></div>
  </div>
</div>
```

`<div class="timeline vert">` flips it vertical — better past six entries.

---

### Quadrant

```html
<div class="quadwrap" style="height:300px">
  <div class="ylab">IMPACT &rarr;</div>
  <div class="quad">
    <div class="cell c-green"><h4>Quick wins</h4><ul><li>Self-serve reporting</li></ul></div>
    <div class="cell c-blue"><h4>Major projects</h4><ul><li>Identity resolution</li></ul></div>
    <div class="cell c-slate"><h4>Fill-ins</h4><ul><li>Doc refresh</li></ul></div>
    <div class="cell c-rose"><h4>Reconsider</h4><ul><li>Custom BI portal</li></ul></div>
  </div>
  <div class="xlab">EFFORT &rarr;</div>
</div>
```

Cells fill top-left, top-right, bottom-left, bottom-right. Give the wrapper
an explicit height.

---

### Swimlanes

```html
<div class="lanehead"><span>Request</span><span>Assess</span><span>Build</span></div>
<div class="swim">
  <div class="lane"><span class="lanelabel c-blue">Requester</span>
    <div class="lanebody">
      <div class="taskbox">Submit intake form</div>
      <div class="taskbox empty">&mdash;</div>
      <div class="taskbox">Sign off output</div>
    </div></div>
</div>
```

Each lane needs one child per header — `.taskbox.empty` for the gaps, which
is what keeps the columns aligned.

---

### Stats, bars and funnels

```html
<div class="stats">
  <div class="stat c-blue"><div class="big">6.2M</div><h4>Records / day</h4><p>Peak throughput</p></div>
</div>

<div class="bars">
  <div class="barrow c-green"><span class="lbl">Marketing</span>
    <span class="track"><span class="fill" style="width:88%"></span></span><span class="val">88%</span></div>
  <div class="barrow gantt c-blue"><span class="lbl">Build</span>
    <span class="track"><span class="fill" style="left:20%;width:45%"></span></span><span class="val">Q1&ndash;Q3</span></div>
</div>

<div class="funnel">
  <div class="fstage c-blue" style="width:100%"><h4>Visitors</h4><span class="val">120k</span></div>
  <div class="fstage c-teal" style="width:82%"><h4>Signups</h4><span class="val">34k</span></div>
</div>
```

`.barrow.gantt` takes both `left` and `width` on the fill. Funnel widths are
visual, not proportional — step down 15&ndash;20% per stage.

---

### Comparison, callouts, nodes and bands

```html
<div class="compare">
  <div class="side c-green"><h4>Build in-house</h4><ul><li>Full control</li><li>Slower to value</li></ul></div>
  <div class="side c-blue"><h4>Buy platform</h4><ul><li>Live in eight weeks</li><li>Harder to customise</li></ul></div>
</div>

<div class="callout c-orange"><b>Watch:</b> support adoption lags because access still needs a ticket.</div>

<div class="treerow">
  <div class="node c-blue" id="lead" data-wire="n1,n2" data-wire-side="tb"><h4>Platform Lead</h4><p>1 role</p></div>
</div>

<div class="band c-teal">DISCOVERY PHASE</div>
<div class="band solid">IN FLIGHT</div>
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

Write these three files into the working directory before rendering. The
house style lives entirely in `theme.css` — copy them byte for byte rather
than paraphrasing.

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

/* ============================================================
   PART 2 — Diagram-type components
   Everything below builds the non-architecture diagram types:
   timelines, quadrants, swimlanes, funnels, org trees, KPIs,
   comparisons, journeys, cycles.
   ============================================================ */

/* --- Numbered steps ------------------------------------- */
.steps { display: flex; gap: 0; align-items: stretch; }
.step { flex: 1 1 0; display: flex; flex-direction: column; align-items: center; text-align: center; padding: 0 8px; position: relative; }
.step .num {
  width: 44px; height: 44px; border-radius: 50%;
  background: var(--bg); border: 2px solid var(--bd); color: var(--ac);
  display: flex; align-items: center; justify-content: center;
  font-size: 19px; font-weight: 700; margin-bottom: 9px; z-index: 1;
}
.step.solid .num { background: var(--brand); border-color: var(--brand); color: #FFF; }
.step h4 { margin: 0; font-size: 16px; font-weight: 600; color: var(--ink); }
.step p  { margin: 3px 0 0; font-size: 12.5px; color: var(--muted); line-height: 1.4; }
/* connecting rule behind the circles */
.steps.linked .step:not(:last-child)::after {
  content: ""; position: absolute; top: 22px; left: 50%; right: -50%;
  height: 2px; background: var(--rule); z-index: 0;
}

/* --- Timeline / roadmap --------------------------------- */
.timeline { position: relative; padding: 26px 0 8px; }
.timeline::before {
  content: ""; position: absolute; top: 34px; left: 2%; right: 2%;
  height: 3px; background: var(--rule); border-radius: 2px;
}
.timeline .track { display: flex; justify-content: space-between; position: relative; }
.tlnode { flex: 1 1 0; text-align: center; padding: 0 6px; }
.tlnode .dot {
  width: 17px; height: 17px; border-radius: 50%;
  background: var(--ac); border: 3px solid #FFF;
  box-shadow: 0 0 0 2px var(--ac);
  margin: 0 auto 12px;
}
.tlnode .when { font-size: 13px; font-weight: 700; color: var(--brand); letter-spacing: .3px; }
.tlnode h4 { margin: 3px 0 0; font-size: 15.5px; font-weight: 600; color: var(--ink); }
.tlnode p  { margin: 3px 0 0; font-size: 12.5px; color: var(--muted); line-height: 1.4; }
/* vertical variant for long lists */
.timeline.vert { padding: 0 0 0 26px; }
.timeline.vert::before { top: 6px; bottom: 6px; left: 8px; right: auto; width: 3px; height: auto; }
.timeline.vert .track { flex-direction: column; gap: 16px; }
.timeline.vert .tlnode { text-align: left; padding: 0; position: relative; }
.timeline.vert .tlnode .dot { position: absolute; left: -25px; top: 4px; margin: 0; }

/* --- 2x2 quadrant --------------------------------------- */
.quadwrap { display: grid; grid-template-columns: 30px 1fr; grid-template-rows: 1fr 30px; gap: 8px; flex: 1 1 auto; min-height: 0; }
.quadwrap .ylab {
  writing-mode: vertical-rl; transform: rotate(180deg);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: var(--heading); letter-spacing: .4px;
}
.quadwrap .xlab {
  grid-column: 2; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: var(--heading); letter-spacing: .4px;
}
.quad { display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 10px; }
.quad .cell {
  background: var(--bg); border: 1px solid var(--bd);
  border-radius: var(--r-md); padding: 13px 15px;
  display: flex; flex-direction: column; gap: 7px;
}
.quad .cell h4 { margin: 0; font-size: 16.5px; font-weight: 700; color: var(--ac); letter-spacing: .2px; }
.quad .cell ul { margin: 0; padding-left: 17px; font-size: 13.5px; color: var(--body); line-height: 1.6; }
.quad .cell p  { margin: 0; font-size: 13.5px; color: var(--body); line-height: 1.5; }

/* --- Swimlanes ------------------------------------------ */
.swim { display: flex; flex-direction: column; gap: 8px; }
.lane { display: flex; align-items: stretch; gap: 9px; }
.lane .lanelabel {
  flex: 0 0 148px; display: flex; align-items: center;
  background: var(--bg); border: 1px solid var(--bd);
  border-radius: var(--r-sm); padding: 9px 12px;
  font-size: 14.5px; font-weight: 600; color: var(--ac);
}
.lane .lanebody { flex: 1 1 auto; display: flex; gap: 9px; align-items: stretch; }
.lane .lanebody > * { flex: 1 1 0; }
.lanehead { display: flex; gap: 9px; padding-left: 157px; }
.lanehead > span {
  flex: 1 1 0; text-align: center; font-size: 13px; font-weight: 600;
  color: var(--heading); letter-spacing: .3px; padding-bottom: 2px;
}
.taskbox {
  background: #FFF; border: 1px solid var(--rule); border-radius: var(--r-sm);
  padding: 9px 11px; font-size: 13.5px; color: var(--body); line-height: 1.4;
  display: flex; align-items: center; justify-content: center; text-align: center;
}
.taskbox.empty { background: #FBFCFE; border-style: dashed; }

/* --- Funnel --------------------------------------------- */
.funnel { display: flex; flex-direction: column; align-items: center; gap: 7px; }
.funnel .fstage {
  background: var(--bg); border: 1px solid var(--bd); border-radius: var(--r-sm);
  padding: 11px 16px; text-align: center;
  display: flex; align-items: baseline; justify-content: center; gap: 12px;
}
.funnel .fstage h4 { margin: 0; font-size: 16px; font-weight: 600; color: var(--ink); }
.funnel .fstage .val { font-size: 16px; font-weight: 700; color: var(--ac); }
.funnel .fstage p { margin: 0; font-size: 12.5px; color: var(--muted); }

/* --- KPI / stat tile ------------------------------------ */
.stats { display: flex; gap: 12px; }
.stat {
  flex: 1 1 0; background: var(--bg); border: 1px solid var(--bd);
  border-radius: var(--r-md); padding: 14px 16px; text-align: center;
}
.stat .big { font-size: 38px; font-weight: 700; color: var(--ac); line-height: 1.05; }
.stat h4 { margin: 5px 0 0; font-size: 14.5px; font-weight: 600; color: var(--ink); }
.stat p  { margin: 3px 0 0; font-size: 12px; color: var(--muted); line-height: 1.35; }

/* --- Bars (comparison, progress, mini-gantt) ------------ */
.bars { display: flex; flex-direction: column; gap: 11px; }
.barrow { display: flex; align-items: center; gap: 12px; font-size: 14px; }
.barrow .lbl { flex: 0 0 150px; color: var(--ink); font-weight: 500; }
.barrow .track { flex: 1 1 auto; height: 17px; background: #EEF2F8; border-radius: 9px; position: relative; overflow: hidden; }
.barrow .fill { position: absolute; top: 0; bottom: 0; background: var(--ac); border-radius: 9px; }
.barrow .val { flex: 0 0 58px; text-align: right; color: var(--muted); font-size: 13px; }
/* Gantt: set left AND width on .fill via inline style */
.barrow.gantt .track { background: #F5F8FC; border: 1px solid var(--rule); border-radius: var(--r-sm); }
.barrow.gantt .fill { border-radius: var(--r-sm); opacity: .9; }

/* --- Callout / note ------------------------------------- */
.callout {
  border-left: 4px solid var(--ac); background: var(--bg);
  border-radius: 0 var(--r-sm) var(--r-sm) 0;
  padding: 11px 15px; font-size: 14px; color: var(--body); line-height: 1.5;
}
.callout b { color: var(--ink); }

/* --- Comparison columns (pros/cons, option A vs B) ------ */
.compare { display: flex; gap: 14px; }
.compare > .side {
  flex: 1 1 0; background: var(--bg); border: 1px solid var(--bd);
  border-radius: var(--r-md); padding: 14px 16px;
}
.compare > .side h4 {
  margin: 0 0 9px; font-size: 17px; font-weight: 700; color: var(--ac);
  padding-bottom: 7px; border-bottom: 1px solid var(--bd);
}
.compare ul { margin: 0; padding-left: 17px; font-size: 13.5px; color: var(--body); line-height: 1.65; }

/* --- Tree node (org charts, decision trees) -------------- */
.treerow { display: flex; justify-content: center; gap: 14px; }
.node {
  background: var(--bg); border: 1px solid var(--bd); border-radius: var(--r-md);
  padding: 10px 15px; text-align: center; min-width: 132px; box-shadow: var(--shadow);
}
.node h4 { margin: 0; font-size: 15px; font-weight: 600; color: var(--ink); }
.node p  { margin: 2px 0 0; font-size: 12px; color: var(--muted); }

/* --- Cycle (loop of stages) ----------------------------- */
.cycle { position: relative; width: 100%; height: 100%; }
.cycle .hubnode {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  text-align: center;
}
.cycle .spoke { position: absolute; transform: translate(-50%, -50%); }

/* --- Venn ------------------------------------------------ */
.venn { position: relative; height: 260px; }
.venn .circle {
  position: absolute; border-radius: 50%; opacity: .68;
  display: flex; align-items: center; justify-content: center; text-align: center;
  font-size: 15px; font-weight: 600; color: var(--ink); padding: 20px;
}

/* --- Band (phase header strip) --------------------------- */
.band {
  background: var(--bg); border: 1px solid var(--bd);
  border-radius: var(--r-sm); padding: 7px 14px;
  font-size: 14px; font-weight: 600; color: var(--ac);
  letter-spacing: .3px; text-align: center;
}
.band.solid { background: var(--brand); border-color: var(--brand); color: #FFF; }

/* Tree wrapper: centres the rows, opens up the gaps so connector curves
   have room, and scales nodes to fill a slide. */
.tree {
  flex: 1 1 auto;
  display: flex; flex-direction: column;
  align-items: center; justify-content: space-evenly;
  gap: 26px;
  padding: 8px 0 14px;
}
.tree .treerow { gap: 26px; }
.tree .node { padding: 14px 20px; min-width: 160px; }
.tree .node h4 { font-size: 17px; }
.tree .node p  { font-size: 13px; }
.tree .treerow.leaf .node { padding: 11px 15px; min-width: 128px; }
.tree .treerow.leaf .node h4 { font-size: 14.5px; font-weight: 500; }
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
