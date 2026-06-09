package com.apartment.hub.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final SimpleVectorStore vectorStore;

    private static final String KB_DIR = "./uploads/kb";
    private static final String KB_FILE = KB_DIR + "/vector-store.json";
    private static final String[] DOC_PATHS = {
            "../合同.md", "../启动.md", "../支付.md",
            "../Redis集成总结.md", "../合同bug.md"
    };

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(800)
            .withMinChunkSizeChars(50)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();

    private boolean ready = false;

    @PostConstruct
    void init() {
        try {
            // Try loading persisted data first
            File file = new File(KB_FILE);
            if (file.exists()) {
                vectorStore.load(file);
                log.info("Knowledge base loaded from disk: {} chunks", file.length());
                ready = true;
                return;
            }

            // First run: ingest documents
            rebuildFromDocuments();
        } catch (Exception e) {
            log.error("Failed to initialize knowledge base", e);
        }
    }

    public void rebuildFromDocuments() {
        log.info("Rebuilding knowledge base from documents...");
        List<Document> allChunks = new ArrayList<>();

        for (String path : DOC_PATHS) {
            try {
                Path filePath = Paths.get(path);
                if (!Files.exists(filePath)) {
                    log.warn("Document not found: {}", path);
                    continue;
                }

                String content = Files.readString(filePath);
                String fileName = filePath.getFileName().toString();

                // Split by markdown sections (## ) first, then by tokens
                String[] sections = content.split("(?=^## )");
                for (String section : sections) {
                    if (section.isBlank()) continue;

                    String sectionTitle = extractTitle(section);
                    Document doc = Document.builder()
                            .text(section.trim())
                            .metadata(Map.of("source", fileName, "title", sectionTitle))
                            .build();

                    // Further split long sections
                    List<Document> chunks = splitter.split(doc);
                    for (Document chunk : chunks) {
                        chunk.getMetadata().put("source", fileName);
                        chunk.getMetadata().put("title", sectionTitle);
                    }
                    allChunks.addAll(chunks);
                }

                log.info("Ingested {} → {} chunks", fileName, allChunks.size());
            } catch (Exception e) {
                log.error("Failed to read document: {}", path, e);
            }
        }

        if (allChunks.isEmpty()) {
            log.warn("No documents ingested — knowledge base is empty");
            return;
        }

        // Store in vector DB
        vectorStore.add(allChunks);

        // Persist to disk
        try {
            File dir = new File(KB_DIR);
            dir.mkdirs();
            vectorStore.save(new File(KB_FILE));
            log.info("Knowledge base built and saved: {} chunks from {} documents",
                    allChunks.size(), DOC_PATHS.length);
            ready = true;
        } catch (Exception e) {
            log.error("Failed to persist knowledge base", e);
        }
    }

    /**
     * Search the knowledge base for relevant context.
     */
    public String searchContext(String query, int topK) {
        if (!ready) {
            return "";
        }

        try {
            List<Document> results = vectorStore.doSimilaritySearch(
                    SearchRequest.query(query).withTopK(topK));

            if (results.isEmpty()) {
                return "";
            }

            return results.stream()
                    .map(doc -> {
                        String source = (String) doc.getMetadata().get("source");
                        String title = (String) doc.getMetadata().get("title");
                        return "【来源：" + source + " - " + (title != null ? title : "文档") + "】\n"
                                + doc.getContent();
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

        } catch (Exception e) {
            log.error("Knowledge base search failed", e);
            return "";
        }
    }

    public boolean isReady() {
        return ready;
    }

    private String extractTitle(String section) {
        String line = section.lines().findFirst().orElse("");
        return line.replaceAll("^#+\\s*", "").trim();
    }
}
