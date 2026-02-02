package org.zhemu.alterego.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * IP地址工具类
 * 
 * @author lushihao
 */
@Slf4j
public class IpUtils {
    
    private static final String UNKNOWN = "unknown";
    
    /**
     * 获取客户端真实IP地址
     * 支持通过代理、负载均衡等场景
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        
        ip = request.getRemoteAddr();
        if (ip == null || ip.isEmpty()) {
            return UNKNOWN;
        }
        
        return ip;
    }
}
