package com.yhh.travelagent.chatmemory;

import com.yhh.travelagent.service.ChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Date 2025-07-11 22:02
 * @ClassName: MybatisPlusChatMemory
 * @Description: 基于MybatisPlusChatMemory 实现对话记忆
 */
@Component
@Slf4j
public class MybatisPlusChatMemory implements ChatMemory {
    private final ChatMemoryService chatMemoryService;

    public MybatisPlusChatMemory(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
        log.info("初始化Mybatis-Plus对话记忆");
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        chatMemoryService.addMessages(conversationId, messages);
    }


    @Override
    public List<Message> get(String conversationId, int lastN) {
        return chatMemoryService.getMessages(conversationId, lastN);
    }

    @Override
    public void clear(String conversationId) {
        chatMemoryService.clearMessages(conversationId);
    }
}
