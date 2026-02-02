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

    // 3.访客统计
    public static final String VISITOR_DAILY_KEY = "visitor:daily:";
    public static final String VISITOR_TOTAL_KEY = "visitor:total";

}
