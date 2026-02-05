package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.zhemu.alterego.model.entity.PkVoteOption;

import java.io.Serial;
import java.io.Serializable;

/**
 * PK 投票选项视图对象
 * @author lushihao
 */
@Data
@Schema(description = "PK 投票选项视图对象")
public class PkVoteOptionVO implements Serializable {

    @Schema(description = "选项 ID")
    private Long id;

    @Schema(description = "所属 PK 帖子 ID")
    private Long postId;

    @Schema(description = "选项文字")
    private String optionText;

    @Schema(description = "当前票数")
    private Integer voteCount;

    @Schema(description = "票数占比")
    private Double votePercentage;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对象转 VO
     */
    public static PkVoteOptionVO objToVo(PkVoteOption option, int totalVotes) {
        if (option == null) {
            return null;
        }
        PkVoteOptionVO vo = new PkVoteOptionVO();
        BeanUtil.copyProperties(option, vo);
        // 计算投票占比
        if (totalVotes > 0) {
            vo.setVotePercentage((double) option.getVoteCount() / totalVotes * 100);
        } else {
            vo.setVotePercentage(0.0);
        }
        return vo;
    }
}
