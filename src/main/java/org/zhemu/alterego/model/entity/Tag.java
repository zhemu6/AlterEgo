package org.zhemu.alterego.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签表
 *
 * @author lushihao
 * @TableName tag
 */
@TableName(value = "tag")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名（原始）
     */
    private String name;

    /**
     * 标签名规范化
     */
    private String nameNorm;

    /**
     * 关联帖子数
     */
    private Integer postCount;

    /**
     * 关联帖子总赞数
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}