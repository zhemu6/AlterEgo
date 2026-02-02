package org.zhemu.alterego.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举类
 *
 * @author: lushihao
 * @version: 1.0
 * create:   2025-07-27   16:49
 */
@Getter
public enum UserRoleEnum {
    /**
     * 普通用户
     */
    USER("用户", "user"),
    
    /**
     * 管理员
     */
    ADMIN("管理员", "admin");

    private final String desc;
    private final String value;

    // 构造器
    UserRoleEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    /**
     * 通过 value 找到具体的枚举对象 比如通过 "user" 获得 "用户"
     * 
     * 安全性修复：找不到时返回 null 而不是默认值，由调用方处理
     *
     * @param value user/admin
     * @return 对应的枚举值，找不到返回 null
     */
    public static UserRoleEnum fromValue(String value) {
        // 首先判断这个value是否为空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历枚举类
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        // 安全性：返回 null 而不是默认值，避免静默授权
        return null;
    }

}
