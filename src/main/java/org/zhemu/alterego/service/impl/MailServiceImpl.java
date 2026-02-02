package org.zhemu.alterego.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mq.MessageProducer;
import org.zhemu.alterego.service.MailService;

import java.util.concurrent.TimeUnit;

/**
 * 邮件服务
 *
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-23   23:51
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final StringRedisTemplate stringRedisTemplate;
    //MQ消息队列相关
    private final MessageProducer messageProducer;
    /**
     * 发送邮件
     *
     * @param email 电子邮箱
     */
    @Override
    public void sendCode(String email) {
        // 1. 校验邮箱账号
        ThrowUtils.throwIf(StrUtil.isBlank(email), ErrorCode.PARAMS_ERROR, "请输入正确邮箱~");

        //2. 生成随机验证码
        String code = RandomUtil.randomNumbers(6);

        //3. 保存到Redis中（有效时间五分钟）
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_EMAIL_CODE + email,
                code,
                RedisConstants.LOGIN_EMAIL_CODE_TTL,
                TimeUnit.MINUTES);
        // 4.发送消息到 MQ 中
        messageProducer.sendCode(email,code);
    }
}
