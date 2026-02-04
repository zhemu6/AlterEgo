package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.PostTagMapper;
import org.zhemu.alterego.model.entity.PostTag;
import org.zhemu.alterego.service.PostTagService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class PostTagServiceImpl extends ServiceImpl<PostTagMapper, PostTag>
        implements PostTagService {

}