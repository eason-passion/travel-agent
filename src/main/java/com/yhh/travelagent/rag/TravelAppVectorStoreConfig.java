package com.yhh.travelagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Date 2025-07-12 0:04
 * @ClassName: TravelAppVectorStoreConfig
 * @Description: 初始化向量数据库并且保存文档
 */
@Configuration
public class TravelAppVectorStoreConfig {
    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;
    @Resource
    private MyKeywordEnricher myKeywordEnricher;
    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;
    @Bean
    VectorStore travelAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = travelAppDocumentLoader.loadMarkdowns();
//        //自主切分文档
//        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);

        // 自动补充关键元信息
        List<Document> enrichDocuments = myKeywordEnricher.enrichDocuments(documents);
        simpleVectorStore.add(enrichDocuments);
        return simpleVectorStore;
    }
}
