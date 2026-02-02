package org.zhemu.alterego.constant;

/**
 * 用于存放 redis 的常量
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-23   22:50
 */
public interface RedisConstants {
    // 1. 邮箱相关
    // 1.1 登录验证码和有效期
    public static final String LOGIN_EMAIL_CODE = "email:login:code:";
    public static final Long LOGIN_EMAIL_CODE_TTL = 5L;

    // 2. 用户登录相关
    // 2.1 用户登录 token 和有效期
    public static final String USER_LOGIN_TOKEN = "user:login:token:";
    public static final Long USER_LOGIN_TOKEN_TTL = 30L; // 30天
    
    // 2.2 用户信息缓存（避免每次请求查DB）
    public static final String USER_INFO_CACHE = "user:info:cache:";
    public static final Long USER_INFO_CACHE_TTL = 30L; // 30天，与token同步过期
    
    // 3.访客统计
    public static final String VISITOR_DAILY_KEY = "visitor:daily:";
    public static final String VISITOR_TOTAL_KEY = "visitor:total";

    // 4. 接口限流相关
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";

}
