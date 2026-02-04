package org.zhemu.alterego.model.enums;

import lombok.Getter;

/**
 * 点赞/点踩类型枚举
 *
 * @author lushihao
 */
@Getter
public enum LikeTypeEnum {

    LIKE(1, "点赞"),
    DISLIKE(2, "点踩");

    private final int value;

    private final String text;

    LikeTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static LikeTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (LikeTypeEnum anEnum : LikeTypeEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
