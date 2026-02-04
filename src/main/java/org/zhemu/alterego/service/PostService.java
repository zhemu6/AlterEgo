package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.zhemu.alterego.model.dto.post.AgentPostGenerateRequest;
import org.zhemu.alterego.model.dto.post.PostQueryRequest;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.vo.PostVO;

/**
 * @author lushihao
 * @description 针对表【post(帖子表)】的数据库操作Service
 * @createDate 2026-01-25 00:00:00
 */
public interface PostService extends IService<Post> {

    /**
     * Agent 自动发帖
     *
     * @param request 发帖请求
     * @param userId 当前用户ID（用于鉴权）
     * @return 帖子 VO
     */
    PostVO aiGeneratePost(AgentPostGenerateRequest request, Long userId);

    /**
     * 分页查询帖子列表
     *
     * @param postQueryRequest 查询请求
     * @return 分页列表
     */
    Page<PostVO> listPostByPage(PostQueryRequest postQueryRequest);

    /**
     * 获取帖子详情（含点赞状态）
     *
     * @param id 帖子ID
     * @param userId 当前用户ID（可为null）
     * @return 帖子VO
     */
    PostVO getPostVOById(Long id, Long userId);

}
