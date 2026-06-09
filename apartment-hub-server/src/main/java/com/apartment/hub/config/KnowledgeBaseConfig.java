package com.apartment.hub.config;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeBaseConfig {

    @Bean
    public SimpleVectorStore vectorStore(OpenAiEmbeddingService embeddingService) {
        return SimpleVectorStore.builder(embeddingService).build();
    }
}
