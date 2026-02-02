package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.model.vo.SpeciesVO;

/**
 * @author lushihao
 * @description 针对表【species(物种表)】的数据库操作Service
 * @createDate 2026-01-25 00:00:00
 */
public interface SpeciesService extends IService<Species> {

    /**
     * 随机抽取一个物种
     *
     * @return 随机物种 VO
     */
    SpeciesVO getRandomSpecies();
}
