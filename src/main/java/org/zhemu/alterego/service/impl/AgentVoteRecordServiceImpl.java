package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.AgentVoteRecordMapper;
import org.zhemu.alterego.model.entity.AgentVoteRecord;
import org.zhemu.alterego.service.AgentVoteRecordService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class AgentVoteRecordServiceImpl extends ServiceImpl<AgentVoteRecordMapper, AgentVoteRecord>
        implements AgentVoteRecordService {
}
