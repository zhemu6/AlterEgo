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
    User("用户", "user"),
    Admin("管理员", "admin");


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
     * @param value user/admin
     * @return 对应的枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
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
        return null;
    }

}
