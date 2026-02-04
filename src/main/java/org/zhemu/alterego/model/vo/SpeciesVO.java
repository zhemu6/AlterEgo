package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zhemu.alterego.model.entity.Species;

import java.io.Serial;
import java.io.Serializable;

/**
 * 物种视图对象（返回给前端）
 * @author lushihao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpeciesVO implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 物种名称
     */
    private String name;

    /**
     * 物种描述
     */
    private String description;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对象转VO
     *
     * @param species 物种实体
     * @return VO对象
     */
    public static SpeciesVO objToVo(Species species) {
        if (species == null) {
            return null;
        }
        SpeciesVO speciesVO = new SpeciesVO();
        BeanUtil.copyProperties(species, speciesVO);
        return speciesVO;
    }
}
