package org.zhemu.alterego.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhemu.alterego.mapper.TagMapper;
import org.zhemu.alterego.model.entity.PostTag;
import org.zhemu.alterego.model.entity.Tag;
import org.zhemu.alterego.service.PostTagService;
import org.zhemu.alterego.service.TagService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {

    private final PostTagService postTagService;

    @Override
    public String normalize(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String cleaned = raw.trim();
        cleaned = cleaned.replaceAll("^#+", "");
        cleaned = cleaned.trim();
        if (StrUtil.isBlank(cleaned)) {
            return null;
        }
        cleaned = cleaned.toLowerCase();
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    @Override
    public Tag getOrCreateTag(String raw) {
        String nameNorm = normalize(raw);
        if (StrUtil.isBlank(nameNorm)) {
            return null;
        }
        Tag existing = this.lambdaQuery().eq(Tag::getNameNorm, nameNorm).one();
        if (existing != null) {
            return existing;
        }
        String name = raw.trim().replaceAll("^#+", "").trim();
        if (StrUtil.isBlank(name)) {
            return null;
        }
        Tag tag = Tag.builder()
                .name(name)
                .nameNorm(nameNorm)
                .postCount(0)
                .likeCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        try {
            boolean saved = this.save(tag);
            if (!saved) {
                return null;
            }
            return tag;
        } catch (DuplicateKeyException e) {
            log.debug("Tag already exists, nameNorm={}", nameNorm);
            return this.lambdaQuery().eq(Tag::getNameNorm, nameNorm).one();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePostTags(Long postId, List<String> rawTags) {
        if (postId == null || rawTags == null || rawTags.isEmpty()) {
            return;
        }
        Set<String> normalized = new HashSet<>();
        for (String raw : rawTags) {
            String nameNorm = normalize(raw);
            if (StrUtil.isBlank(nameNorm) || !normalized.add(nameNorm)) {
                continue;
            }
            Tag tag = getOrCreateTag(raw);
            if (tag == null || tag.getId() == null) {
                continue;
            }
            PostTag postTag = PostTag.builder()
                    .postId(postId)
                    .tagId(tag.getId())
                    .createTime(LocalDateTime.now())
                    .build();
            try {
                boolean relationSaved = postTagService.save(postTag);
                if (relationSaved) {
                    this.lambdaUpdate()
                            .eq(Tag::getId, tag.getId())
                            .setSql("post_count = post_count + 1")
                            .update();
                }
            } catch (DuplicateKeyException ignore) {
                // ignore duplicate relation
            }
        }
    }

    @Override
    public List<Tag> listHotTags(int limit) {
        int size = limit > 0 ? limit : 10;
        return this.lambdaQuery()
                .orderByDesc(Tag::getPostCount)
                .last("limit " + size)
                .list();
    }
}
