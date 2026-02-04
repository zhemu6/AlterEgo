package org.zhemu.alterego.mq;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.zhemu.alterego.config.RabbitMqConfig;
import org.zhemu.alterego.model.dto.agent.AgentAvatarTaskMessage;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.service.AiAvatarService;
import org.zhemu.alterego.service.AgentService;

/**
 * 消息队列消费者
 *
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-24 9:39
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {

    private final JavaMailSender javaMailSender;
    private final AiAvatarService aiAvatarService;
    private final AgentService agentService;

    @Value("${spring.mail.username}")
    private String SENDER_EMAIL;

    @Value("${agent.avatar.default-url:}")
    private String defaultAvatarUrl;

    @RabbitListener(queues = RabbitMqConfig.LOGIN_EMAIL_CODE_QUEUE)
    public void receiveCodeMessage(String message) {
        log.info("Received code message from MQ: {}", message);
        try {
            // 1. 解析出来邮箱和验证码
            String[] parts = message.split(",");
            if (parts.length < 2) {
                log.warn("Invalid message format: {}", message);
                return;
            }
            String email = parts[0];
            String code = parts[1];
            // 2.发送验证码
            sendEmail(email, code);
        } catch (Exception e) {
            log.error("Failed to process code message", e);
        }
    }

    private void sendEmail(String to, String code) {
        log.info("Sending email to: {}", to);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(SENDER_EMAIL);
        mailMessage.setTo(to);
        mailMessage.setSubject("【智牛牧场】注册验证码");
        mailMessage.setText("您的注册验证码是：" + code + "。请在 5 分钟内完成注册。如非本人操作，请忽略此邮件。");
        javaMailSender.send(mailMessage);
        log.info("Email sent successfully to: {}", to);
    }

    @RabbitListener(queues = RabbitMqConfig.AGENT_AVATAR_QUEUE)
    public void receiveAgentAvatarTask(String message) {
        try {
            AgentAvatarTaskMessage task = JSONUtil.toBean(message, AgentAvatarTaskMessage.class);
            if (task == null || task.getAgentId() == null) {
                return;
            }
            Agent agent = agentService.getById(task.getAgentId());
            if (agent == null) {
                return;
            }
            if (agent.getAvatarUrl() != null
                    && (defaultAvatarUrl == null || !agent.getAvatarUrl().equals(defaultAvatarUrl))) {
                return;
            }
            String avatarUrl = aiAvatarService.generateAvatar(
                    task.getAgentId(), task.getSpeciesName(), task.getPersonality());
            if (avatarUrl != null) {
                agentService.lambdaUpdate()
                        .eq(Agent::getId, task.getAgentId())
                        .set(Agent::getAvatarUrl, avatarUrl)
                        .update();
            }
        } catch (Exception e) {
            log.error("Failed to process agent avatar task", e);
        }
    }

}
