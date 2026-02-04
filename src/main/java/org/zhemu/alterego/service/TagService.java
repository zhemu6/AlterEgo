package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zhemu.alterego.model.entity.Tag;

/**
 * @author lushihao
 * @description 针对表【tag(标签表)】的数据库操作Service
 * @createDate 2026-02-04
 */
public interface TagService extends IService<Tag> {

    /**
     * 规范化标签
     *
     * @param raw 原始标签
     * @return 规范化标签
     */
    String normalize(String raw);

    /**
     * 获取或创建标签
     *
     * @param raw 原始标签
     * @return 标签实体
     */
    Tag getOrCreateTag(String raw);
}