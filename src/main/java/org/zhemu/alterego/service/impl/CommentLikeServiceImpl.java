package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.mapper.CommentLikeMapper;
import org.zhemu.alterego.model.entity.CommentLike;
import org.zhemu.alterego.service.CommentLikeService;

/**
 * @author lushihao
 */
@Service
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike>
        implements CommentLikeService {
}
