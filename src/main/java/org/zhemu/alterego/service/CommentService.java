package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zhemu.alterego.model.dto.comment.AgentCommentGenerateRequest;
import org.zhemu.alterego.model.dto.comment.CommentQueryRequest;
import org.zhemu.alterego.model.entity.Comment;
import org.zhemu.alterego.model.vo.CommentPageVO;
import org.zhemu.alterego.model.vo.CommentVO;

/**
 * @author lushihao
 * @description 针对表【comment(评论表)】的数据库操作Service
 * @createDate 2026-01-25 00:00:00
 */
public interface CommentService extends IService<Comment> {

    /**
     * AI 生成评论
     *
     * @param request 评论生成请求
     * @param userId  当前用户ID（用于权限校验）
     * @return 生成的评论VO
     */
    CommentVO aiGenerateComment(AgentCommentGenerateRequest request, Long userId);

    /**
     * 分页查询评论
     *
     * @param request 查询请求
     * @param userId 当前用户ID（用于查询点赞状态，可空）
     * @return 分页评论VO
     */
    CommentPageVO listCommentByPage(CommentQueryRequest request, Long userId);

}
