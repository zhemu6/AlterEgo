package org.zhemu.alterego.service.impl;

import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.model.dto.pk.AiPkGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiPkGeneratorService;
import org.zhemu.alterego.service.base.AbstractAiTextGenerator;

import java.util.Collections;

import static org.zhemu.alterego.constant.Constants.AGENT_PK_SESSION_PREFIX;

/**
 * AI PK 话题生成服务实现
 * 重构后：继承 AbstractAiTextGenerator
 * @author lushihao
 */
@Service
@Slf4j
public class AiPkGeneratorServiceImpl 
    extends AbstractAiTextGenerator<Void, AiPkGenerateResult> 
    implements AiPkGeneratorService {

    public AiPkGeneratorServiceImpl(Model dashScopeModel, Session mysqlSession) {
        super(dashScopeModel, mysqlSession);
    }

    @Override
    protected String getSessionPrefix() {
        return AGENT_PK_SESSION_PREFIX;
    }

    @Override
    protected String getGeneratorType() {
        return "pk";
    }

    @Override
    protected String buildPrompt(Agent agent, Species species, Void unused) {
        return String.format("""
            现在你想发起一个有趣的 PK 投票话题。
            
            你的身份：
            - 物种：%s
            - 名字：%s
            - 性格：%s
            
            要求：
            1. 话题要有趣、有争议性
            2. 两个选项要对立但都有道理
            3. 选项文字简短（10字以内），可用emoji
            4. 请回顾你之前的 PK 话题（如果有），避免重复相同主题
            
            输出 JSON 格式：
            {
              "topic": "话题标题（30字以内）",
              "description": "话题描述（100字以内）",
              "optionA": "选项A文字",
              "optionB": "选项B文字",
              "tags": ["标签1", "标签2"]
            }
            """, species.getName(), agent.getAgentName(), agent.getPersonality());
    }

    @Override
    protected Class<AiPkGenerateResult> getResultClass() {
        return AiPkGenerateResult.class;
    }

    @Override
    protected AiPkGenerateResult getFallbackResult() {
        AiPkGenerateResult result = new AiPkGenerateResult();
        result.topic = "猫派 vs 狗派";
        result.description = "你更喜欢猫还是狗？";
        result.optionA = "猫咪派🐱";
        result.optionB = "狗狗派🐶";
        result.tags = Collections.singletonList("宠物");
        return result;
    }

    @Override
    protected boolean validateResult(AiPkGenerateResult result) {
        return result != null && result.topic != null && result.description != null;
    }

    @Override
    protected String getAgentName() {
        return "PkGenerator";
    }

    @Override
    protected String getSystemPrompt() {
        return "你是一个擅长角色扮演的 AI，能够完美代入各种角色的性格和说话方式。你有长期的记忆，记得自己之前发起过什么话题。";
    }

    @Override
    public AiPkGenerateResult generatePk(Agent agent, Species species) {
        return generate(agent, species, null);
    }
}
