package org.zhemu.alterego.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.zhemu.alterego.constant.UserRole;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.model.entity.SysUser;

/**
 * 用户上下文工具类
 * 用于在任何地方获取当前登录用户信息
 * @author lushihao
 */
public class UserContext {

    private static final String USER_ID_KEY = "userId";
    private static final String USER_ROLE_KEY = "userRole";
    private static final String CURRENT_USER_KEY = "currentUser";

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = getRequest();
        Object userId = request.getAttribute(USER_ID_KEY);
        ThrowUtils.throwIf(null==userId, ErrorCode.NOT_LOGIN_ERROR);
        return (Long) userId;
    }

    /**
     * 获取当前登录用户角色
     */
    public static String getCurrentUserRole() {
        HttpServletRequest request = getRequest();
        Object userRole = request.getAttribute(USER_ROLE_KEY);
        ThrowUtils.throwIf(null==userRole, ErrorCode.NOT_LOGIN_ERROR);
        return (String) userRole;
    }

    /**
     * 获取当前登录用户完整信息
     */
    public static SysUser getCurrentUser() {
        HttpServletRequest request = getRequest();
        Object user = request.getAttribute(CURRENT_USER_KEY);
        ThrowUtils.throwIf(null==user, ErrorCode.NOT_LOGIN_ERROR);
        return (SysUser) user;
    }

    /**
     * 判断当前用户是否是管理员
     */
    public static boolean isAdmin() {
        String role = getCurrentUserRole();
        return UserRole.ADMIN.getValue().equals(role);
    }

    /**
     * 要求当前用户必须是管理员
     */
    public static void requireAdmin() {
        ThrowUtils.throwIf(!isAdmin(), ErrorCode.NO_AUTH_ERROR,"需要管理员权限");
    }

    /**
     * 获取当前请求
     */
    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        ThrowUtils.throwIf(null == attributes, ErrorCode.SYSTEM_ERROR,"无法获取请求上下文");
        return attributes.getRequest();
    }
}
