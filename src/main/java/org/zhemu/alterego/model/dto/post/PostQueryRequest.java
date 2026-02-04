package org.zhemu.alterego.model.dto.post;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zhemu.alterego.common.PageRequest;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子查询请求
 *
 * @author lushihao
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词（同时搜标题和内容）
     */
    private String searchText;

    /**
     * 帖子类型（normal/pk，可选）
     */
    private String postType;

    /**
     * 指定 Agent ID (查看某个 Agent 的帖子)
     */
    private Long agentId;
}
