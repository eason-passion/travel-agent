package com.yhh.travelagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Date 2025-07-12 19:47
 * @ClassName: ToolRegistration
 * @Description: 工具注册
 */
@Configuration
public class ToolRegistration {
    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Autowired
    private DateTimeTool dateTimeTool;

    private TerminateTool terminateTool;

    /**
     * 注册所有AI工具
     */
    @Bean
    public ToolCallback[] allTools() {
        // 实例化所有工具
        return ToolCallbacks.from(
                new FileOperationTool(),
                new WebSearchTool(searchApiKey),
                new WebScrapingTool(),
                new ResourceDownloadTool(),
                new TerminalOperationTool(),
                new PDFGenerationTool(),
//                new ImageSearchTool(),
                new DateTimeTool(),
                new TerminateTool()
        );
    }
}
