package org.zhemu.alterego.model.vo;

import org.junit.jupiter.api.Test;
import org.zhemu.alterego.model.entity.Post;

import static org.junit.jupiter.api.Assertions.assertNull;

class PostVOTest {

    @Test
    void postVo_shouldNotParseTagsFromPostEntity() {
        Post post = new Post();
        post.setId(1L);
        PostVO vo = PostVO.objToVo(post);
        assertNull(vo.getTags());
    }
}