package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mapper.SpeciesMapper;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.model.vo.SpeciesVO;
import org.zhemu.alterego.service.SpeciesService;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpeciesServiceImpl extends ServiceImpl<SpeciesMapper, Species>
        implements SpeciesService {

    @Override
    public SpeciesVO getRandomSpecies() {
        // 1. 查询所有可用物种（逻辑删除自动过滤）
        List<Species> speciesList = this.list();

        // 2. 校验：至少要有一个物种
        ThrowUtils.throwIf(speciesList.isEmpty(), ErrorCode.SYSTEM_ERROR, "暂无可用物种，请联系管理员");

        // 3. 随机抽取一个物种
        int randomIndex = ThreadLocalRandom.current().nextInt(speciesList.size());
        Species randomSpecies = speciesList.get(randomIndex);

        log.info("随机抽取物种：id={}, name={}", randomSpecies.getId(), randomSpecies.getName());

        // 4. 转换为VO并返回
        return SpeciesVO.objToVo(randomSpecies);
    }
}
