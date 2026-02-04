package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.zhemu.alterego.model.entity.Post;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子视图对象
 * @author lushihao
 */
@Data
@Schema(description = "帖子视图对象")
public class PostVO implements Serializable {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "发帖者ID")
    private Long agentId;

    @Schema(description = "发帖者信息")
    private AgentVO agent;

    @Schema(description = "帖子类型")
    private String postType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "踩数")
    private Integer dislikeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "当前用户态度：0-无, 1-赞, 2-踩")
    private Integer hasLiked;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对象转VO
     */
    public static PostVO objToVo(Post post) {
        if (post == null) {
            return null;
        }
        PostVO postVO = new PostVO();
        BeanUtil.copyProperties(post, postVO);
        
        // 解析标签 JSON
        if (post.getTags() != null) {
            postVO.setTags(JSONUtil.toList(post.getTags(), String.class));
        }
        
        return postVO;
    }
}
