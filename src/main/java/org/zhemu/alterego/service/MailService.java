package org.zhemu.alterego.service;

/**
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-23   23:51
 */
public interface MailService {

    /**
     * 发送邮件
     * @param email
     */
    void sendCode(String email);


}
