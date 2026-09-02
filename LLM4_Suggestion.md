# File Processing Discussion + Complete Java Reference Implementation

This document consolidates the full discussion in this chat about uploaded-file processing, the recommended architecture for a Java platform, the 20 MB / 30-second SLA design, and the complete Java reference implementation.

---

# Conversation

## User

May I ask how are you processing the files when I upload it?
Is there any difference between smaller and bigger file handling?
Put a small sequence detailing how you are processing files when uploaded. What is happening in client, server, OCR, LLM, etc., when a file is uploaded.

I have a requirement in my application where I need to read and understand what is inside the file. I might get DOCX, PDF (with or without images), images (PNG/JPEG). I have PaddleOCR and a GPT LLM at my disposal; they are hosted outside my application as two different endpoints.

The requirement is to process the files as fast as possible. I need to process this in a Java platform, and the file-processing output should always be Markdown.

1. Provide information on how ChatGPT/OpenAI-style processing works at a high level.
2. Provide information on how I should implement the processing.

## Assistant

The key design principle is: **do not OCR everything and do not send every entire file directly to the LLM.** Fast document systems first determine what can be extracted cheaply, then use OCR/vision only where necessary.

### High-level file-processing model

A publicly describable conceptual flow is:

```text
Browser / Client
      │
      │ Upload PDF / DOCX / PNG / JPEG
      ▼
┌─────────────────────┐
│ File Upload Service │
│ validation/storage  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ File Pre-processing │
│ • detect file type  │
│ • extract content   │
│ • parse structure   │
│ • index/chunk       │
└──────────┬──────────┘
           │
     ┌─────┴─────────┐
     │               │
     ▼               ▼
Digital text      Image / visual
extraction        understanding
     │               │
     └──────┬────────┘
            ▼
     Relevant content
       is retrieved
            │
            ▼
          LLM
            │
            ▼
     User response
```

For small documents, most or all extracted content can often be handled directly. For large documents, the better model is parse → chunk/index → retrieve relevant sections → LLM, rather than sending hundreds of pages in one model call.

### Recommended Java pipeline

```text
                    ┌──────────────────┐
                    │ Java REST API    │
                    │ Multipart Upload │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │ File Classifier  │
                    │ PDF/DOCX/Image   │
                    └────────┬─────────┘
                             │
             ┌───────────────┼────────────────┐
             │               │                │
             ▼               ▼                ▼
           DOCX             PDF          PNG / JPEG
             │               │                │
             ▼               ▼                │
       Apache POI       PDFBox detect          │
       extract text     text per page          │
             │               │                │
             │         ┌─────┴─────┐          │
             │         │           │          │
             │      Text PDF    Scanned PDF   │
             │         │           │          │
             │         │        PDF → Image   │
             │         │           │          │
             │         │           ▼          ▼
             │         │       PaddleOCR ─────┘
             │         │           │
             └─────────┴─────┬─────┘
                             ▼
                 ┌────────────────────┐
                 │ Normalized Content │
                 │ pages/blocks/table │
                 └─────────┬──────────┘
                           │
                           ▼
                 ┌────────────────────┐
                 │ GPT LLM            │
                 │ Understand +       │
                 │ normalize structure│
                 └─────────┬──────────┘
                           │
                           ▼
                    Markdown Output
```

For PDFs, determine OCR need **per page**, not per document. If PDFBox extracts useful text from a page, skip OCR. If a page has little or no text, render only that page and send it to PaddleOCR.

For DOCX, Apache POI should extract paragraphs, headings, lists, and tables directly. OCR should only be considered for embedded images that actually contain text.

For PNG/JPEG, send the image to PaddleOCR. If the GPT endpoint supports vision, a future enhancement is to combine OCR text with vision-based structure understanding.

Markdown should be the canonical output format. Internally, use an intermediate document model such as `Document → Page → Block`, then render that model into Markdown.

The LLM should be used mainly for semantic cleanup and understanding, not as the basic parser. A good conceptual pipeline is:

```text
File
 ↓
Deterministic extraction
 ↓
OCR only where required
 ↓
Structured intermediate format
 ↓
GPT
 ↓
Semantic cleanup / understanding
 ↓
Markdown
```

---

## User

1. How should I process the files if the requirement is to support a maximum file size of 20 MB and processing time should be less than 30 seconds?
2. Give a small sequence explaining how the flow should be.
3. Provide a Java application which does all the extraction with the help of libraries. No need to include code for PaddleOCR and LLM - just put comments as `To be implemented`.

## Assistant

For a **20 MB / <30 second** target, do not use file size alone as the complexity indicator. A small compressed scanned PDF can contain many image pages and be more expensive than a larger text-native PDF.

### Suggested processing budget

| Stage | Target budget |
|---|---:|
| Upload + validation | 1-2 sec |
| Native extraction | 2-4 sec |
| OCR where required | 10-15 sec |
| GPT understanding / Markdown cleanup | 5-8 sec |
| Headroom | 2-3 sec |
| **Total** | **<30 sec** |

Recommended controls:

```text
Max file            = 20 MB
Max PDF pages        = e.g. 100
Max image dimensions = configurable
Processing deadline  = 28 sec
OCR concurrency      = 4
```

A strict `<30 seconds for every possible 20 MB file>` cannot be guaranteed unless you also constrain page count, pixel count, or have a timeout/degraded-result policy.

### Recommended sequence

```text
Client
  │
  │ multipart upload
  ▼
Java Spring Boot API
  │
  ├── Validate <= 20 MB
  ├── Detect PDF / DOCX / Image
  │
  ▼
Extractor
  │
  ├── DOCX ──► Apache POI ───────────────┐
  │                                      │
  ├── PDF ──► PDFBox                    │
  │             │                        │
  │             ├─ text found ──────────┤
  │             │                        │
  │             └─ scanned page          │
  │                    │                 │
  │                    ▼                 │
  │                PaddleOCR             │
  │                                      │
  └── PNG/JPEG ─────► PaddleOCR          │
                         │                │
                         └────────────────┤
                                          ▼
                                Normalized document
                                          │
                                          ▼
                                      GPT LLM
                               understand / cleanup
                                          │
                                          ▼
                                      Markdown
                                          │
                                          ▼
                                        Client
```

The main optimization is:

```text
PDF page has usable text?
        │
      YES ──► PDFBox text
        │
       NO ──► Render page → PaddleOCR
```

Use bounded parallelism for OCR, for example 4 concurrent calls, rather than firing every page at PaddleOCR simultaneously. Java virtual threads are a good fit for this once OCR calls are implemented.

---

# Complete Java Reference Implementation

The following source code is the complete project created for this design. PaddleOCR and GPT integration points are deliberately left as extension points with TODO comments.

## `README.md`

```markdown
# Document Markdown Extractor

Fast-path Java/Spring Boot sample for files up to 20 MB. Supported input: PDF, DOCX, PNG, JPG/JPEG. Output is Markdown.

## Processing strategy

- PDF: PDFBox native text extraction per page. If a page has too little text, render only that page and route it to the OCR placeholder.
- DOCX: Apache POI extracts paragraphs, headings and tables directly; embedded pictures are routed to the OCR placeholder.
- PNG/JPEG: routed to the OCR placeholder.
- Markdown is rendered deterministically, then passed through an LLM placeholder that currently returns it unchanged.
- An internal 28-second deadline leaves roughly 2 seconds of headroom against a 30-second external SLA.

## Run

```bash
mvn spring-boot:run
```

## Test API

Markdown only:

```bash
curl -X POST http://localhost:8080/api/documents/extract \
  -F 'file=@sample.pdf'
```

With processing metadata:

```bash
curl -X POST http://localhost:8080/api/documents/extract/details \
  -F 'file=@sample.docx'
```

## External integrations intentionally not implemented

- `OcrClient.extractText(...)`: TODO call PaddleOCR.
- `LlmClient.normalizeMarkdown(...)`: TODO call GPT LLM.

Use HTTP connect/read timeouts derived from `ProcessingDeadline.remaining()` so downstream calls cannot overrun the SLA.

## Important production improvements

1. Do not use original filename/content-type alone for security-sensitive type detection; validate magic bytes/content signatures.
2. Add encrypted/password-protected PDF handling and page-count/pixel-count limits.
3. Add metrics for parse time, OCR time, LLM time, pages OCR'd, timeout rate and file type.
4. For large scanned documents, batch/parallelize OCR with a bounded concurrency based on PaddleOCR capacity. Do not create unbounded parallel calls.
5. If semantic understanding is mandatory, reserve an explicit LLM budget (for example 5-8 seconds) instead of allowing extraction/OCR to consume the entire 28 seconds.
```

## `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>document-markdown-extractor</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>document-markdown-extractor</name>
    <properties>
        <java.version>21</java.version>
        <pdfbox.version>3.0.8</pdfbox.version>
        <poi.version>5.5.1</poi.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>${pdfbox.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>${poi.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## `src/main/java/com/example/docproc/DocumentMarkdownExtractorApplication.java`

```java
package com.example.docproc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentMarkdownExtractorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentMarkdownExtractorApplication.class, args);
    }
}
```

## `src/main/java/com/example/docproc/api/ApiExceptionHandler.java`

```java
package com.example.docproc.api;

import com.example.docproc.service.ProcessingTimeoutException;
import com.example.docproc.service.UnsupportedDocumentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, UnsupportedDocumentException.class})
    ProblemDetail badRequest(RuntimeException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(ProcessingTimeoutException.class)
    ProblemDetail timeout(ProcessingTimeoutException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.REQUEST_TIMEOUT);
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(IOException.class)
    ProblemDetail extractionError(IOException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        detail.setDetail("Unable to extract document: " + ex.getMessage());
        return detail;
    }
}
```

## `src/main/java/com/example/docproc/api/DocumentController.java`

```java
package com.example.docproc.api;

import com.example.docproc.model.ProcessingResult;
import com.example.docproc.service.DocumentProcessingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentProcessingService service;

    public DocumentController(DocumentProcessingService service) {
        this.service = service;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "text/markdown;charset=UTF-8")
    public String extract(@RequestPart("file") MultipartFile file) throws IOException {
        return service.process(file).markdown();
    }

    @PostMapping(value = "/extract/details", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProcessingResult extractWithDetails(@RequestPart("file") MultipartFile file) throws IOException {
        return service.process(file);
    }
}
```

## `src/main/java/com/example/docproc/config/DocumentProcessingProperties.java`

```java
package com.example.docproc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "docproc")
public class DocumentProcessingProperties {
    private int timeoutSeconds = 28;
    private int pdfNativeTextThreshold = 40;
    private int maxOcrConcurrency = 4;
    private int pdfRenderDpi = 144;

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getPdfNativeTextThreshold() { return pdfNativeTextThreshold; }
    public void setPdfNativeTextThreshold(int pdfNativeTextThreshold) { this.pdfNativeTextThreshold = pdfNativeTextThreshold; }
    public int getMaxOcrConcurrency() { return maxOcrConcurrency; }
    public void setMaxOcrConcurrency(int maxOcrConcurrency) { this.maxOcrConcurrency = maxOcrConcurrency; }
    public int getPdfRenderDpi() { return pdfRenderDpi; }
    public void setPdfRenderDpi(int pdfRenderDpi) { this.pdfRenderDpi = pdfRenderDpi; }
}
```

## `src/main/java/com/example/docproc/model/BlockType.java`

```java
package com.example.docproc.model;

public enum BlockType {
    HEADING, PARAGRAPH, TABLE, IMAGE_TEXT, PAGE_TEXT
}
```

## `src/main/java/com/example/docproc/model/DocumentBlock.java`

```java
package com.example.docproc.model;

public record DocumentBlock(BlockType type, String content) {}
```

## `src/main/java/com/example/docproc/model/ExtractedDocument.java`

```java
package com.example.docproc.model;

import java.util.List;

public record ExtractedDocument(String fileName, String mediaType, List<PageContent> pages) {}
```

## `src/main/java/com/example/docproc/model/PageContent.java`

```java
package com.example.docproc.model;

import java.util.List;

public record PageContent(int pageNumber, List<DocumentBlock> blocks) {}
```

## `src/main/java/com/example/docproc/model/ProcessingResult.java`

```java
package com.example.docproc.model;

public record ProcessingResult(String fileName, long processingTimeMs, String markdown) {}
```

## `src/main/java/com/example/docproc/service/DocumentProcessingService.java`

```java
package com.example.docproc.service;

import com.example.docproc.config.DocumentProcessingProperties;
import com.example.docproc.model.ExtractedDocument;
import com.example.docproc.model.ProcessingResult;
import com.example.docproc.service.external.LlmClient;
import com.example.docproc.service.extractor.DocxDocumentExtractor;
import com.example.docproc.service.extractor.DocumentExtractor;
import com.example.docproc.service.extractor.ImageDocumentExtractor;
import com.example.docproc.service.extractor.PdfDocumentExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

@Service
public class DocumentProcessingService {
    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private final PdfDocumentExtractor pdfExtractor;
    private final DocxDocumentExtractor docxExtractor;
    private final ImageDocumentExtractor imageExtractor;
    private final MarkdownRenderer markdownRenderer;
    private final LlmClient llmClient;
    private final DocumentProcessingProperties properties;

    public DocumentProcessingService(PdfDocumentExtractor pdfExtractor,
                                     DocxDocumentExtractor docxExtractor,
                                     ImageDocumentExtractor imageExtractor,
                                     MarkdownRenderer markdownRenderer,
                                     LlmClient llmClient,
                                     DocumentProcessingProperties properties) {
        this.pdfExtractor = pdfExtractor;
        this.docxExtractor = docxExtractor;
        this.imageExtractor = imageExtractor;
        this.markdownRenderer = markdownRenderer;
        this.llmClient = llmClient;
        this.properties = properties;
    }

    public ProcessingResult process(MultipartFile file) throws IOException {
        long started = System.nanoTime();
        validate(file);
        ProcessingDeadline deadline = new ProcessingDeadline(Duration.ofSeconds(properties.getTimeoutSeconds()));

        byte[] bytes = file.getBytes();
        deadline.check();
        String fileName = file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename();
        String mediaType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        DocumentExtractor extractor = selectExtractor(fileName, mediaType);
        ExtractedDocument extracted = extractor.extract(fileName, mediaType, bytes, deadline);
        String markdown = markdownRenderer.render(extracted);

        // Optional semantic normalization/understanding. Current implementation is a no-op.
        markdown = llmClient.normalizeMarkdown(markdown, deadline);
        deadline.check();

        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        return new ProcessingResult(fileName, elapsedMs, markdown);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Maximum supported file size is 20 MB");
    }

    private DocumentExtractor selectExtractor(String fileName, String mediaType) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf") || mediaType.equalsIgnoreCase("application/pdf")) return pdfExtractor;
        if (lower.endsWith(".docx") || mediaType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) return docxExtractor;
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || mediaType.startsWith("image/")) return imageExtractor;
        throw new UnsupportedDocumentException("Supported formats: PDF, DOCX, PNG, JPEG");
    }
}
```

## `src/main/java/com/example/docproc/service/MarkdownRenderer.java`

```java
package com.example.docproc.service;

import com.example.docproc.model.*;
import org.springframework.stereotype.Component;

@Component
public class MarkdownRenderer {
    public String render(ExtractedDocument document) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(escapeHeading(document.fileName())).append("\n\n");

        boolean showPages = document.pages().size() > 1;
        for (PageContent page : document.pages()) {
            if (showPages) md.append("## Page ").append(page.pageNumber()).append("\n\n");
            for (DocumentBlock block : page.blocks()) {
                if (block.content() == null || block.content().isBlank()) continue;
                switch (block.type()) {
                    case HEADING -> md.append("## ").append(escapeHeading(block.content())).append("\n\n");
                    case TABLE -> md.append(block.content()).append("\n\n");
                    case IMAGE_TEXT -> md.append("### Extracted image text\n\n").append(block.content()).append("\n\n");
                    default -> md.append(block.content()).append("\n\n");
                }
            }
        }
        return md.toString().stripTrailing() + "\n";
    }

    private String escapeHeading(String value) {
        return value.replace("\n", " ").replace("\r", " ").strip();
    }
}
```

## `src/main/java/com/example/docproc/service/ProcessingDeadline.java`

```java
package com.example.docproc.service;

import java.time.Duration;
import java.time.Instant;

public final class ProcessingDeadline {
    private final Instant deadline;

    public ProcessingDeadline(Duration timeout) {
        this.deadline = Instant.now().plus(timeout);
    }

    public void check() {
        if (Instant.now().isAfter(deadline)) {
            throw new ProcessingTimeoutException("Document processing deadline exceeded");
        }
    }

    public Duration remaining() {
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
```

## `src/main/java/com/example/docproc/service/ProcessingTimeoutException.java`

```java
package com.example.docproc.service;

public class ProcessingTimeoutException extends RuntimeException {
    public ProcessingTimeoutException(String message) { super(message); }
}
```

## `src/main/java/com/example/docproc/service/UnsupportedDocumentException.java`

```java
package com.example.docproc.service;

public class UnsupportedDocumentException extends RuntimeException {
    public UnsupportedDocumentException(String message) { super(message); }
}
```

## `src/main/java/com/example/docproc/service/external/LlmClient.java`

```java
package com.example.docproc.service.external;

import com.example.docproc.service.ProcessingDeadline;
import org.springframework.stereotype.Component;

@Component
public class LlmClient {
    public String normalizeMarkdown(String markdown, ProcessingDeadline deadline) {
        deadline.check();
        // TODO: To be implemented - call the external GPT LLM endpoint.
        // Suggested use: semantic cleanup/table repair only. Do not make this call mandatory
        // for the fast path unless the business requirement requires LLM understanding.
        return markdown;
    }
}
```

## `src/main/java/com/example/docproc/service/external/OcrClient.java`

```java
package com.example.docproc.service.external;

import com.example.docproc.service.ProcessingDeadline;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Component
public class OcrClient {
    public String extractText(BufferedImage image, ProcessingDeadline deadline) {
        deadline.check();
        // TODO: To be implemented - call the external PaddleOCR endpoint.
        // Keep the HTTP timeout below deadline.remaining() and return OCR text.
        return "[OCR_TO_BE_IMPLEMENTED]";
    }
}
```

## `src/main/java/com/example/docproc/service/extractor/DocumentExtractor.java`

```java
package com.example.docproc.service.extractor;

import com.example.docproc.model.ExtractedDocument;
import com.example.docproc.service.ProcessingDeadline;

import java.io.IOException;

public interface DocumentExtractor {
    ExtractedDocument extract(String fileName, String mediaType, byte[] bytes, ProcessingDeadline deadline) throws IOException;
}
```

## `src/main/java/com/example/docproc/service/extractor/DocxDocumentExtractor.java`

```java
package com.example.docproc.service.extractor;

import com.example.docproc.model.*;
import com.example.docproc.service.ProcessingDeadline;
import com.example.docproc.service.external.OcrClient;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocxDocumentExtractor implements DocumentExtractor {
    private final OcrClient ocrClient;

    public DocxDocumentExtractor(OcrClient ocrClient) {
        this.ocrClient = ocrClient;
    }

    @Override
    public ExtractedDocument extract(String fileName, String mediaType, byte[] bytes, ProcessingDeadline deadline) throws IOException {
        List<DocumentBlock> blocks = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (IBodyElement element : document.getBodyElements()) {
                deadline.check();
                if (element instanceof XWPFParagraph paragraph) {
                    addParagraph(blocks, paragraph);
                } else if (element instanceof XWPFTable table) {
                    blocks.add(new DocumentBlock(BlockType.TABLE, tableToMarkdown(table)));
                }
            }

            // Embedded images may contain screenshots/scans. OCR is intentionally isolated here.
            for (XWPFPictureData picture : document.getAllPictures()) {
                deadline.check();
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(picture.getData()));
                if (image != null) {
                    String ocr = ocrClient.extractText(image, deadline);
                    if (ocr != null && !ocr.isBlank()) {
                        blocks.add(new DocumentBlock(BlockType.IMAGE_TEXT, ocr.strip()));
                    }
                }
            }
        }
        return new ExtractedDocument(fileName, mediaType,
                List.of(new PageContent(1, List.copyOf(blocks))));
    }

    private void addParagraph(List<DocumentBlock> blocks, XWPFParagraph paragraph) {
        String text = paragraph.getText();
        if (text == null || text.isBlank()) return;
        String style = paragraph.getStyle();
        BlockType type = style != null && style.toLowerCase().startsWith("heading")
                ? BlockType.HEADING : BlockType.PARAGRAPH;
        blocks.add(new DocumentBlock(type, text.strip()));
    }

    private String tableToMarkdown(XWPFTable table) {
        List<List<String>> rows = table.getRows().stream()
                .map(row -> row.getTableCells().stream()
                        .map(cell -> sanitize(cell.getText()))
                        .toList())
                .toList();
        if (rows.isEmpty()) return "";

        int columns = rows.stream().mapToInt(List::size).max().orElse(1);
        StringBuilder md = new StringBuilder();
        appendRow(md, rows.getFirst(), columns);
        md.append('|');
        for (int i = 0; i < columns; i++) md.append(" --- |");
        md.append('\n');
        for (int i = 1; i < rows.size(); i++) appendRow(md, rows.get(i), columns);
        return md.toString().stripTrailing();
    }

    private void appendRow(StringBuilder md, List<String> row, int columns) {
        md.append('|');
        for (int i = 0; i < columns; i++) {
            String value = i < row.size() ? row.get(i) : "";
            md.append(' ').append(value).append(" |");
        }
        md.append('\n');
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("|", "\\|").replaceAll("\\s+", " ").strip();
    }
}
```

## `src/main/java/com/example/docproc/service/extractor/ImageDocumentExtractor.java`

```java
package com.example.docproc.service.extractor;

import com.example.docproc.model.*;
import com.example.docproc.service.ProcessingDeadline;
import com.example.docproc.service.external.OcrClient;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ImageDocumentExtractor implements DocumentExtractor {
    private final OcrClient ocrClient;

    public ImageDocumentExtractor(OcrClient ocrClient) {
        this.ocrClient = ocrClient;
    }

    @Override
    public ExtractedDocument extract(String fileName, String mediaType, byte[] bytes, ProcessingDeadline deadline) throws IOException {
        deadline.check();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IOException("Unable to decode image");
        String ocrText = ocrClient.extractText(image, deadline);
        DocumentBlock block = new DocumentBlock(BlockType.IMAGE_TEXT,
                ocrText == null ? "" : ocrText.strip());
        return new ExtractedDocument(fileName, mediaType,
                List.of(new PageContent(1, List.of(block))));
    }
}
```

## `src/main/java/com/example/docproc/service/extractor/PdfDocumentExtractor.java`

```java
package com.example.docproc.service.extractor;

import com.example.docproc.config.DocumentProcessingProperties;
import com.example.docproc.model.*;
import com.example.docproc.service.ProcessingDeadline;
import com.example.docproc.service.external.OcrClient;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentExtractor implements DocumentExtractor {
    private final OcrClient ocrClient;
    private final DocumentProcessingProperties properties;

    public PdfDocumentExtractor(OcrClient ocrClient, DocumentProcessingProperties properties) {
        this.ocrClient = ocrClient;
        this.properties = properties;
    }

    @Override
    public ExtractedDocument extract(String fileName, String mediaType, byte[] bytes, ProcessingDeadline deadline) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(pdf);
            List<PageContent> pages = new ArrayList<>(pdf.getNumberOfPages());

            for (int pageIndex = 0; pageIndex < pdf.getNumberOfPages(); pageIndex++) {
                deadline.check();
                int pageNo = pageIndex + 1;
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);
                String nativeText = clean(stripper.getText(pdf));

                List<DocumentBlock> blocks = new ArrayList<>();
                if (nativeText.length() >= properties.getPdfNativeTextThreshold()) {
                    blocks.add(new DocumentBlock(BlockType.PAGE_TEXT, nativeText));
                } else {
                    BufferedImage image = renderer.renderImageWithDPI(
                            pageIndex, properties.getPdfRenderDpi(), ImageType.RGB);
                    String ocrText = clean(ocrClient.extractText(image, deadline));
                    blocks.add(new DocumentBlock(BlockType.IMAGE_TEXT, ocrText));
                }
                pages.add(new PageContent(pageNo, List.copyOf(blocks)));
            }
            return new ExtractedDocument(fileName, mediaType, List.copyOf(pages));
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
```

## `src/main/resources/application.yml`

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB
server:
  tomcat:
    max-swallow-size: 20MB

docproc:
  timeout-seconds: 28
  pdf-native-text-threshold: 40
  max-ocr-concurrency: 4
  pdf-render-dpi: 144
```

---

# Recommended Production Enhancements

1. Add MIME sniffing rather than trusting only filename extensions.
2. Reject password-protected or malformed documents early.
3. Add page-count and rendered-pixel limits for scanned PDFs.
4. Implement bounded parallel OCR using Java virtual threads or an executor with a semaphore.
5. Track the remaining request deadline and stop launching expensive OCR/LLM calls when the time budget is nearly exhausted.
6. Add metrics for upload time, native extraction time, OCR time, LLM time, number of OCR pages, and total processing latency.
7. Preserve page ordering when parallel OCR calls complete out of order.
8. Consider returning partial Markdown plus a processing-status flag when the SLA deadline is reached, depending on business requirements.
9. Keep raw extracted content and LLM-normalized content logically separate so hallucinated changes can be detected.
10. Add tests with text PDFs, scanned PDFs, mixed PDFs, DOCX tables, DOCX embedded images, PNG/JPEG screenshots, malformed files, and files close to 20 MB.
