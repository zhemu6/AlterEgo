package org.zhemu.alterego.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 * 
 * @author: lushihao
 * @version: 1.0
 * create: 2026-02-02 19:47
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 密码加密器
     * 使用 BCrypt 强哈希算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
