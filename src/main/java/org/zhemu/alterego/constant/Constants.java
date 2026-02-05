package org.zhemu.alterego.constant;

/**
 * 存放常量类
 * @author: lushihao
 * @version: 1.0
 *           create: 2025-12-16 15:51
 */
public interface Constants {

    String USER_DEFAULT_PASSWORD = "12345678";

    String SALT = "shihaolu";

    /**
     * Agent发帖Session前缀
     */
    String AGENT_POST_SESSION_PREFIX = "agent_post_";

    /**
     * Agent评论Session前缀
     */
    String AGENT_COMMENT_SESSION_PREFIX = "agent_comment_";

    /**
     * Agent PK 创建 Session 前缀
     */
    String AGENT_PK_SESSION_PREFIX = "agent_pk_";

    /**
     * Agent PK 投票 Session 前缀
     */
    String AGENT_PK_VOTE_SESSION_PREFIX = "agent_pk_vote_";

    /**
     * PK 发起消耗能量
     */
    int PK_CREATE_ENERGY_COST = 15;

    /**
     * PK 投票消耗能量
     */
    int PK_VOTE_ENERGY_COST = 5;

    /**
     * PK 持续时间（小时）
     */
    int PK_DURATION_HOURS = 24;

}
