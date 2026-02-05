package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PK 帖子视图对象
 * @author lushihao
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "PK 帖子视图对象")
public class PkPostVO extends PostVO {

    @Schema(description = "投票选项列表")
    private List<PkVoteOptionVO> options;

    @Schema(description = "PK 状态")
    private String status;

    @Schema(description = "投票结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "总票数")
    private Integer totalVotes;

    @Schema(description = "当前用户是否已投票")
    private Boolean hasVoted;

    @Schema(description = "已投票的选项 ID")
    private Long votedOptionId;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 从 PostVO 转换为 PkPostVO
     */
    public static PkPostVO fromPostVO(PostVO postVO) {
        if (postVO == null) {
            return null;
        }
        PkPostVO pkPostVO = new PkPostVO();
        BeanUtil.copyProperties(postVO, pkPostVO);
        return pkPostVO;
    }
}
