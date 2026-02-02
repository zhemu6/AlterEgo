package org.zhemu.alterego.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * IP地址工具类
 * 
 * 安全注意事项：
 * - X-Forwarded-For 等代理头可能被客户端伪造
 * - 在生产环境中，建议配置可信代理列表，仅信任来自可信代理的头信息
 * - 或者在反向代理层（如 Nginx）过滤/重写这些头信息
 * 
 * @author lushihao
 */
@Slf4j
public class IpUtils {
    
    private static final String UNKNOWN = "unknown";
    private static final String IPV4_PATTERN = 
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final String IPV6_PATTERN = 
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$|^([0-9a-fA-F]{1,4}:){0,6}::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$";
    
    /**
     * 获取客户端真实IP地址
     * 支持通过代理、负载均衡等场景
     * 
     * 注意：此方法信任代理头信息。在生产环境中应该：
     * 1. 在反向代理（Nginx/LB）层面确保只有可信代理能设置这些头
     * 2. 或者配置应用只信任特定来源的代理头
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = getIpFromHeader(request, "X-Forwarded-For");
        if (isValidIp(ip)) {
            return ip;
        }
        
        ip = getIpFromHeader(request, "X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }
        
        ip = getIpFromHeader(request, "Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }
        
        ip = getIpFromHeader(request, "WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }
        
        ip = request.getRemoteAddr();
        if (ip == null || ip.isEmpty()) {
            return UNKNOWN;
        }
        
        return ip;
    }
    
    /**
     * 从请求头获取IP地址
     */
    private static String getIpFromHeader(HttpServletRequest request, String headerName) {
        String ip = request.getHeader(headerName);
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }
        return null;
    }
    
    /**
     * 验证IP地址格式是否有效（支持IPv4和IPv6）
     * 基本的格式验证，防止明显的伪造IP
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        // IPv4 或 IPv6 格式验证
        return ip.matches(IPV4_PATTERN) || ip.matches(IPV6_PATTERN);
    }
}
