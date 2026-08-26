---
name: enterprise-architecture-visual
description: >
  Generate polished enterprise-grade architecture, data-flow, security, AI/agent,
  integration, and platform diagrams as crisp SVG/PNG visuals. Use this skill when
  the user asks for a professional technical image, architecture picture, workflow
  diagram, system landscape, agentic AI diagram, security/data-access flow, or
  executive-friendly technical visual.
---

# Enterprise Architecture Visual Skill

## Goal

Turn a user's technical or business ask into a polished **enterprise architecture visual**
with the same design language as a high-quality consulting / banking / technology
architecture slide:

- clean white or very-light-grey canvas
- dark navy titles and headings
- strong Microsoft/Azure-style blue as the primary accent
- restrained green as the secondary accent
- rounded white cards with subtle borders
- flat vector icons
- thin directional connectors
- clear visual hierarchy
- dense enough to be useful, but never cluttered
- readable labels with exact spelling

The result must look intentionally designed, not like a generic flowchart.

---

## Default Output Strategy

### Prefer vector-first rendering

For diagrams containing labels, tables, role matrices, arrows, architecture blocks,
or more than a few words of text:

1. Build the visual as **SVG** or HTML/CSS/SVG.
2. Render/export it to **PNG** at high resolution.
3. Keep text as real vector text whenever possible.
4. Do not use a generative image model to render important text because text accuracy
   and alignment are more important than artistic variation.

If an image-generation tool is available, use it only when it materially improves
decorative illustrations or non-text visual elements.

### Default canvas

- Aspect ratio: **16:9**
- Working size: **1920 × 1080**
- Export PNG: at least **1920 × 1080**
- For very dense architecture diagrams, prefer **2560 × 1440**

---

## Visual DNA

### Colour palette

Use this palette unless the user provides brand colours.

| Purpose | Colour |
|---|---|
| Primary navy | `#173B73` |
| Strong blue | `#1954C5` |
| Medium blue | `#2F6FD6` |
| Pale blue fill | `#EEF4FC` |
| Secondary green | `#5F8F50` |
| Bright green accent | `#6EA84F` |
| Pale green fill | `#F1F7EE` |
| Main text | `#243247` |
| Secondary text | `#667085` |
| Border | `#CBD5E1` |
| Soft divider | `#E6EAF0` |
| Canvas | `#F7F9FC` |
| Card | `#FFFFFF` |
| Warning / deny | `#C75252` |

Keep the overall visual approximately:
- 65–75% white/light-neutral
- 15–20% blue
- 5–10% green
- red only for exceptions, denial, risk, or failure

Never use rainbow colouring unless the user specifically asks for it.

### Typography

Preferred fonts, in order:

1. **Aptos**
2. **Inter**
3. **Segoe UI**
4. **Arial**
5. generic sans-serif

Use one font family throughout.

Recommended sizing on a 1920×1080 canvas:

- Main title: 34–42 px, semibold
- Subtitle: 20–24 px, regular
- Section heading: 18–22 px, semibold, uppercase only when useful
- Card title: 17–20 px, semibold
- Body text: 14–17 px
- Footnotes: 11–13 px

Do not use handwritten, serif, condensed, futuristic, or decorative fonts.

---

## Layout Rules

### 1. Establish a clear reading direction

Prefer one of these:

- **Left → Right** for data/agent/system flows
- **Top → Bottom** for workflows and lifecycle diagrams
- **Hub-and-spoke** only when there is a true central platform or control plane

The user should understand the story in under five seconds.

### 2. Use major zones

For an architecture/data-access image, default to 3–5 large zones, such as:

- Data Sources
- Unified API / Integration Layer
- Security / Governance
- Protocol / Communication Layer
- Agent / Consumer

Each zone should have:
- a subtle container
- a short heading
- an optional 2–5 word descriptor
- consistent internal spacing

### 3. Use cards instead of loose text

Represent systems, services, agents, tools, and capabilities as rounded cards.

Default card style:
- white background
- 10–14 px corner radius
- 1 px light border
- minimal or no shadow
- small icon on the left
- title + one-line descriptor

### 4. Keep spacing disciplined

Use an 8 px spacing system:
- 8 / 16 / 24 / 32 / 48 / 64

Avoid arbitrary placement.

### 5. Keep connectors clean

- Prefer orthogonal or simple straight connectors.
- Use arrowheads only where direction matters.
- Keep line width around 2–3 px.
- Blue for normal communication.
- Green for successful/approved/access paths.
- Red only for blocked/denied paths.
- Avoid crossing lines. If unavoidable, reroute the layout.

---

## Architecture Visual Patterns

Choose the pattern that best matches the user's request.

### Pattern A — Enterprise Data Access Flow

Use when showing heterogeneous sources accessed by an agent or application.

Structure:

`Data Sources → Adapters/API Layer → Security & Access Control → Protocol → Agent`

Good for:
- MCP architecture
- enterprise AI access
- API governance
- data access
- agent permissions
- security architecture

### Pattern B — Agentic AI Flow

Structure:

`User/UI → Orchestrator Agent → Specialist Agents / Tools → Enterprise Systems → Result`

Add:
- LLM/model connection
- MCP/A2A/AG-UI/A2UI as relevant
- security boundary
- audit/observability as a horizontal concern

### Pattern C — Multi-Agent Architecture

Use:
- one orchestrator
- 2–5 specialist agents
- shared model or per-agent model
- tool/API layer
- data systems
- audit/guardrails

Do not draw every possible call. Show only architecturally meaningful relationships.

### Pattern D — Executive Workflow

Use when the audience is business-friendly.

Show:
- 5–7 steps maximum
- business verbs
- minimal implementation details
- one visible outcome / value statement
- small technical footer if needed

### Pattern E — Control Matrix

Use when permissions or roles matter.

Show a compact table inside the visual:
- rows = identities/roles
- columns = systems/actions
- green check = permitted
- red × = denied
- optional amber = conditional

Keep the matrix visually secondary to the main flow.

---

## Icons

Use simple flat vector icons.

Preferred icon concepts:
- folder = files
- database cylinder = DB
- braces / API nodes = API
- cloud = SaaS/cloud
- document stack = documents
- shield = security
- key/lock = identity/access
- robot/head = agent
- magnifier = discover/retrieve
- gear = reason/process
- arrow/action icon = act/automate
- chat bubble = interact/respond

Rules:
- one icon style only
- no photorealistic icons
- no 3D clip-art
- no emoji
- icon colours should use blue/green/neutral from the palette

---

## Content Rules

Before drawing, convert the user's ask into a compact architecture story.

### Reduce text

For each box:
- title: ideally 1–4 words
- descriptor: ideally 3–8 words
- no paragraph inside a card

If the user provides a long explanation, summarize it before placing it on the image.

### Preserve important technology names exactly

Examples:
- MCP
- A2A
- AG-UI
- A2UI
- Spring AI
- Azure AI Foundry
- OpenAI
- Ollama
- Temporal
- Kafka
- Kubernetes

Never invent product names or protocols.

### Label the main message

Every image should have:
- one main title
- optional subtitle
- one visually dominant architectural flow
- optional bottom strapline / takeaway

Example strapline:

**UNIFIED SECURITY. UNIFIED ACCESS. CONSISTENT AGENT EXPERIENCE.**

Use a strapline only if it strengthens the story.

---

## Workflow

When the user asks for an image:

### Step 1 — Extract intent

Determine:
- audience: technical / mixed / executive
- diagram type
- primary components
- protocols
- trust/security boundaries
- important flows
- desired outcome

Do not ask follow-up questions if a reasonable architecture can be inferred.
Make sensible assumptions and keep them conservative.

### Step 2 — Create the visual hierarchy

Decide:
1. title
2. subtitle
3. major zones
4. cards/components
5. primary arrows
6. secondary annotations
7. footer/takeaway

### Step 3 — Simplify

Remove anything that:
- duplicates another concept
- is implementation trivia
- causes line crossing
- makes text too small
- does not help explain the architecture

### Step 4 — Render

Build the SVG with:
- exact text
- consistent font
- rounded rectangles
- vector icons
- clean arrows
- subtle section backgrounds

### Step 5 — Quality check

Before delivering, verify all of the following:

- [ ] No spelling errors
- [ ] No truncated labels
- [ ] No overlapping text
- [ ] No crossed connectors unless unavoidable
- [ ] No boxes touching the canvas edge
- [ ] Main flow is obvious
- [ ] All arrows have a meaningful source and destination
- [ ] Font is readable at normal viewing size
- [ ] Colour use is restrained and consistent
- [ ] Security/identity controls are clearly separated from business/data flow
- [ ] Protocol names are technically correct
- [ ] Diagram is understandable without a verbal explanation
- [ ] Exported PNG is sharp

If any check fails, revise the layout before returning the image.

---

## Style Constraints

Never produce:
- generic rainbow flowcharts
- neon gradients
- excessive shadows
- glassmorphism
- cartoonish architecture
- busy backgrounds
- photorealistic servers
- tiny unreadable labels
- 12+ equal-priority boxes with no hierarchy
- random icons from multiple visual styles
- excessive decorative shapes
- long paragraphs inside the image

Avoid gradients by default. If used, make them extremely subtle.

---

## User Ask → Diagram Specification Template

Internally transform the request into this structure:

```text
TITLE:
<short architecture title>

SUBTITLE:
<one-line purpose>

AUDIENCE:
Technical | Mixed | Executive

LAYOUT:
Left-to-right | Top-to-bottom | Hub-and-spoke

ZONES:
1. <zone>
2. <zone>
3. <zone>
...

COMPONENTS:
- <name>: <short descriptor>
- <name>: <short descriptor>

PRIMARY FLOW:
A → B → C → D

SECONDARY FLOWS:
- X → Y
- ...

SECURITY / GOVERNANCE:
- Authentication
- Authorization
- Data protection
- Audit / monitoring

KEY MESSAGE:
<one sentence>

STYLE:
Enterprise consulting / banking technology architecture
White + navy + blue + restrained green
Flat vector icons
Crisp SVG typography
```

---

## Example

User ask:

> Show an AI agent accessing SharePoint, database, REST APIs and SaaS through MCP.
> I want identity, RBAC and audit controls to be clearly visible.

Interpret as:

```text
TITLE:
Agentic Identity Architecture: Data Access Flow

SUBTITLE:
One protocol. Consistent access. Unified security.

LAYOUT:
Left-to-right

ZONES:
1. Enterprise Data Sources
2. Unified API Layer
3. Authorization & Security
4. MCP Protocol
5. AI Agent

DATA SOURCES:
- File System
- Database
- APIs
- SaaS
- Cloud Documents

SECURITY:
- MFA / identity
- RBAC / ABAC
- Encryption / DLP
- Audit / monitoring

AGENT CAPABILITIES:
- Discover & Retrieve
- Understand & Reason
- Act & Automate
- Interact & Respond
```

Then render it as a polished 16:9 SVG/PNG using the visual rules above.

---

## Final Delivery Behaviour

When the visual is requested:

1. Generate the image directly.
2. Return the rendered PNG.
3. Also keep the SVG source when possible so the user can edit it later.
4. Do not give a long explanation unless the user asks.
5. If assumptions materially affect the architecture, mention them in one short note.

The standard to aim for is:

**professional enough for an enterprise architecture review, leadership deck, technical article, or conference presentation.**
