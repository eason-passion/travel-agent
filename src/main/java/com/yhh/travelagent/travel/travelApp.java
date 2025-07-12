package com.yhh.travelagent.travel;

import com.yhh.travelagent.advisor.MyLoggerAdvisor;
import com.yhh.travelagent.advisor.ProhibitedWordAdvisor;
import com.yhh.travelagent.advisor.ReReadingAdvisor;
import com.yhh.travelagent.chatmemory.FileBasedChatMemory;
import com.yhh.travelagent.chatmemory.MySQLChatMemory;
import com.yhh.travelagent.chatmemory.MybatisPlusChatMemory;
import com.yhh.travelagent.rag.QueryRewriter;
import com.yhh.travelagent.rag.TravelAppRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @Date 2025-07-11 19:12
 * @ClassName: travelApp
 * @Description:
 */
@Component
@Slf4j
public class travelApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "请你作为【旅游规划大师】，以专业且亲和的旅游顾问身份，模拟真实的旅游咨询场景与用户互动。\n" +
            "在沟通中，要通过一系列引导性问题深入了解用户的具体需求 —— 比如出行时间（假期时长、季节偏好）、同行人员（是否有老人 / 小孩、情侣 / 朋友 / 独自出行）、预算范围（经济型 / 舒适型 / 豪华型）、旅行偏好（喜欢自然风光还是城市人文、偏爱小众秘境还是热门景点、是否热衷美食探索 / 户外活动 / 购物娱乐）、旅行节奏（轻松休闲型还是紧凑打卡型、希望深度体验还是浅尝辄止），以及是否有特殊需求（比如饮食忌口、身体状况、对交通 / 住宿的特殊要求、是否想融入当地生活体验等）。\n" +
            "基于这些细节，为用户量身定制全面且实用的旅游规划，包括目的地推荐（附具体推荐理由）、每日行程安排（细化到交通方式、景点玩法、餐饮建议）、住宿选择（结合预算和需求推荐合适类型及区域）、出行注意事项（天气、穿搭、当地习俗等）。\n" +
            "始终以用户需求为核心，通过持续提问精准捕捉潜在诉求（比如是否有必去清单、是否想避开人流高峰等），确保给出的规划方案贴合用户期待，帮用户避开旅行中的常见坑，让每一段行程都更符合其个性化期待，拥有舒适且难忘的旅行体验。";

    public travelApp(ChatModel dashscopeChatModel, MybatisPlusChatMemory chatMemory,MySQLChatMemory jdbcmysqlchatMemory) {
        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
//        ChatMemory chatMemory = new InMemoryChatMemory();
//        ChatMemory chatMemory = new MySQLChatMemory(dataSource);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志拦截
                        new MyLoggerAdvisor(),
                        // 违禁词检测 - 从文件读取违禁词
                        new ProhibitedWordAdvisor()
//                        // 自定义重读拦截
//                        new ReReadingAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient.prompt().user(message).advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)).call().chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    /**
     * 旅游报告
    */
    public record TravelReport(String title, List<String> suggestions) {
    }

    /**
     * 旅游报告功能
     * @param message
     * @param chatId
     * @return
     */
    public TravelReport doChatWithReport(String message, String chatId) {
        TravelReport travelReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成旅游建议，标题为{用户名}的旅游计划报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(TravelReport.class);
        log.info("travelReport: {}", travelReport);
        return travelReport;
    }
    /**
     * 基于本地知识库 rag
     */
    @Resource
    @Qualifier("travelAppVectorStore")
    private VectorStore travelAppVectorStore;

    @Resource
    private VectorStore pgVectorVectorStore;
    @Resource
    private QueryRewriter queryRewriter;
    @Resource
    private Advisor travelAppRagCloudAdvisor;
    public String doChatWithRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答
                .advisors(new QuestionAnswerAdvisor(travelAppVectorStore))
                // 应用增强检索服务（云知识库服务）
//                .advisors(travelAppRagCloudAdvisor)
                // rag应用 （基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
//                 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        TravelAppRagCustomAdvisorFactory.createTravelAppRagCustomAdvisor(
//                                travelAppVectorStore, "风景优美"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
//    /**
//     * 云知识库 rag
//     */
//    @Resource
//    private Advisor travelAppRagCloudAdvisor;
//    public String doChatWithRag2(String message, String chatId) {
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
//                // 开启日志，便于观察效果
//                .advisors(new MyLoggerAdvisor())
//                // 应用增强检索服务（云知识库服务）
//                .advisors(travelAppRagCloudAdvisor)
//                .call()
//                .chatResponse();
//        String content = chatResponse.getResult().getOutput().getText();
//        log.info("content: {}", content);
//        return content;
//    }


    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 旅游规划报告功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }




    /**
     * mcp
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }



}
