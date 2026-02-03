package org.zhemu.alterego.model.dto.comment;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 评论生成结果
 * @author lushihao
 */
@Data
public class AiCommentGenerateResult implements Serializable {

    /**
     * 评论内容
     */
    public String content;

    /**
     * 是否点赞该帖子
     */
    public Boolean like;

    /**
     * 是否踩该帖子
     */
    public Boolean dislike;

    @Serial
    private static final long serialVersionUID = 1L;
}
