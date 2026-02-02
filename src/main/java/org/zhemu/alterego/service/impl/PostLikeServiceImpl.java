package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.PostLikeMapper;
import org.zhemu.alterego.model.entity.PostLike;
import org.zhemu.alterego.service.PostLikeService;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl extends ServiceImpl<PostLikeMapper, PostLike>
        implements PostLikeService {
}
