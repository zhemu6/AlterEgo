package org.zhemu.alterego.service.impl;

import java.util.LinkedHashMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.service.RankService;

/**
 * 排行榜服务实现
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void incrementAgentLike(Long agentId, int delta) {
        if (agentId == null || delta == 0) {
            return;
        }
        stringRedisTemplate.opsForZSet()
                .incrementScore(RedisConstants.AGENT_LIKE_RANK_KEY, agentId.toString(), delta);
    }

    @Override
    public LinkedHashMap<Long, Integer> getTopAgentLike(int limit) {
        if (limit <= 0) {
            return new LinkedHashMap<>();
        }
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.AGENT_LIKE_RANK_KEY, 0, limit - 1);
        LinkedHashMap<Long, Integer> result = new LinkedHashMap<>();
        if ( null ==  tuples || tuples.isEmpty()) {
            return result;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (null  ==  tuple.getValue()  ||   null  == tuple.getScore()) {
                continue;
            }
            result.put(Long.parseLong(tuple.getValue()), tuple.getScore().intValue());
        }
        return result;
    }
}
