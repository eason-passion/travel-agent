package com.yhh.travelagent.agent;

import com.yhh.travelagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * @Date 2025-07-13 15:01
 * @ClassName: HealthAssistant
 * @Description: 健康助手
 */
@Component
public class HealthAssistant extends ToolCallAgent{
    public HealthAssistant(ToolCallback[] availableTools, ChatModel dashscopeChatModel) {
        super(availableTools); //全部工具

        //基础配置
        this.setName("HealthAssistant");
        this.setMaxSteps(15);
        // 提示词设置
        this.setSystemPrompt(
                "You are HealthAssistant, a professional health assistant designed to provide health-related information and advice. " +
                        "You can answer questions about general health topics, wellness, disease prevention, healthy lifestyle choices, and basic medical knowledge. " +
                        "Always emphasize that you are not a replacement for professional medical advice, diagnosis, or treatment. " +
                        "For serious health concerns, always recommend consulting with qualified healthcare professionals. " +
                        "When providing information, prioritize evidence-based medical knowledge and reliable health sources."
        );

        this.setNextStepPrompt(
                "Based on the user's health-related questions or concerns, provide helpful, accurate, and scientifically-backed information. " +
                        "Use appropriate tools to retrieve relevant health information when needed. " +
                        "Present information in a clear, compassionate, and easy-to-understand manner. " +
                        "Always maintain a balance between being informative and acknowledging the limitations of digital health assistance. " +
                        "If you want to stop the interaction at any point, use the `terminate` tool/function call."
        );

        // 初始化对话客户端
        this.setChatClient(
                ChatClient.builder(dashscopeChatModel)
                        .defaultAdvisors(new MyLoggerAdvisor())
                        .build()
        );
    }
}

