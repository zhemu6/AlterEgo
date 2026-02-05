package org.zhemu.alterego.service.base;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;

/**
 * AI 文本生成服务抽象基类
 * 使用模板方法模式，定义 AI 调用的骨架流程，子类只需实现具体步骤
 * 
 * @param <TRequest> 请求参数类型（可为 Void）
 * @param <TResult> 返回结果类型
 * @author lushihao
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAiTextGenerator<TRequest, TResult> {
    
    protected final Model dashScopeModel;
    protected final Session mysqlSession;
    
    /**
     * 模板方法：定义 AI 生成的完整流程
     * final 防止子类覆盖，确保流程一致性
     * 
     * @param agent Agent 实体
     * @param species 物种实体
     * @param request 额外请求参数（可为 null）
     * @return 生成结果
     */
    public final TResult generate(Agent agent, Species species, TRequest request) {
        log.info("AI 生成开始: agent={}, type={}", agent.getAgentName(), getGeneratorType());
        
        try {
            String sessionId = buildSessionId(agent, request);
            String prompt = buildPrompt(agent, species, request);
            String rawResponse = callAi(sessionId, prompt);
            TResult result = parseResponse(rawResponse);
            
            if (!validateResult(result)) {
                log.warn("AI 结果验证失败，使用降级策略");
                result = getFallbackResult();
            }
            
            log.info("AI 生成成功: agent={}, type={}", agent.getAgentName(), getGeneratorType());
            return result;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 生成失败: agent={}, type={}", agent.getAgentName(), getGeneratorType(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 子类必须实现：构建 Prompt
     * 
     * @param agent Agent 实体
     * @param species 物种实体
     * @param request 额外请求参数
     * @return Prompt 字符串
     */
    protected abstract String buildPrompt(Agent agent, Species species, TRequest request);
    
    /**
     * 子类必须实现：解析 AI 原始响应
     * 
     * @param rawResponse AI 返回的原始字符串
     * @return 解析后的结果对象
     */
    protected abstract TResult parseResponse(String rawResponse);
    
    /**
     * 子类必须实现：返回结果类型的 Class
     * 用于 AgentScope 的结构化数据解析
     * 
     * @return 结果类型的 Class 对象
     */
    protected abstract Class<TResult> getResultClass();
    
    /**
     * 子类必须实现：提供降级结果
     * 当 AI 调用失败或返回无效数据时使用
     * 
     * @return 降级的默认结果
     */
    protected abstract TResult getFallbackResult();
    
    /**
     * 子类必须实现：返回 Session 前缀
     * 如：Constants.AGENT_POST_SESSION_PREFIX
     * 
     * @return Session ID 前缀
     */
    protected abstract String getSessionPrefix();
    
    /**
     * 子类必须实现：返回生成器类型（用于日志）
     * 如："post", "comment", "pk"
     * 
     * @return 生成器类型标识
     */
    protected abstract String getGeneratorType();
    
    /**
     * 钩子方法：构建 Session ID
     * 默认实现：prefix + agentId
     * 子类可覆盖（如 Comment 需要 agentId + postId）
     * 
     * @param agent Agent 实体
     * @param request 额外请求参数
     * @return Session ID
     */
    protected String buildSessionId(Agent agent, TRequest request) {
        return getSessionPrefix() + agent.getId();
    }
    
    /**
     * 钩子方法：验证结果有效性
     * 默认返回 true，子类可覆盖
     * 
     * @param result 解析后的结果
     * @return 是否有效
     */
    protected boolean validateResult(TResult result) {
        return result != null;
    }
    
    /**
     * 钩子方法：获取 Agent 名称（用于 ReActAgent）
     * 默认返回类名，子类可覆盖
     * 
     * @return Agent 名称
     */
    protected String getAgentName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 钩子方法：获取系统 Prompt
     * 默认提供通用 Prompt，子类可覆盖
     * 
     * @return 系统 Prompt
     */
    protected String getSystemPrompt() {
        return "你是一个擅长角色扮演的 AI，能够完美代入各种角色的性格和说话方式。你有长期的记忆，记得自己之前说过什么。";
    }
    
    /**
     * 钩子方法：获取 AutoContext 配置
     * 默认配置：tokenRatio=0.4, lastKeep=10
     * 子类可覆盖
     * 
     * @return AutoContext 配置
     */
    protected AutoContextConfig getAutoContextConfig() {
        return AutoContextConfig.builder()
            .tokenRatio(0.4)
            .lastKeep(10)
            .build();
    }
    
    /**
     * 钩子方法：获取 Agent 最大迭代次数
     * 默认 3 次，子类可覆盖
     * 
     * @return 最大迭代次数
     */
    protected int getMaxIters() {
        return 3;
    }
    
    /**
     * 核心私有方法：调用 AI（封装 AgentScope 完整流程）
     * 
     * @param sessionId 会话 ID
     * @param prompt 用户 Prompt
     * @return AI 原始响应字符串
     */
    private String callAi(String sessionId, String prompt) {
        try {
            AutoContextMemory memory = new AutoContextMemory(
                getAutoContextConfig(), 
                dashScopeModel
            );
            
            ReActAgent aiAgent = ReActAgent.builder()
                .name(getAgentName())
                .sysPrompt(getSystemPrompt())
                .model(dashScopeModel)
                .memory(memory)
                .maxIters(getMaxIters())
                .build();
            
            try {
                aiAgent.loadIfExists(mysqlSession, sessionId);
                log.debug("加载会话历史成功: sessionId={}", sessionId);
            } catch (Exception e) {
                log.debug("会话历史不存在或加载失败（首次调用？）: sessionId={}", sessionId);
            }
            
            Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(prompt).build())
                .build();
            
            Msg response = aiAgent.call(userMsg, getResultClass()).block();
            
            try {
                aiAgent.saveTo(mysqlSession, sessionId);
                log.debug("保存会话历史成功: sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("保存会话历史失败: sessionId={}", sessionId, e);
            }
            
            if (response == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成无响应");
            }
            
            Object structuredData = response.getStructuredData(getResultClass());
            if (structuredData == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回数据解析失败");
            }
            
            return structuredData.toString();
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 调用失败: " + e.getMessage());
        }
    }
}
