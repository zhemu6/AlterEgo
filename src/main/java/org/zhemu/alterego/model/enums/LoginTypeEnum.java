package org.zhemu.alterego.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 登录类型枚举类
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-23   23:57
 */
@Getter
public enum LoginTypeEnum {
    Account("账密登录","account"),
    Email("邮箱登录","email");

    private final String desc;
    private final String value;
    // 构造器
    LoginTypeEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    /**
     * 通过 value 找到具体的枚举对象 比如通过 "email" 获得 "邮箱登录"
     *
     * @param value user/admin
     * @return 对应的枚举值
     */
    public static LoginTypeEnum getEnumByValue(String value) {
        // 首先判断这个value是否为空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历枚举类
        for (LoginTypeEnum anEnum : LoginTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
