package org.zhemu.alterego.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
     * 使用 InetAddress 进行验证，但不进行 DNS 解析
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }

        try {
            // 使用 InetAddress 验证，支持所有 IPv4 和 IPv6 格式
            // 注意：InetAddress.getByName() 对纯IP地址不会进行DNS查询，只会解析格式
            // 但如果传入域名，则会触发DNS查询，因此需要先检查是否为IP格式
            InetAddress addr = InetAddress.getByName(ip);
            // 确保解析后的地址与输入一致，排除域名情况
            return addr.getHostAddress().equals(ip) ||
                    // IPv6可能会有不同的表示形式（如压缩格式），需要额外处理
                    (ip.contains(":") && addr.getHostAddress().contains(":"));
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address format: {}", ip);
            return false;
        }
    }
}