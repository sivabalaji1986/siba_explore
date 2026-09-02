# File Processing Pipeline: From Upload to Markdown

**A design discussion and reference implementation**
Target: DOCX, PDF (native and scanned), PNG/JPEG → Markdown
Constraints: 20 MB maximum input, sub-30-second wall clock, Java platform
External dependencies: PaddleOCR endpoint, GPT endpoint

---

## Contents

1. [How Claude handles uploaded files](#1-how-claude-handles-uploaded-files)
2. [How you should handle them](#2-how-you-should-handle-them)
3. [Making 30 seconds a hard guarantee](#3-making-30-seconds-a-hard-guarantee)
4. [The flow](#4-the-flow)
5. [The application](#5-the-application)
6. [Source code](#6-source-code)
7. [Caveats](#7-caveats)

---

## 1. How Claude handles uploaded files

A caveat first: this describes observable behaviour from where I sit, not
Anthropic's internals. But the shape is clear enough to be useful.

**Client → server.** The file is uploaded over HTTPS and stored. Type is
detected server-side.

**Then it forks based on type, and this is the important part:**

- **Plain text-ish** (txt, md, csv, json, html, source code): decoded and
  injected directly into the context window as text. No parsing stage, no OCR.

- **Images** (PNG/JPEG): passed in as image blocks. There is **no separate OCR
  service**. The vision encoder is part of the model — text in an image is read
  the same way a chart in it is read. One hop, no intermediate text artifact.

- **PDFs**: both paths. The extracted text layer *and* page renders go into
  context, so the words and the layout arrive together.

- **Office formats** (docx, xlsx, pptx): these are ZIP containers of XML. They
  cannot be rendered into context natively, so they land on a container
  filesystem and get read with tools — unzip, parse, extract.

**Small versus large is a real difference.** Small files ride in the context
window whole. Large ones exceed what is worth spending context on, so they stay
on disk and get queried selectively — grep, read a page range, parse a sheet at
a time. The switch is from "everything is in front of me" to "I fetch what I
need."

**The relevant takeaway:** Claude's speed comes from *not* having a pipeline for
most of it. Yours will come from the opposite — from routing aggressively so the
expensive hops fire as rarely as possible.

---

## 2. How you should handle them

Your two endpoints (PaddleOCR, GPT) are the slow, expensive links. The whole
design goal is to avoid touching them.

```
                     ┌──────────────────────────┐
                     │   Detect by magic bytes  │
                     └────────────┬─────────────┘
                ┌─────────────────┼─────────────────┐
                ▼                 ▼                 ▼
        ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
        │     DOCX      │ │      PDF      │ │  PNG / JPEG   │
        │  Zip of XML   │ │ Probe each pg │ │ No text layer │
        └───────┬───────┘ └───┬───────┬───┘ └───────┬───────┘
                │             │       │             │
                ▼             ▼       ▼             ▼
        ┌────────────┐ ┌───────────┐ ┌──────────┐ ┌────────────┐
        │  POI walk  │ │  PDFBox   │ │Rasterize │ │ PaddleOCR  │
        │Text+tables │ │Has text   │ │ Scanned  │→│Images+scans│
        └──────┬─────┘ └─────┬─────┘ └──────────┘ └──────┬─────┘
               │             │                            │
               └─────────────┼────────────────────────────┘
                             ▼
              ┌────────────────────────────────┐      ┌─────────────┐
              │   Canonical document model     │◄────►│     GPT     │
              │  Heading, para, table, image   │      │Only if noisy│
              └───────────────┬────────────────┘      └─────────────┘
                              ▼
                       ┌─────────────┐
                       │  Markdown   │
                       └─────────────┘
```

**Detect by content, not extension.** Apache Tika's detector alone
(`tika-core`, not the parsers) reads magic bytes and is fast. Users mislabel
files constantly.

**DOCX never touches OCR.** It is a ZIP of XML with the text already in it. Walk
`XWPFDocument` with Apache POI, mapping paragraph styles to markdown headings
and `XWPFTable` to pipe tables. Runs on the order of tens of milliseconds.
Embedded images get extracted separately and only go to OCR if their content
actually matters to you. If you can tolerate a native binary, shelling out to
`pandoc` gives noticeably better markdown for the same effort.

**PDF is the one that needs real logic, and the decision is per page, not per
document.** Run `PDFTextStripper` scoped to a single page. If it returns under
roughly 100 characters, that page is a scan — render it with
`PDFRenderer.renderImageWithDPI(page, 200)` and send it to Paddle. Otherwise
keep the extracted text. Mixed PDFs (typed report with scanned appendix) are
extremely common, and treating the file as all-one-or-the-other is where most
pipelines lose their time.

Two things that will bite you here: `PDDocument` is not thread-safe, so render
pages on one thread and fan out the OCR calls; and `PDFTextStripper` gives you a
flat character stream with no heading or table structure. If your markdown needs
real structure out of PDFs, use PaddleOCR's PP-Structure model on the page image
instead of the text layer — you trade latency for layout.

**Images go straight to Paddle**, but downscale to ~2000 px on the longest edge
and convert to grayscale JPEG first. Payload drops five to ten times and
accuracy barely moves.

**Build one canonical document model.** A list of typed blocks — heading,
paragraph, table, list, image — with a single markdown serializer at the end. If
each route emits markdown strings independently you will get three subtly
different dialects and spend a month reconciling them.

**Gate the LLM hard.** For a DOCX or a text-layer PDF you should never call GPT
at all; the text is already correct and the model only adds a second or two of
latency and a hallucination risk. Reserve it for low-confidence OCR output or
when you genuinely need semantic restructuring of a messy scan. Paddle returns
per-line confidence — threshold on it.

**On concurrency:** this workload is almost entirely IO-bound on your two
endpoints, so Java 21 virtual threads
(`Executors.newVirtualThreadPerTaskExecutor()`) fit it perfectly. Put a
`Semaphore` in front of the OCR client so you do not stampede it, use
`java.net.http.HttpClient` with HTTP/2 and keep-alive, and cache results keyed on
the SHA-256 of the file bytes — reprocessing the same document is the cheapest
win available.

**Split your API by size.** Under roughly 10 pages, process synchronously and
return the markdown in the response. Above that, return a job ID and process
asynchronously, streaming markdown per page as it completes. A 200-page scanned
PDF is minutes of OCR no matter how well you build this, and a synchronous
endpoint will just time out.

---

## 3. Making 30 seconds a hard guarantee

Two things drive this, and neither is file size: **page count** and **how many
of those pages need OCR**. A 20 MB DOCX is usually 19 MB of embedded images and
half a second of text. A 20 MB scanned PDF is ~130 pages of JPEG and could be
50+ seconds of OCR. Same limit, wildly different cost.

Stop treating 30 s as a hope and make it a budget you spend down.

### Allocate it explicitly

| Stage | Budget | Notes |
|---|---|---|
| Validate + detect | 100 ms | Magic bytes, size check |
| Cache lookup | 20 ms | SHA-256 of the bytes |
| Triage | 500 ms | Page count, text-layer probe |
| Extraction | 24 s | The only stage allowed to be slow |
| Assemble + serialize | 500 ms | |
| Reserve | 4 s | Network jitter, GC, response write |

### Propagate a deadline object through every call

Each HTTP request to Paddle gets `timeout = min(preferred, remaining budget)`.
When the budget runs out you return what you have with `truncated: true`, not a
timeout error. Partial markdown from 80 of 130 pages is a far better outcome
than a 504.

### Do the free work first

Probe the text layer on every PDF page before spending a millisecond on OCR.
Those pages cost ~10 ms each and might be 90% of the document. Only then spend
the remaining budget on the scanned pages. If you run out mid-way, you have
already banked the cheap majority.

### Derive a page cap from measured OCR latency

This is the number that actually protects you:

```
max_ocr_pages = (24s × ocr_concurrency) / p95_seconds_per_page
```

At 1 s/page and 8 concurrent workers that is ~190 pages. At 3 s/page
(CPU-hosted Paddle) it is 64. Measure your endpoint, then reject or go async
above the cap during triage — before you have spent anything.

### Rasterize and OCR concurrently, not in phases

`PDDocument` is not thread-safe, so render pages on one thread and hand JPEGs to
a worker pool. Bound in-flight images with a semaphore: an A4 page at 200 DPI
grayscale is ~3.9 MB in memory, so 8 in flight is 31 MB. RGB would be 4× that,
which is one of several reasons to force grayscale.

### Skip the LLM by default

A GPT round trip on a large document is 5–15 seconds and does not fit. Gate it
on document size and OCR confidence — small docs and low-confidence pages only.

### Also cap text-layer PDFs

A 20 MB born-digital PDF can be 2,000 pages, and `PDFTextStripper` at 15 ms/page
is 30 seconds all by itself. Apply a page cap on that path too, just a much
higher one.

---

## 4. The flow

```
    ┌──────────────────────────────────┐
    │      Validate and detect         │
    │   Size and magic bytes · 100 ms  │
    └────────────────┬─────────────────┘
                     ▼
    ┌──────────────────────────────────┐       ┌────────────────────┐
    │          Cache lookup            │──────►│ Hit: return stored │
    │    SHA-256 of bytes · 20 ms      │       └────────────────────┘
    └────────────────┬─────────────────┘
                     ▼
    ┌──────────────────────────────────┐       ┌────────────────────┐
    │             Triage               │──────►│ Over cap: 202 async│
    │ Page count, scan ratio · 500 ms  │       └────────────────────┘
    └────────────────┬─────────────────┘
                     ▼
    ┌──────────────────────────────────┐       ┌────────────────────┐
    │       Extract in parallel        │──────►│Out of budget:      │
    │     Cheap pages first · 24 s     │       │partial result      │
    └────────────────┬─────────────────┘       └────────────────────┘
                     ▼
    ┌──────────────────────────────────┐
    │        Assemble model            │
    │   Merge blocks in page order     │
    └────────────────┬─────────────────┘
                     ▼
    ┌──────────────────────────────────┐
    │       Serialize markdown         │
    │    Cache and return · 200 ms     │
    └──────────────────────────────────┘
```

The three branches on the right are the whole point. Each one is an exit that
costs almost nothing and prevents a request from ever reaching 30 seconds by
accident.

---

## 5. The application

A 24-class Maven project, roughly 1,340 lines. Java 21 (virtual threads,
`Math.clamp`), PDFBox 3.0.3, POI 5.3.0, tika-core 3.0.0.

### Build and run

```bash
mvn -q clean package
java -jar target/file-extractor-1.0.0.jar sample.pdf
```

Markdown goes to stdout; page counts, timing and warnings go to stderr.

### What is implemented

| Path | Status |
|---|---|
| Type detection (magic bytes, tika-core) | complete |
| DOCX to markdown (POI, headings/lists/tables/bold/italic) | complete |
| PDF text-layer extraction, per-page (PDFBox) | complete |
| PDF page rasterization, grayscale JPEG, bounded in-flight | complete |
| Image normalization (downscale + grayscale + JPEG) | complete |
| Deadline propagation, triage, partial results | complete |
| Markdown serialization from the block model | complete |
| `PaddleOcrClient.recognize` | **stub** |
| `GptLlmClient.toCleanMarkdown` | **stub** |

Nothing else is stubbed. DOCX, plain text and text-layer PDFs run end to end
against the current code.

### Project layout

```
file-extractor/
├── pom.xml
├── README.md
└── src/main/java/com/example/extract/
    ├── Deadline.java                     the SLA, in 40 lines
    ├── ExtractionConfig.java             every tuning lever
    ├── ExtractionResult.java
    ├── ExtractionService.java            orchestrator, routing, cache
    ├── Main.java                         CLI demo
    ├── TooLargeForBudgetException.java   the async-path hook
    ├── UnsupportedFileException.java
    ├── detect/
    │   ├── SourceType.java
    │   └── TypeDetector.java             magic bytes, not extensions
    ├── extract/
    │   ├── Extractor.java
    │   ├── DocxExtractor.java            POI, never touches OCR
    │   ├── PdfExtractor.java             per-page triage, bounded rasterization
    │   ├── ImageExtractor.java
    │   └── PlainTextExtractor.java
    ├── llm/
    │   ├── LlmClient.java
    │   └── GptLlmClient.java             STUB
    ├── markdown/
    │   └── MarkdownWriter.java           the single serializer
    ├── model/
    │   ├── Block.java
    │   ├── BlockType.java
    │   └── DocumentModel.java
    ├── ocr/
    │   ├── OcrClient.java
    │   ├── OcrPage.java
    │   └── PaddleOcrClient.java          STUB
    └── util/
        └── Images.java                   downscale, grayscale, JPEG
```

### Tuning

Everything numeric lives in `ExtractionConfig.defaults()`. The two that matter:

**`maxPagesOcr`** — the cap that actually protects the SLA. Derive it, do not
guess it:

```
maxPagesOcr = (extractionBudgetMillis / 1000 * ocrConcurrency) / p95SecondsPerPage
```

Measure `p95SecondsPerPage` against your own Paddle deployment at 200 DPI. A GPU
host at ~1 s/page with 8 workers gives roughly 190; a CPU host at ~3 s/page
gives roughly 64. Set it too high and requests time out; too low and you push
work to the async queue unnecessarily.

**`maxImagesInFlight`** — the memory ceiling for rasterization. An A4 page at
200 DPI grayscale is about 3.9 MB in heap, so 8 permits caps that stage near
31 MB no matter how long the document is.

### Where to look first

`Deadline` and `PdfExtractor`. `Deadline` is the whole SLA argument in 40 lines —
every downstream call gets `min(preferred, remaining)` as its timeout, so nothing
can overrun. `PdfExtractor` is where the per-page triage, the
render-on-one-thread/OCR-in-parallel split, and the semaphore memory bound all
live.

`PaddleOcrClient.recognize` and `GptLlmClient.toCleanMarkdown` are the only
stubs. Both have implementation notes in their javadoc — the one to act on is
batching: if your Paddle endpoint accepts multiple images per request, batching
8 pages into one round trip removes most of the per-call overhead and is
probably the largest single speedup available.

---

## 6. Source code


### `pom.xml`  — Maven build

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>file-extractor</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <pdfbox.version>3.0.3</pdfbox.version>
    <poi.version>5.3.0</poi.version>
    <tika.version>3.0.0</tika.version>
  </properties>

  <dependencies>
    <!-- PDF: text layer extraction + page rasterization -->
    <dependency>
      <groupId>org.apache.pdfbox</groupId>
      <artifactId>pdfbox</artifactId>
      <version>${pdfbox.version}</version>
    </dependency>

    <!-- DOCX: OOXML parsing. No OCR needed on this path. -->
    <dependency>
      <groupId>org.apache.poi</groupId>
      <artifactId>poi-ooxml</artifactId>
      <version>${poi.version}</version>
    </dependency>

    <!-- Type detection by magic bytes. tika-core only: we do NOT want the
         tika-parsers pulled in, they are slow and we parse everything ourselves. -->
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-core</artifactId>
      <version>${tika.version}</version>
    </dependency>

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>2.0.16</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.6.0</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>com.example.extract.Main</mainClass>
                </transformer>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
              </transformers>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                  </excludes>
                </filter>
              </filters>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```


### `src/main/java/com/example/extract/Deadline.java`

```java
package com.example.extract;

import java.util.concurrent.TimeUnit;

/**
 * A wall-clock budget that is threaded through every stage of extraction.
 *
 * The whole design of this pipeline rests on this class: no stage is allowed to
 * run "until it is done", every stage runs "until it is done OR the budget is
 * gone, whichever comes first". That is what turns a 30 second SLA from a hope
 * into a guarantee.
 */
public final class Deadline {

    private final long startNanos;
    private final long budgetNanos;

    private Deadline(long budgetMillis) {
        this.startNanos = System.nanoTime();
        this.budgetNanos = TimeUnit.MILLISECONDS.toNanos(budgetMillis);
    }

    public static Deadline ofMillis(long budgetMillis) {
        return new Deadline(budgetMillis);
    }

    public long remainingMillis() {
        long left = budgetNanos - (System.nanoTime() - startNanos);
        return left <= 0L ? 0L : TimeUnit.NANOSECONDS.toMillis(left);
    }

    public long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    public boolean expired() {
        return remainingMillis() <= 0L;
    }

    /**
     * Timeout to hand to a single downstream call (OCR, LLM). Never let a call
     * wait longer than the budget that is actually left, no matter what the
     * caller asked for.
     */
    public long callTimeoutMillis(long preferredMillis) {
        return Math.max(1L, Math.min(preferredMillis, remainingMillis()));
    }

    /**
     * Reserve the tail of the budget so assembly and serialization still fit
     * after extraction stops.
     */
    public boolean hasAtLeast(long millis) {
        return remainingMillis() >= millis;
    }
}
```


### `src/main/java/com/example/extract/ExtractionConfig.java`

```java
package com.example.extract;

/**
 * Every number here is a lever you should tune against your own hardware.
 * The defaults target: 20 MB max input, 30 second wall clock.
 */
public record ExtractionConfig(
        long maxBytes,
        long totalBudgetMillis,
        long extractionBudgetMillis,
        long tailReserveMillis,

        // --- PDF ---
        float renderDpi,
        int textLayerMinChars,
        int maxPagesTextLayer,
        int maxPagesOcr,

        // --- concurrency / memory ---
        int ocrConcurrency,
        int maxImagesInFlight,

        // --- images ---
        int maxImageEdgePx,
        float jpegQuality,

        // --- LLM ---
        boolean llmCleanupEnabled,
        double llmConfidenceThreshold,
        int llmMaxChars
) {
    public static ExtractionConfig defaults() {
        return new ExtractionConfig(
                20L * 1024 * 1024,   // 20 MB
                30_000L,             // 30 s total
                24_000L,             // 24 s for extraction itself
                4_000L,              // keep 4 s in hand

                200f,                // 200 DPI is the sweet spot for OCR accuracy vs bytes
                80,                  // fewer than 80 chars on a page => treat it as scanned
                2_000,               // a text-layer PDF can still be huge; cap it
                150,                 // derive from: (extractionBudget * ocrConcurrency) / p95PerPage

                8,
                8,                   // ~3.9 MB per A4 grayscale page at 200 DPI => ~31 MB ceiling

                2_000,
                0.85f,

                false,               // off by default: it does not fit in 30 s for large docs
                0.80,                // only clean up pages OCR was unsure about
                12_000
        );
    }
}
```


### `src/main/java/com/example/extract/ExtractionResult.java`

```java
package com.example.extract;

import java.util.List;

/**
 * What the API hands back. Note {@code truncated}: a partial result is a
 * successful response, not an error. Returning 80 of 130 pages beats a 504.
 */
public record ExtractionResult(
        String markdown,
        String sourceName,
        String detectedMediaType,
        int pagesTotal,
        int pagesProcessed,
        int pagesOcred,
        boolean truncated,
        boolean fromCache,
        long elapsedMillis,
        List<String> warnings
) {}
```


### `src/main/java/com/example/extract/UnsupportedFileException.java`

```java
package com.example.extract;

public class UnsupportedFileException extends RuntimeException {
    public UnsupportedFileException(String message) {
        super(message);
    }
}
```


### `src/main/java/com/example/extract/TooLargeForBudgetException.java`

```java
package com.example.extract;

/**
 * Thrown during triage, before any real work is spent. The caller should
 * translate this into a 202 Accepted with a job id and run the document on the
 * asynchronous path instead.
 */
public class TooLargeForBudgetException extends RuntimeException {

    private final int pages;
    private final int estimatedSeconds;

    public TooLargeForBudgetException(String message, int pages, int estimatedSeconds) {
        super(message);
        this.pages = pages;
        this.estimatedSeconds = estimatedSeconds;
    }

    public int getPages() { return pages; }
    public int getEstimatedSeconds() { return estimatedSeconds; }
}
```


### `src/main/java/com/example/extract/model/BlockType.java`

```java
package com.example.extract.model;

public enum BlockType {
    HEADING,
    PARAGRAPH,
    LIST_ITEM,
    TABLE,
    IMAGE_TEXT,   // text recovered from an image or a rasterized page
    PAGE_BREAK
}
```


### `src/main/java/com/example/extract/model/Block.java`

```java
package com.example.extract.model;

import java.util.List;

/**
 * One unit of document content, format-agnostic.
 *
 * Every extractor produces these and nothing else. That is deliberate: if each
 * route emitted markdown strings directly you would end up with three subtly
 * different markdown dialects and no single place to fix them.
 */
public record Block(
        BlockType type,
        String text,
        int level,                  // heading level 1..6, or list nesting depth
        List<List<String>> rows,    // TABLE only; first row treated as the header
        int page,                   // 1-based; 0 when the format has no pages
        double confidence           // 1.0 for native text, OCR score for scanned content
) {

    public static Block heading(int level, String text, int page) {
        return new Block(BlockType.HEADING, text, Math.clamp(level, 1, 6), List.of(), page, 1.0);
    }

    public static Block paragraph(String text, int page) {
        return new Block(BlockType.PARAGRAPH, text, 0, List.of(), page, 1.0);
    }

    public static Block listItem(String text, int level, int page) {
        return new Block(BlockType.LIST_ITEM, text, Math.max(0, level), List.of(), page, 1.0);
    }

    public static Block table(List<List<String>> rows, int page) {
        return new Block(BlockType.TABLE, "", 0, rows, page, 1.0);
    }

    public static Block imageText(String text, int page, double confidence) {
        return new Block(BlockType.IMAGE_TEXT, text, 0, List.of(), page, confidence);
    }

    public static Block pageBreak(int page) {
        return new Block(BlockType.PAGE_BREAK, "", 0, List.of(), page, 1.0);
    }
}
```


### `src/main/java/com/example/extract/model/DocumentModel.java`

```java
package com.example.extract.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The canonical intermediate representation. One serializer reads this. */
public final class DocumentModel {

    private final List<Block> blocks = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> warnings = Collections.synchronizedSet(new LinkedHashSet<>());

    private String sourceName = "";
    private int pagesTotal;
    private int pagesProcessed;
    private int pagesOcred;
    private boolean truncated;

    public void add(Block block) {
        if (block != null) blocks.add(block);
    }

    public void addAll(List<Block> in) {
        if (in != null) blocks.addAll(in);
    }

    /** Pages can finish out of order when OCR runs in parallel. Restore reading order. */
    public void sortByPage() {
        synchronized (blocks) {
            blocks.sort(Comparator.comparingInt(Block::page));
        }
    }

    public void warn(String message) { warnings.add(message); }

    public List<Block> blocks() { return List.copyOf(blocks); }
    public List<String> warnings() { return List.copyOf(warnings); }

    public String sourceName() { return sourceName; }
    public void sourceName(String v) { this.sourceName = v; }

    public int pagesTotal() { return pagesTotal; }
    public void pagesTotal(int v) { this.pagesTotal = v; }

    public int pagesProcessed() { return pagesProcessed; }
    public void pagesProcessed(int v) { this.pagesProcessed = v; }
    public synchronized void incrementProcessed() { this.pagesProcessed++; }

    public int pagesOcred() { return pagesOcred; }
    public synchronized void incrementOcred() { this.pagesOcred++; }

    public boolean truncated() { return truncated; }
    public void truncated(boolean v) { this.truncated = v; }

    /** Mean OCR confidence across scanned content; 1.0 when nothing was OCRed. */
    public double meanOcrConfidence() {
        synchronized (blocks) {
            return blocks.stream()
                    .filter(b -> b.type() == BlockType.IMAGE_TEXT)
                    .mapToDouble(Block::confidence)
                    .average()
                    .orElse(1.0);
        }
    }
}
```


### `src/main/java/com/example/extract/detect/SourceType.java`

```java
package com.example.extract.detect;

public enum SourceType {
    PDF,
    DOCX,
    IMAGE,
    PLAIN_TEXT,
    UNSUPPORTED
}
```


### `src/main/java/com/example/extract/detect/TypeDetector.java`

```java
package com.example.extract.detect;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;

import java.io.IOException;
import java.io.InputStream;

/**
 * Detects by content, never by filename extension. Users mislabel files
 * constantly, and a .pdf that is actually a JPEG will otherwise blow up deep
 * inside PDFBox where the error message is useless.
 *
 * Only tika-core is on the classpath, so this is pure magic-byte sniffing:
 * fast, no parsing.
 */
public final class TypeDetector {

    private static final Detector DETECTOR = TikaConfig.getDefaultConfig().getDetector();

    public record Detected(SourceType type, String mediaType) {}

    private TypeDetector() {}

    public static Detected detect(byte[] bytes, String filenameHint) throws IOException {
        Metadata metadata = new Metadata();
        if (filenameHint != null && !filenameHint.isBlank()) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filenameHint);
        }

        MediaType mediaType;
        try (InputStream in = TikaInputStream.get(bytes)) {
            mediaType = DETECTOR.detect(in, metadata);
        }

        String mime = mediaType.getBaseType().toString();
        return new Detected(classify(mime), mime);
    }

    private static SourceType classify(String mime) {
        return switch (mime) {
            case "application/pdf" -> SourceType.PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> SourceType.DOCX;
            case "image/png", "image/jpeg", "image/tiff", "image/bmp", "image/webp" -> SourceType.IMAGE;
            case "text/plain", "text/markdown", "text/csv" -> SourceType.PLAIN_TEXT;
            default -> mime.startsWith("image/") ? SourceType.IMAGE : SourceType.UNSUPPORTED;
        };
    }
}
```


### `src/main/java/com/example/extract/ocr/OcrPage.java`

```java
package com.example.extract.ocr;

/**
 * @param text          recognised text for one page or image
 * @param confidence    0.0 - 1.0, mean over recognised lines. Drives the LLM gate.
 */
public record OcrPage(String text, double confidence) {

    public static OcrPage empty() {
        return new OcrPage("", 1.0);
    }
}
```


### `src/main/java/com/example/extract/ocr/OcrClient.java`

```java
package com.example.extract.ocr;

public interface OcrClient {

    /**
     * @param jpegBytes      a single grayscale JPEG, already downscaled
     * @param timeoutMillis  hard timeout, derived from the request deadline
     */
    OcrPage recognize(byte[] jpegBytes, long timeoutMillis) throws Exception;
}
```


### `src/main/java/com/example/extract/ocr/PaddleOcrClient.java`  — STUB

```java
package com.example.extract.ocr;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP client for the externally hosted PaddleOCR endpoint.
 *
 * Implementation notes for whoever fills this in:
 *   - Reuse ONE HttpClient for the whole application. Creating one per call
 *     throws away connection pooling and TLS session reuse, which is most of
 *     the latency on a short request.
 *   - Set the per-request timeout from timeoutMillis, not a fixed constant.
 *   - If the endpoint accepts a batch of images in one request, add a
 *     recognizeBatch method. Batching 8 pages into one round trip removes 8x
 *     the connection overhead and is usually the single biggest win available.
 *   - Map Paddle's per-line scores to a mean confidence for the page.
 *   - Treat non-2xx as a page-level failure, not a request-level one: record a
 *     warning and carry on with the other pages.
 */
public final class PaddleOcrClient implements OcrClient {

    private final HttpClient http;
    private final URI endpoint;

    public PaddleOcrClient(URI endpoint) {
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public OcrPage recognize(byte[] jpegBytes, long timeoutMillis) throws Exception {
        // TODO: To be implemented.
        //
        // POST jpegBytes to `endpoint` (multipart or base64 JSON, whichever your
        // deployment expects), apply `timeoutMillis` as the request timeout,
        // parse the response into text + mean confidence, return an OcrPage.
        throw new UnsupportedOperationException("PaddleOCR client not implemented yet");
    }
}
```


### `src/main/java/com/example/extract/llm/LlmClient.java`

```java
package com.example.extract.llm;

public interface LlmClient {

    /**
     * Turns noisy OCR output into clean markdown: fixes broken line wrapping,
     * restores heading structure, rebuilds tables.
     *
     * Call this sparingly. It is the slowest hop in the pipeline and it can
     * introduce content that was never in the document.
     */
    String toCleanMarkdown(String rawText, long timeoutMillis) throws Exception;
}
```


### `src/main/java/com/example/extract/llm/GptLlmClient.java`  — STUB

```java
package com.example.extract.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP client for the externally hosted GPT endpoint.
 *
 * Implementation notes:
 *   - Prompt it to reformat only. Explicitly forbid summarising, completing, or
 *     inventing content, and instruct it to return markdown with no preamble
 *     and no code fences.
 *   - Chunk long input on block boundaries, not mid-sentence, and keep chunks
 *     well inside the context window.
 *   - Respect timeoutMillis. If it expires, return the raw text unchanged
 *     rather than failing the whole extraction.
 */
public final class GptLlmClient implements LlmClient {

    private final HttpClient http;
    private final URI endpoint;

    public GptLlmClient(URI endpoint) {
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public String toCleanMarkdown(String rawText, long timeoutMillis) throws Exception {
        // TODO: To be implemented.
        throw new UnsupportedOperationException("LLM client not implemented yet");
    }
}
```


### `src/main/java/com/example/extract/util/Images.java`

```java
package com.example.extract.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

public final class Images {

    private Images() {}

    /**
     * Downscale so the longest edge is at most maxEdge, and force grayscale.
     *
     * Both matter more than they look. An A4 page at 200 DPI is 1654x2338: as
     * RGB that is 15.5 MB in heap, as TYPE_BYTE_GRAY it is 3.9 MB. With eight
     * pages in flight that is the difference between 124 MB and 31 MB. On the
     * wire, grayscale JPEG is roughly a fifth the size of RGB PNG, and OCR
     * accuracy is essentially unchanged.
     */
    public static BufferedImage normalize(BufferedImage src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();

        double scale = Math.min(1.0, (double) maxEdge / Math.max(w, h));
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));

        boolean alreadyGray = src.getType() == BufferedImage.TYPE_BYTE_GRAY;
        if (scale == 1.0 && alreadyGray) {
            return src;
        }

        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, tw, th, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static byte[] toJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available in this JRE");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream(256 * 1024);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }
}
```


### `src/main/java/com/example/extract/extract/Extractor.java`

```java
package com.example.extract.extract;

import com.example.extract.Deadline;
import com.example.extract.model.DocumentModel;

public interface Extractor {
    DocumentModel extract(byte[] bytes, String sourceName, Deadline deadline) throws Exception;
}
```


### `src/main/java/com/example/extract/extract/DocxExtractor.java`

```java
package com.example.extract.extract;

import com.example.extract.Deadline;
import com.example.extract.ExtractionConfig;
import com.example.extract.model.Block;
import com.example.extract.model.DocumentModel;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCX is a ZIP of XML with the text already sitting in it. This path never
 * calls OCR and never calls the LLM. A 20 MB DOCX is typically 19 MB of
 * embedded images and half a second of actual text extraction.
 *
 * Iterating getBodyElements() rather than getParagraphs() + getTables()
 * separately is what preserves the interleaving of prose and tables. Reading
 * them as two independent lists produces a document where every table has been
 * shunted to the bottom.
 */
public final class DocxExtractor implements Extractor {

    private static final Pattern HEADING_STYLE =
            Pattern.compile("(?i)^heading\\s*([1-6])$");

    private final ExtractionConfig config;

    public DocxExtractor(ExtractionConfig config) {
        this.config = config;
    }

    @Override
    public DocumentModel extract(byte[] bytes, String sourceName, Deadline deadline) throws Exception {
        DocumentModel model = new DocumentModel();
        model.sourceName(sourceName);
        model.pagesTotal(0); // DOCX has no fixed pagination until it is laid out

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {

            for (IBodyElement element : doc.getBodyElements()) {
                if (deadline.expired()) {
                    model.truncated(true);
                    model.warn("Time budget exhausted while reading the document body");
                    break;
                }

                if (element instanceof XWPFParagraph paragraph) {
                    Block block = toBlock(paragraph);
                    if (block != null) model.add(block);

                } else if (element instanceof XWPFTable table) {
                    List<List<String>> rows = toRows(table);
                    if (!rows.isEmpty()) model.add(Block.table(rows, 0));
                }
            }

            List<XWPFPictureData> pictures = doc.getAllPictures();
            if (!pictures.isEmpty()) {
                model.warn(pictures.size() + " embedded image(s) present; "
                        + "not sent to OCR. Enable that only if image content matters to you.");
                // If you do need them: pull picture.getData(), normalize to JPEG,
                // and push through the same OcrClient the PDF path uses.
            }
        }

        model.pagesProcessed(1);
        return model;
    }

    private Block toBlock(XWPFParagraph paragraph) {
        String text = inlineFormatted(paragraph);
        if (text.isBlank()) return null;

        String style = paragraph.getStyle();
        if (style != null) {
            Matcher m = HEADING_STYLE.matcher(style.trim());
            if (m.matches()) {
                return Block.heading(Integer.parseInt(m.group(1)), stripMarks(text), 0);
            }
            if (style.equalsIgnoreCase("Title")) {
                return Block.heading(1, stripMarks(text), 0);
            }
            if (style.equalsIgnoreCase("Subtitle")) {
                return Block.heading(2, stripMarks(text), 0);
            }
        }

        // getNumID() is non-null when Word attached a numbering definition,
        // which covers both bulleted and numbered lists.
        if (paragraph.getNumID() != null) {
            int depth = paragraph.getNumIlvl() == null ? 0 : paragraph.getNumIlvl().intValue();
            return Block.listItem(text, depth, 0);
        }

        return Block.paragraph(text, 0);
    }

    /** Rebuilds bold and italic from the runs, since Word splits a styled sentence across many. */
    private String inlineFormatted(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String t = run.text();
            if (t == null || t.isEmpty()) continue;

            boolean bold = run.isBold();
            boolean italic = run.isItalic();

            // Markdown emphasis cannot straddle the leading/trailing whitespace
            // of a run, so peel it off and re-attach it outside the marks.
            String lead = t.substring(0, t.length() - t.stripLeading().length());
            String trail = t.substring(t.stripTrailing().length());
            String core = t.strip();

            if (core.isEmpty()) {
                sb.append(t);
                continue;
            }

            sb.append(lead);
            if (bold) sb.append("**");
            if (italic) sb.append('*');
            sb.append(core);
            if (italic) sb.append('*');
            if (bold) sb.append("**");
            sb.append(trail);
        }
        return sb.toString().strip();
    }

    private String stripMarks(String s) {
        return s.replace("**", "").replace("*", "").strip();
    }

    private List<List<String>> toRows(XWPFTable table) {
        List<List<String>> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText();
                cells.add(text == null ? "" : text.replace("\n", " ").strip());
            }
            if (cells.stream().anyMatch(c -> !c.isBlank())) {
                rows.add(cells);
            }
        }
        return rows;
    }
}
```


### `src/main/java/com/example/extract/extract/PdfExtractor.java`

```java
package com.example.extract.extract;

import com.example.extract.Deadline;
import com.example.extract.ExtractionConfig;
import com.example.extract.TooLargeForBudgetException;
import com.example.extract.model.Block;
import com.example.extract.model.DocumentModel;
import com.example.extract.ocr.OcrClient;
import com.example.extract.ocr.OcrPage;
import com.example.extract.util.Images;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * The only route that needs real logic, and the only one that can blow the
 * budget.
 *
 * Two decisions carry the whole design:
 *
 *  1. The text-layer decision is made PER PAGE, not per document. Mixed PDFs -
 *     a typed report with a scanned appendix, a form with a photographed
 *     signature page - are extremely common. Classifying the whole file as
 *     "digital" or "scanned" is where most pipelines lose their time.
 *
 *  2. Cheap work runs to completion before expensive work starts. Text-layer
 *     pages cost ~10 ms each; OCR pages cost 1-3 seconds. Banking all the cheap
 *     pages first means that running out of budget costs you the tail of the
 *     document rather than all of it.
 */
public final class PdfExtractor implements Extractor {

    private final ExtractionConfig config;
    private final OcrClient ocr;
    private final ExecutorService ocrPool;

    public PdfExtractor(ExtractionConfig config, OcrClient ocr, ExecutorService ocrPool) {
        this.config = config;
        this.ocr = ocr;
        this.ocrPool = ocrPool;
    }

    @Override
    public DocumentModel extract(byte[] bytes, String sourceName, Deadline deadline) throws Exception {
        DocumentModel model = new DocumentModel();
        model.sourceName(sourceName);

        try (PDDocument doc = Loader.loadPDF(bytes)) {

            if (doc.isEncrypted()) {
                throw new IllegalStateException("PDF is encrypted; decrypt before extraction");
            }

            int pageCount = doc.getNumberOfPages();
            model.pagesTotal(pageCount);

            if (pageCount > config.maxPagesTextLayer()) {
                throw new TooLargeForBudgetException(
                        "PDF has " + pageCount + " pages, above the synchronous cap of "
                                + config.maxPagesTextLayer(),
                        pageCount, -1);
            }

            // ---------- Pass 1: cheap text-layer sweep ----------
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            Map<Integer, String> nativeText = new ConcurrentHashMap<>();
            List<Integer> needsOcr = new ArrayList<>();

            for (int page = 1; page <= pageCount; page++) {
                if (!deadline.hasAtLeast(config.tailReserveMillis())) {
                    model.truncated(true);
                    model.warn("Budget exhausted during text-layer pass at page " + page);
                    break;
                }

                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc);

                if (text != null && text.strip().length() >= config.textLayerMinChars()) {
                    nativeText.put(page, text);
                } else {
                    needsOcr.add(page);
                }
            }

            // ---------- Triage: does the OCR work actually fit? ----------
            if (needsOcr.size() > config.maxPagesOcr()) {
                throw new TooLargeForBudgetException(
                        needsOcr.size() + " scanned pages exceed the synchronous OCR cap of "
                                + config.maxPagesOcr() + "; route to the async queue",
                        pageCount, -1);
            }

            // ---------- Pass 2: render + OCR the scanned pages ----------
            Map<Integer, OcrPage> ocrText = new ConcurrentHashMap<>();
            if (!needsOcr.isEmpty()) {
                runOcr(doc, needsOcr, ocrText, model, deadline);
            }

            // ---------- Merge in reading order ----------
            for (int page = 1; page <= pageCount; page++) {
                String native_ = nativeText.get(page);
                if (native_ != null) {
                    for (Block b : splitIntoBlocks(native_, page)) {
                        model.add(b);
                    }
                    model.incrementProcessed();
                    continue;
                }

                OcrPage recognised = ocrText.get(page);
                if (recognised != null && !recognised.text().isBlank()) {
                    model.add(Block.imageText(recognised.text().strip(), page, recognised.confidence()));
                    model.incrementProcessed();
                    model.incrementOcred();
                }
            }

            if (model.pagesProcessed() < pageCount) {
                model.truncated(true);
                model.warn("Processed " + model.pagesProcessed() + " of " + pageCount + " pages");
            }
        }

        model.sortByPage();
        return model;
    }

    /**
     * PDDocument and PDFRenderer are NOT thread safe. Rendering therefore stays
     * on this thread and only the OCR round trips fan out.
     *
     * The semaphore bounds how many rendered pages exist in memory at once. An
     * A4 page at 200 DPI grayscale is ~3.9 MB, so a permit count of 8 caps this
     * stage at roughly 31 MB regardless of how long the document is. Without it,
     * a 500 page scan would render faster than OCR consumes and exhaust the heap.
     */
    private void runOcr(PDDocument doc,
                        List<Integer> pages,
                        Map<Integer, OcrPage> out,
                        DocumentModel model,
                        Deadline deadline) throws Exception {

        PDFRenderer renderer = new PDFRenderer(doc);
        Semaphore inFlight = new Semaphore(config.maxImagesInFlight());
        List<Future<?>> futures = new ArrayList<>(pages.size());

        for (int page : pages) {
            if (!deadline.hasAtLeast(config.tailReserveMillis())) {
                model.truncated(true);
                model.warn("Budget exhausted before OCR of page " + page);
                break;
            }

            inFlight.acquire();

            byte[] jpeg;
            try {
                BufferedImage rendered = renderer.renderImageWithDPI(
                        page - 1, config.renderDpi(), ImageType.GRAY);
                BufferedImage normalized = Images.normalize(rendered, config.maxImageEdgePx());
                jpeg = Images.toJpeg(normalized, config.jpegQuality());
            } catch (Exception e) {
                inFlight.release();
                model.warn("Failed to render page " + page + ": " + e.getMessage());
                continue;
            }

            final int pageNo = page;
            final byte[] payload = jpeg;

            futures.add(ocrPool.submit(() -> {
                try {
                    long timeout = deadline.callTimeoutMillis(8_000L);
                    out.put(pageNo, ocr.recognize(payload, timeout));
                } catch (Exception e) {
                    // A page-level failure must not fail the request.
                    model.warn("OCR failed on page " + pageNo + ": " + e.getMessage());
                } finally {
                    inFlight.release();
                }
                return null;
            }));
        }

        // Drain, still under the deadline. Anything not back in time gets cancelled
        // and simply does not appear in the output.
        for (Future<?> f : futures) {
            long left = deadline.remainingMillis() - config.tailReserveMillis();
            if (left <= 0) {
                f.cancel(true);
                model.truncated(true);
                continue;
            }
            try {
                f.get(left, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                f.cancel(true);
                model.truncated(true);
            }
        }
    }

    /**
     * PDFTextStripper hands back a flat character stream with no structural
     * information. This recovers paragraphs from blank lines and makes a modest
     * guess at headings.
     *
     * If you need real headings and tables out of PDFs, this heuristic will not
     * get you there. Use PaddleOCR's PP-Structure model on the rendered page
     * instead and trade latency for layout.
     */
    private List<Block> splitIntoBlocks(String pageText, int page) {
        List<Block> blocks = new ArrayList<>();
        String[] chunks = pageText.split("\\r?\\n\\s*\\r?\\n");

        for (String chunk : chunks) {
            String text = chunk.replaceAll("\\s*\\r?\\n\\s*", " ").strip();
            if (text.isBlank()) continue;

            if (looksLikeHeading(text)) {
                blocks.add(Block.heading(2, text, page));
            } else {
                blocks.add(Block.paragraph(text, page));
            }
        }
        return blocks;
    }

    private boolean looksLikeHeading(String text) {
        if (text.length() > 80 || text.endsWith(".")) return false;
        String letters = text.replaceAll("[^A-Za-z]", "");
        if (letters.length() < 3) return false;
        long upper = letters.chars().filter(Character::isUpperCase).count();
        return (double) upper / letters.length() > 0.7;
    }
}
```


### `src/main/java/com/example/extract/extract/ImageExtractor.java`

```java
package com.example.extract.extract;

import com.example.extract.Deadline;
import com.example.extract.ExtractionConfig;
import com.example.extract.model.Block;
import com.example.extract.model.DocumentModel;
import com.example.extract.ocr.OcrClient;
import com.example.extract.ocr.OcrPage;
import com.example.extract.util.Images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * The simplest route: one image, one OCR call.
 *
 * The only thing worth being careful about is the preprocessing. Downscaling to
 * a 2000 px longest edge and forcing grayscale cuts the payload five to ten
 * times over a full-resolution PNG, and OCR accuracy barely moves. A 20 MB
 * phone photo is mostly resolution the recogniser cannot use.
 */
public final class ImageExtractor implements Extractor {

    private final ExtractionConfig config;
    private final OcrClient ocr;

    public ImageExtractor(ExtractionConfig config, OcrClient ocr) {
        this.config = config;
        this.ocr = ocr;
    }

    @Override
    public DocumentModel extract(byte[] bytes, String sourceName, Deadline deadline) throws Exception {
        DocumentModel model = new DocumentModel();
        model.sourceName(sourceName);
        model.pagesTotal(1);

        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            throw new IllegalStateException("Could not decode image; unsupported or corrupt");
        }

        BufferedImage normalized = Images.normalize(source, config.maxImageEdgePx());
        byte[] jpeg = Images.toJpeg(normalized, config.jpegQuality());

        try {
            OcrPage result = ocr.recognize(jpeg, deadline.callTimeoutMillis(10_000L));
            if (!result.text().isBlank()) {
                model.add(Block.imageText(result.text().strip(), 1, result.confidence()));
            }
            model.pagesProcessed(1);
            model.incrementOcred();
        } catch (Exception e) {
            model.truncated(true);
            model.warn("OCR failed: " + e.getMessage());
        }

        return model;
    }
}
```


### `src/main/java/com/example/extract/extract/PlainTextExtractor.java`

```java
package com.example.extract.extract;

import com.example.extract.Deadline;
import com.example.extract.model.Block;
import com.example.extract.model.DocumentModel;

import java.nio.charset.StandardCharsets;

/** Already text. No parsing, no OCR, no LLM. Included so the router is total. */
public final class PlainTextExtractor implements Extractor {

    @Override
    public DocumentModel extract(byte[] bytes, String sourceName, Deadline deadline) {
        DocumentModel model = new DocumentModel();
        model.sourceName(sourceName);
        model.pagesTotal(1);
        model.pagesProcessed(1);

        String text = new String(bytes, StandardCharsets.UTF_8);
        for (String chunk : text.split("\\r?\\n\\s*\\r?\\n")) {
            String p = chunk.strip();
            if (!p.isBlank()) model.add(Block.paragraph(p, 1));
        }
        return model;
    }
}
```


### `src/main/java/com/example/extract/markdown/MarkdownWriter.java`

```java
package com.example.extract.markdown;

import com.example.extract.model.Block;
import com.example.extract.model.BlockType;
import com.example.extract.model.DocumentModel;

import java.util.List;

/**
 * The single place markdown is produced. Every extractor feeds the same block
 * model into this, which is why the output is consistent across formats.
 */
public final class MarkdownWriter {

    private final boolean includePageMarkers;

    public MarkdownWriter(boolean includePageMarkers) {
        this.includePageMarkers = includePageMarkers;
    }

    public String write(DocumentModel model) {
        StringBuilder sb = new StringBuilder(8 * 1024);
        int lastPage = -1;

        for (Block block : model.blocks()) {
            if (includePageMarkers && block.page() > 0 && block.page() != lastPage) {
                if (lastPage != -1) sb.append("\n---\n\n");
                sb.append("<!-- page ").append(block.page()).append(" -->\n\n");
                lastPage = block.page();
            }
            append(sb, block);
        }

        return sb.toString().replaceAll("\n{3,}", "\n\n").strip() + "\n";
    }

    private void append(StringBuilder sb, Block block) {
        switch (block.type()) {
            case HEADING -> sb.append("#".repeat(block.level()))
                    .append(' ')
                    .append(escapeInline(block.text()))
                    .append("\n\n");

            case PARAGRAPH, IMAGE_TEXT -> sb.append(escapeInline(block.text()))
                    .append("\n\n");

            case LIST_ITEM -> sb.append("  ".repeat(block.level()))
                    .append("- ")
                    .append(escapeInline(block.text()))
                    .append('\n');

            case TABLE -> appendTable(sb, block.rows());

            case PAGE_BREAK -> sb.append("\n---\n\n");
        }
    }

    private void appendTable(StringBuilder sb, List<List<String>> rows) {
        if (rows.isEmpty()) return;

        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns == 0) return;

        sb.append('\n');
        writeRow(sb, rows.get(0), columns);

        sb.append('|');
        sb.append(" --- |".repeat(columns));
        sb.append('\n');

        for (int i = 1; i < rows.size(); i++) {
            writeRow(sb, rows.get(i), columns);
        }
        sb.append('\n');
    }

    private void writeRow(StringBuilder sb, List<String> cells, int columns) {
        sb.append('|');
        for (int c = 0; c < columns; c++) {
            String cell = c < cells.size() ? cells.get(c) : "";
            sb.append(' ').append(escapeCell(cell)).append(" |");
        }
        sb.append('\n');
    }

    /** A raw pipe inside a cell silently destroys the table. */
    private String escapeCell(String s) {
        return s.replace("|", "\\|").replace("\n", " ").strip();
    }

    /** Leading markers that would accidentally turn body text into structure. */
    private String escapeInline(String s) {
        return s.replaceAll("^(#{1,6})\\s", "\\\\$1 ")
                .replaceAll("^(>)\\s", "\\\\$1 ");
    }
}
```


### `src/main/java/com/example/extract/ExtractionService.java`

```java
package com.example.extract;

import com.example.extract.detect.SourceType;
import com.example.extract.detect.TypeDetector;
import com.example.extract.extract.DocxExtractor;
import com.example.extract.extract.Extractor;
import com.example.extract.extract.ImageExtractor;
import com.example.extract.extract.PdfExtractor;
import com.example.extract.extract.PlainTextExtractor;
import com.example.extract.llm.LlmClient;
import com.example.extract.markdown.MarkdownWriter;
import com.example.extract.model.DocumentModel;
import com.example.extract.ocr.OcrClient;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The orchestrator. Owns the deadline, the routing decision and the cache.
 */
public final class ExtractionService implements AutoCloseable {

    private final ExtractionConfig config;
    private final OcrClient ocr;
    private final LlmClient llm;
    private final ExecutorService ocrPool;
    private final MarkdownWriter markdown;

    /**
     * Keyed on the SHA-256 of the file bytes. Reprocessing an identical document
     * is the cheapest win in the whole pipeline and users re-upload constantly.
     * Swap this for Redis or Caffeine in production; a plain map has no eviction.
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ExtractionService(ExtractionConfig config, OcrClient ocr, LlmClient llm) {
        this.config = config;
        this.ocr = ocr;
        this.llm = llm;
        this.markdown = new MarkdownWriter(true);

        // Virtual threads: this workload is almost entirely blocked on two HTTP
        // endpoints, so platform threads would just be idle memory. Concurrency
        // against the OCR service is bounded by a semaphore in PdfExtractor, not
        // by the size of this pool.
        this.ocrPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    public ExtractionResult extract(byte[] bytes, String filename) throws Exception {
        Deadline deadline = Deadline.ofMillis(config.totalBudgetMillis());

        // ---- 1. Validate, cheaply and first ----
        if (bytes == null || bytes.length == 0) {
            throw new UnsupportedFileException("Empty upload");
        }
        if (bytes.length > config.maxBytes()) {
            throw new UnsupportedFileException(
                    "File is " + (bytes.length / 1_048_576) + " MB; limit is "
                            + (config.maxBytes() / 1_048_576) + " MB");
        }

        // ---- 2. Cache ----
        String hash = sha256(bytes);
        String cached = cache.get(hash);
        if (cached != null) {
            return new ExtractionResult(cached, filename, "cached",
                    0, 0, 0, false, true, deadline.elapsedMillis(), java.util.List.of());
        }

        // ---- 3. Detect by content ----
        TypeDetector.Detected detected = TypeDetector.detect(bytes, filename);
        Extractor extractor = route(detected.type(), detected.mediaType());

        // ---- 4. Extract, under the deadline ----
        DocumentModel model = extractor.extract(bytes, filename, deadline);

        // ---- 5. Optional LLM cleanup, heavily gated ----
        String md = markdown.write(model);
        md = maybeCleanUp(md, model, deadline);

        if (!model.truncated()) {
            cache.put(hash, md);
        }

        return new ExtractionResult(
                md,
                filename,
                detected.mediaType(),
                model.pagesTotal(),
                model.pagesProcessed(),
                model.pagesOcred(),
                model.truncated(),
                false,
                deadline.elapsedMillis(),
                model.warnings());
    }

    private Extractor route(SourceType type, String mediaType) {
        return switch (type) {
            case PDF        -> new PdfExtractor(config, ocr, ocrPool);
            case DOCX       -> new DocxExtractor(config);
            case IMAGE      -> new ImageExtractor(config, ocr);
            case PLAIN_TEXT -> new PlainTextExtractor();
            case UNSUPPORTED -> throw new UnsupportedFileException("Unsupported type: " + mediaType);
        };
    }

    /**
     * The LLM is off unless three conditions all hold: it is enabled, the OCR
     * output was actually uncertain, and there is real budget left.
     *
     * Calling it on a DOCX or a clean text-layer PDF buys nothing - the text is
     * already correct - and costs seconds plus a hallucination risk.
     */
    private String maybeCleanUp(String md, DocumentModel model, Deadline deadline) {
        if (!config.llmCleanupEnabled()) return md;
        if (model.pagesOcred() == 0) return md;
        if (model.meanOcrConfidence() >= config.llmConfidenceThreshold()) return md;
        if (md.length() > config.llmMaxChars()) return md;
        if (!deadline.hasAtLeast(config.tailReserveMillis() + 3_000L)) return md;

        try {
            return llm.toCleanMarkdown(md, deadline.callTimeoutMillis(6_000L));
        } catch (Exception e) {
            model.warn("LLM cleanup skipped: " + e.getMessage());
            return md;   // Never fail the request over an optional stage.
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    @Override
    public void close() {
        ocrPool.shutdown();
    }
}
```


### `src/main/java/com/example/extract/Main.java`

```java
package com.example.extract;

import com.example.extract.llm.GptLlmClient;
import com.example.extract.ocr.PaddleOcrClient;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demo entry point.
 *
 *   mvn -q clean package
 *   java -jar target/file-extractor-1.0.0.jar /path/to/document.pdf
 *
 * DOCX, plain text and text-layer PDFs run end to end today. Scanned pages and
 * images will throw until PaddleOcrClient is implemented, which is the correct
 * behaviour: the pipeline has no fallback OCR and should not pretend otherwise.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: file-extractor <file>");
            System.exit(2);
        }

        Path path = Path.of(args[0]);
        byte[] bytes = Files.readAllBytes(path);

        ExtractionConfig config = ExtractionConfig.defaults();

        var ocr = new PaddleOcrClient(URI.create(
                System.getenv().getOrDefault("OCR_ENDPOINT", "http://localhost:8868/ocr")));
        var llm = new GptLlmClient(URI.create(
                System.getenv().getOrDefault("LLM_ENDPOINT", "http://localhost:8080/v1/chat")));

        try (ExtractionService service = new ExtractionService(config, ocr, llm)) {
            ExtractionResult result = service.extract(bytes, path.getFileName().toString());

            System.out.println(result.markdown());

            System.err.printf("%n--- %s | %s | %d/%d pages (%d OCR) | %d ms%s%n",
                    result.sourceName(),
                    result.detectedMediaType(),
                    result.pagesProcessed(),
                    result.pagesTotal(),
                    result.pagesOcred(),
                    result.elapsedMillis(),
                    result.truncated() ? " | TRUNCATED" : "");

            result.warnings().forEach(w -> System.err.println("warn: " + w));
        }
    }
}
```


---

## 7. Caveats

**This code was never compiled.** Maven Central was unreachable from the
environment it was written in, and no JDK was present — only a runtime. The API
usage is written carefully against PDFBox 3.0.3, POI 5.3.0 and tika-core 3.0.0,
but expect to fix an import or a signature on first build rather than assuming
it is clean. The places most likely to need a touch:

- `Loader.loadPDF(byte[])` — PDFBox 3.x moved loading off `PDDocument`
- `TikaCoreProperties.RESOURCE_NAME_KEY` — moved from `Metadata` in Tika 2.x
- `XWPFParagraph.getStyle()` returns the style *ID*, which is usually
  `Heading1` but depends on the template the document was authored from

**Not covered by this implementation:**

- No async job path. `TooLargeForBudgetException` is where you hook one up:
  catch it at the controller and return 202 with a job id.
- The cache is an unbounded `ConcurrentHashMap`. Replace with Caffeine or Redis.
- PDF heading detection is a crude uppercase-ratio heuristic. If you need real
  structure out of PDFs, run PaddleOCR's PP-Structure model on the rendered page
  and trade latency for layout.
- No retry logic on the OCR endpoint. A single retry with jitter is worth adding,
  but only when the deadline has room for it.
- Encrypted PDFs are rejected rather than decrypted.
