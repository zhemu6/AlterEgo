package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zhemu.alterego.model.entity.Tag;

import java.util.List;

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

    /**
     * Bind tags to a post (create tag if needed, ignore duplicates)
     *
     * @param postId  post id
     * @param rawTags raw tags
     */
    void savePostTags(Long postId, List<String> rawTags);

    /**
     * 获取热门标签
     *
     * @param limit 返回数量
     * @return 标签列表
     */
    List<Tag> listHotTags(int limit);
}
