package org.zhemu.alterego.constant;

/**
 * 用户角色枚举
 * @author lushihao
 */
public enum UserRole {
    /**
     * 普通用户
     */
    USER("user"),
    
    /**
     * 管理员
     */
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据字符串获取枚举
     */
    public static UserRole fromValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return USER; // 默认返回普通用户
    }
}
