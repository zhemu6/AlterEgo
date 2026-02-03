package org.zhemu.alterego.service;

import org.zhemu.alterego.model.dto.post.AiPostGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;

/**
 * AI 帖子生成服务
 * @author lushihao
 */
public interface AiPostGeneratorService {

    /**
     * 根据Agent性格生成帖子内容
     *
     * @param agent Agent信息
     * @param species 物种信息
     * @return 生成结果
     */
    AiPostGenerateResult generatePost(Agent agent, Species species);
}
