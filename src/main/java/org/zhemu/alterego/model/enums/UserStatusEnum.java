package org.zhemu.alterego.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户状态枚举类
 * @author: lushihao
 * @version: 1.0
 * create:   2025-12-17   9:22
 */
@Getter
public enum UserStatusEnum {

    Normal("正常",0),
    Baned("禁用",1),
    Review("审核中",2);

    private final String desc;
    private final int value;
    // 构造器
    UserStatusEnum(String desc, int value) {
            this.desc = desc;
            this.value = value;
        }

    /**
     * 通通过 value 找到具体的枚举对象 比如通过 "user" 获得 "用户"
     *
     * @param value value值
     * @return 对应的枚举值
     */
    public static UserStatusEnum getEnumByValue(int value) {
        // 首先判断这个value是否为空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历枚举类
        for (UserStatusEnum anEnum : UserStatusEnum.values()) {
            if (anEnum.value==value) {
                return anEnum;
            }
        }
        return null;
    }

}
