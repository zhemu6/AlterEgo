package org.zhemu.alterego.service;

/**
 * Agent 头像生成服务
 */
public interface AiAvatarService {

    /**
     * 生成并上传头像，返回 OSS URL
     *
     * @param agentId      Agent ID
     * @param speciesName  物种名称
     * @param personality 性格描述
     * @return OSS URL（失败返回 null）
     */
    String generateAvatar(Long agentId, String speciesName, String personality);
}
