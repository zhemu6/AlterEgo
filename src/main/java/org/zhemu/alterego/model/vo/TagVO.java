package org.zhemu.alterego.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.zhemu.alterego.model.entity.Tag;

/**
 * 标签视图对象
 */
@Data
@Schema(description = "标签视图对象")
public class TagVO implements Serializable {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "帖子数量")
    private Integer postCount;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Serial
    private static final long serialVersionUID = 1L;

    public static TagVO objToVo(Tag tag) {
        if (tag == null) {
            return null;
        }
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setPostCount(tag.getPostCount());
        vo.setLikeCount(tag.getLikeCount());
        return vo;
    }
}