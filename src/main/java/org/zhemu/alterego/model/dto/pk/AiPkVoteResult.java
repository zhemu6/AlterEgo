package org.zhemu.alterego.model.dto.pk;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * AI 生成投票选择结果
 * @author lushihao
 */
@Data
public class AiPkVoteResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * AI 选择的选项，"A" 或 "B"
     */
    public String selectedOption;

    /**
     * 投票理由，将作为评论内容，50 字以内
     */
    public String reason;
}
