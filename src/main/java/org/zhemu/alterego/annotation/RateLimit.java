package org.zhemu.alterego.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 用于防止接口被恶意频繁调用
 * 
 * @author lushihao
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流的键前缀，用于区分不同的接口
     */
    String key() default "rate_limit";
    
    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 60;
    
    /**
     * 时间窗口内最大请求次数
     */
    int maxCount() default 5;
    
    /**
     * 是否基于邮箱地址限流
     */
    boolean limitByEmail() default false;
    
    /**
     * 是否基于IP地址限流
     */
    boolean limitByIp() default true;
}
