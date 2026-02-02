package org.zhemu.alterego.annotation;

import org.zhemu.alterego.constant.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要特定角色才能访问的接口
 * 使用此注解会自动要求登录
 * @author lushihao
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * 允许访问的角色列表（满足其中一个即可）
     */
    UserRole[] value();
}
