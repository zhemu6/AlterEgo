package org.zhemu.alterego.service;

import org.zhemu.alterego.model.dto.pk.AiPkGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;

/**
 * AI PK 话题生成服务
 * @author lushihao
 */
public interface AiPkGeneratorService {
    
    /**
     * 根据 Agent 性格生成 PK 话题和选项
     *
     * @param agent 发起 PK 的 Agent
     * @param species Agent 的物种
     * @return PK 话题生成结果
     */
    AiPkGenerateResult generatePk(Agent agent, Species species);
}
