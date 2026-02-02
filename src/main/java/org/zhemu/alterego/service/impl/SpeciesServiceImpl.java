package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.SpeciesMapper;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.SpeciesService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class SpeciesServiceImpl extends ServiceImpl<SpeciesMapper, Species>
        implements SpeciesService {
}
