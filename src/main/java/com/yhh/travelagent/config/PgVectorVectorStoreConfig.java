package com.yhh.travelagent.config;

import cn.hutool.core.collection.CollUtil;
import com.yhh.travelagent.rag.TravelAppDocumentLoader;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * @Date 2025-07-05 15:37
 * @ClassName: PgVectorVectorStoreConfig
 * @Description: 配置向量数据库
 */
// 为方便开发调试和部署，临时注释，如果需要使用 PgVector 存储知识库，取消注释即可
@Configuration
@Slf4j
public class PgVectorVectorStoreConfig {

    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Bean
    @Primary
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        // 创建PgVectorStore实例，配置向量存储的参数
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)                    // 设置向量的维度，可选，默认为模型维度或1536
                .distanceType(COSINE_DISTANCE)       // 设置计算向量间距离的方法，可选，默认为余弦距离
                .indexType(HNSW)                     // 设置索引类型，可选，默认为HNSW（高效近似最近邻搜索）
                .initializeSchema(true)              // 是否初始化数据库模式，可选，默认为false
                .schemaName("public")                // 设置数据库模式名称，可选，默认为"public"
                .vectorTableName("vector_store")     // 设置存储向量数据的表名，可选，默认为"vector_store"
                .maxDocumentBatchSize(10000)         // 设置文档批量插入的最大数量，可选，默认为10000
                .build();
        // 加载文档
        List<Document> documents = travelAppDocumentLoader.loadMarkdowns();
        // 关键：查询数据库中已存在的文档ID
        List<String> existingIds = new ArrayList<>();
        try {
            // 查询vector_store表中的所有文档ID（表名与配置一致）
            existingIds = jdbcTemplate.queryForList(
                    "SELECT id FROM public.vector_store",
                    String.class
            );
        } catch (Exception e) {
            // 首次启动时表可能刚创建，查询可能抛异常（忽略即可，视为无数据）
            log.warn("查询已有文档ID失败（可能是首次启动）：{}", e.getMessage());
        }
        // 过滤出不存在的新文档（只添加ID不在existingIds中的文档）
        List<String> finalExistingIds = existingIds;
        List<Document> newDocuments = documents.stream()
                .filter(doc -> !finalExistingIds.contains(doc.getId()))
                .collect(Collectors.toList());
        // 只添加新文档（避免重复）
        if (!newDocuments.isEmpty()) {
            log.info("发现{}个新文档，开始添加到向量库...", newDocuments.size());
            vectorStore.add(newDocuments);
        } else {
            log.info("所有文档已存在，无需重复添加");
        }
        vectorStore.add(documents);
        return vectorStore;
    }
}
