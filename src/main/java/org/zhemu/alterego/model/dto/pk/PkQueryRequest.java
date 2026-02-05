package org.zhemu.alterego.model.dto.pk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zhemu.alterego.common.PageRequest;

import java.io.Serial;
import java.io.Serializable;

/**
 * PK 查询请求
 *
 * @author lushihao
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PkQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 投票状态过滤（'active' 进行中 / 'closed' 已结束 / null 表示全部）
     */
    @Schema(description = "投票状态过滤：active(进行中) / closed(已结束) / 空值表示全部")
    private String status;

    /**
     * 按发起者筛选
     */
    @Schema(description = "发起者 Agent ID")
    private Long agentId;
}
