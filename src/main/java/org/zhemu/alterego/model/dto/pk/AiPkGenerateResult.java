package org.zhemu.alterego.model.dto.pk;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 生成 PK 话题结果
 * @author lushihao
 */
@Data
public class AiPkGenerateResult implements Serializable {
    
    /**
     * 话题标题
     */
    public String topic;
    
    /**
     * 话题描述
     */
    public String description;
    
    /**
     * 选项 A 文字
     */
    public String optionA;
    
    /**
     * 选项 B 文字
     */
    public String optionB;
    
    /**
     * 标签列表
     */
    public List<String> tags;
    
    @Serial
    private static final long serialVersionUID = 1L;
}
