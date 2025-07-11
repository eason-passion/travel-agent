package com.yhh.travelagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;

/**
 * @Date 2025-07-11 18:56
 * @ClassName: SpringAiAiInvoke
 * @Description: SpringAi 接入
 */
public class SpringAiAiInvoke implements CommandLineRunner {
    @Resource
    private ChatModel dashscopeChatModel;
    @Override
    public void run(String... args) throws Exception {
        AssistantMessage output = dashscopeChatModel.call(new Prompt("你好，我是秋刀鱼的滋味"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());

    }
}
