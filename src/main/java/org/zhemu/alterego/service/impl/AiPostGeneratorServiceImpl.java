package org.zhemu.alterego.service.impl;

import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.model.dto.post.AiPostGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiPostGeneratorService;
import org.zhemu.alterego.service.base.AbstractAiTextGenerator;

import java.util.Collections;

import static org.zhemu.alterego.constant.Constants.AGENT_POST_SESSION_PREFIX;

/**
 * AI 帖子生成服务实现
 * 重构后：继承 AbstractAiTextGenerator，只需实现特定逻辑
 * @author lushihao
 */
@Service
@Slf4j
public class AiPostGeneratorServiceImpl 
    extends AbstractAiTextGenerator<Void, AiPostGenerateResult> 
    implements AiPostGeneratorService {
    
    public AiPostGeneratorServiceImpl(Model dashScopeModel, Session mysqlSession) {
        super(dashScopeModel, mysqlSession);
    }
    
    @Override
    protected String getSessionPrefix() {
        return AGENT_POST_SESSION_PREFIX;
    }
    
    @Override
    protected String getGeneratorType() {
        return "post";
    }
    
    @Override
    protected String buildPrompt(Agent agent, Species species, Void unused) {
        return String.format("""
            现在你想发一条社交动态。
            
            你的身份：
            - 物种：%s
            - 名字：%s
            - 性格：%s
            
            要求：
            1. 标题要吸引人，符合你的性格。
            2. 内容要短小精悍（100字以内），像发朋友圈一样。
            3. 生成 3-5 个有趣的标签（Hashtag）。
            4. 请回顾你之前的发帖记录（如果有），避免重复相同的话题，保持内容的新鲜感。
            
            输出 JSON 格式：
            {
              "title": "标题",
              "content": "内容",
              "tags": ["标签1", "标签2"]
            }
            """, species.getName(), agent.getAgentName(), agent.getPersonality());
    }
    
    @Override
    protected Class<AiPostGenerateResult> getResultClass() {
        return AiPostGenerateResult.class;
    }
    
    @Override
    protected AiPostGenerateResult getFallbackResult() {
        AiPostGenerateResult result = new AiPostGenerateResult();
        result.title = "今天天气真好";
        result.content = "出来晒晒太阳，心情美美哒~";
        result.tags = Collections.singletonList("日常");
        return result;
    }
    
    @Override
    protected boolean validateResult(AiPostGenerateResult result) {
        return result != null && result.title != null && result.content != null;
    }
    
    @Override
    protected String getAgentName() {
        return "PostGenerator";
    }
    
    @Override
    public AiPostGenerateResult generatePost(Agent agent, Species species) {
        return generate(agent, species, null);
    }
}
