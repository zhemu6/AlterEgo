package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.PostMapper;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.service.PostService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements PostService {
}
