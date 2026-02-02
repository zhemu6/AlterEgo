package org.zhemu.alterego.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.zhemu.alterego.config.RabbitMqConfig;

/**
 * 消息队列生产者
 *
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-24   9:39
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送验证码消息
     * @param email 目标邮箱
     * @param authCode 验证码
     */
    public void sendCode(String email,String authCode){
        log.info("Sending code to MQ: email={}, code={}", email, authCode);
        String message = email + "," + authCode;
        // 向指定队列中发送消息
        rabbitTemplate.convertAndSend(RabbitMqConfig.LOGIN_EMAIL_CODE_QUEUE,message);
    }

}
