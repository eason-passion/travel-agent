package com.yhh.travelagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * @Date 2025-07-12 22:44
 * @ClassName: TerminateTool
 * @Description: 停止工具
 }
 */
public class TerminateTool {
    @Tool(description = """  
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.  
            "When you have finished all the tasks, call this tool to end the work.  
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
