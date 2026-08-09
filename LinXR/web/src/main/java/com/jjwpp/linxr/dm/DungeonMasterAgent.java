package com.jjwpp.linxr.dm;

import dev.langchain4j.service.TokenStream;

/**
 * AI 地下城主 Agent 接口
 * <p>
 * 基于 LangChain4j AI Services 构建，通过 Function Calling 实现
 * AI 自主调用游戏工具（掷骰、攻击、施法、掉落等），
 * 形成"感知-决策-执行"的 ReAct 闭环。
 * <p>
 * 核心特征：
 * - AI 自主决定调用哪个工具（不再由 Java 代码解析玩家意图）
 * - 工具执行结果返回给 AI，AI 根据结果生成叙事
 * - AI 可以连续调用多个工具（如：攻击 → 检查胜利 → 生成掉落）
 * - 流式输出通过 TokenStream 回调推送至前端
 */
public interface DungeonMasterAgent {

    /**
     * 处理玩家行动，返回流式 Token 流
     * <p>
     * AI 会根据玩家输入和当前游戏状态，自主选择调用合适的工具，
     * 然后基于工具返回的结果生成叙事文本。
     *
     * @param playerAction 玩家的行动描述
     * @return TokenStream 流式响应
     */
    TokenStream chat(String playerAction);
}
