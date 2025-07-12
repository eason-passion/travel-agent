package com.yhh.travelagent.agent;

import cn.hutool.core.util.StrUtil;
import com.yhh.travelagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态
    private AgentState state = AgentState.IDLE;

    // 执行控制
    private int maxSteps = 10;
    private int currentStep = 0;

    // 循环检测
    private int duplicateThreshold = 2;

    // LLM
    private ChatClient chatClient;

    // Memory（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtils.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 步骤循环
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                currentStep = i + 1;
                log.info("执行步骤 {}/{}", currentStep, maxSteps);
                String stepResult = step();
                results.add("Step " + currentStep + ": " + stepResult);

                // 检查是否陷入循环
                if (isStuck()) {
                    handleStuckState();
                    results.add("检测到可能的循环，已添加额外提示以避免重复");
                }
            }

            // 检查终止条件
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("终止: 达到最大步骤 (" + maxSteps + ")");
            }

            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("执行错误", e);
            return "执行错误: " + e.getMessage();
        } finally {
            cleanup();
        }
    }

    /**
     * 执行单个步骤
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }

    /**
     * 运行代理(流式输出)
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        CompletableFuture.runAsync(() -> {
            try {
                // 校验
                if (state != AgentState.IDLE || StrUtil.isBlank(userPrompt)) {
                    String error = "错误: " + (state != AgentState.IDLE ? "无法从状态运行: " + state : "空提示词");
                    emitter.send(error);
                    emitter.complete();
                    return;
                }

                // 执行
                state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));

                // 步骤循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    currentStep = i + 1;
                    log.info("执行步骤 {}/{}", currentStep, maxSteps);
                    String stepResult = step();
                    String result = "Step " + currentStep + ": " + stepResult;
                    emitter.send(result);

                    // 检查是否陷入循环
                    if (isStuck()) {
                        handleStuckState();
                        emitter.send("检测到可能的循环，已添加额外提示以避免重复");
                    }
                }

                // 检查终止条件
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                }

                emitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("执行错误", e);
                try {
                    emitter.send("执行错误: " + e.getMessage());
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                cleanup();
            }
        });

        // 事件处理
        emitter.onTimeout(() -> {
            state = AgentState.ERROR;
            cleanup();
            log.warn("SSE连接超时");
        });

        emitter.onCompletion(() -> {
            if (state == AgentState.RUNNING) {
                state = AgentState.FINISHED;
            }
            cleanup();
            log.info("SSE连接完成");
        });

        return emitter;
    }




    /**
     * 处理陷入循环的状态
     */
    protected void handleStuckState() {
        String stuckPrompt = "观察到重复响应。请考虑新的策略，避免重复已尝试过的无效路径。";
        this.nextStepPrompt = stuckPrompt + "\n" + (this.nextStepPrompt != null ? this.nextStepPrompt : "");
        log.warn("检测到智能体陷入循环状态。添加额外提示: {}", stuckPrompt);
    }

    /**
     * 检查代理是否陷入循环
     *
     * @return 是否陷入循环
     */
    protected boolean isStuck() {
        if (messageList.size() < 2) {
            return false;
        }

        // 获取最后一条助手消息
        AssistantMessage lastAssistantMessage = null;
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i) instanceof AssistantMessage) {
                lastAssistantMessage = (AssistantMessage) messageList.get(i);
                break;
            }
        }

        if (lastAssistantMessage == null || lastAssistantMessage.getText() == null
                || lastAssistantMessage.getText().isEmpty()) {
            return false;
        }

        // 计算重复内容出现次数
        int duplicateCount = 0;
        String lastContent = lastAssistantMessage.getText();

        for (int i = messageList.size() - 2; i >= 0; i--) {
            Message msg = messageList.get(i);
            if (msg instanceof AssistantMessage) {
                AssistantMessage assistantMsg = (AssistantMessage) msg;
                if (lastContent.equals(assistantMsg.getText())) {
                    duplicateCount++;

                    if (duplicateCount >= this.duplicateThreshold) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}


