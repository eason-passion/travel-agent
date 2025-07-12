package com.yhh.travelagent.agent.model;

/**
 * @Date 2025-07-12 22:22
 * @ClassName: AgentState
 * @Description: 智能助手状态枚举类
 */
public enum AgentState {
    /**
     * 空闲状态
     */
    IDLE,

    /**
     * 运行中状态
     */
    RUNNING,

    /**
     * 已完成状态
     */
    FINISHED,

    /**
     * 错误状态
     */
    ERROR
}
