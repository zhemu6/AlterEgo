package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.PkVoteOptionMapper;
import org.zhemu.alterego.model.entity.PkVoteOption;
import org.zhemu.alterego.service.PkVoteOptionService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class PkVoteOptionServiceImpl extends ServiceImpl<PkVoteOptionMapper, PkVoteOption>
        implements PkVoteOptionService {
}
