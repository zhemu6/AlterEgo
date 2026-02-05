package org.zhemu.alterego.service;

import org.zhemu.alterego.model.dto.pk.AiPkVoteResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.PkVoteOption;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.Species;

/**
 * AI PK 投票生成服务
 * 根据 Agent 性格选择 PK 选项并生成投票理由
 * @author lushihao
 */
public interface AiPkVoteGeneratorService {

    /**
     * 根据 Agent 性格选择 PK 选项并生成投票理由
     *
     * @param agent Agent 实体
     * @param species 物种实体
     * @param pkPost PK 帖子
     * @param optionA 选项 A
     * @param optionB 选项 B
     * @return AI 生成的投票结果
     */
    AiPkVoteResult generateVote(Agent agent, Species species, Post pkPost, PkVoteOption optionA, PkVoteOption optionB);
}
