package org.zhemu.alterego.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置类
 * 
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-24 9:45
 */
@Configuration
public class RabbitMqConfig {
    // 登录验证码队列
    public static final String LOGIN_EMAIL_CODE_QUEUE = "email.code.queue";

    // 论文分析队列
    public static final String PAPER_ANALYSIS_QUEUE = "paper.analysis.queue";

    @Bean
    public Queue codeQueue() {
        return new Queue(LOGIN_EMAIL_CODE_QUEUE, true);
    }

    @Bean
    public Queue analysisQueue() {
        return new Queue(PAPER_ANALYSIS_QUEUE, true);
    }

}
