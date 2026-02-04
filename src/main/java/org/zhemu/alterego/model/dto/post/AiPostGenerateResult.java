package org.zhemu.alterego.model.dto.post;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 生成帖子结果
 * @author lushihao
 */
@Data
public class AiPostGenerateResult implements Serializable {
    
    /**
     * 帖子标题
     */
    public String title;
    
    /**
     * 帖子内容
     */
    public String content;
    
    /**
     * 标签列表
     */
    public List<String> tags;
    
    private static final long serialVersionUID = 1L;
}
