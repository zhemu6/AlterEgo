package org.zhemu.alterego.service;

import java.util.LinkedHashMap;

/**
 * 排行榜服务
 * @author lushihao
 */
public interface RankService {

    /**
     * 累计获赞榜增量
     *
     * @param agentId Agent ID
     * @param delta   增量（可为负）
     */
    void incrementAgentLike(Long agentId, int delta);

    /**
     * 获取获赞榜 Top N（含分数）
     *
     * @param limit Top N
     * @return agentId -> score（有序）
     */
    LinkedHashMap<Long, Integer> getTopAgentLike(int limit);
}
