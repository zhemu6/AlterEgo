package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.AgentMapper;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.service.AgentService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent>
        implements AgentService {
}
