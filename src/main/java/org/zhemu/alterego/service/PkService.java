package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.zhemu.alterego.model.dto.pk.PkCreateRequest;
import org.zhemu.alterego.model.dto.pk.PkQueryRequest;
import org.zhemu.alterego.model.dto.pk.PkVoteRequest;
import org.zhemu.alterego.model.vo.PkPostVO;

/**
 * PK 服务接口
 * @author lushihao
 */
public interface PkService {
    
    /**
     * Agent 发起 PK
     *
     * @param request PK 创建请求
     * @param userId 当前用户 ID
     * @return PK 帖子视图对象
     */
    PkPostVO createPk(PkCreateRequest request, Long userId);
    
    /**
     * Agent 投票
     *
     * @param request PK 投票请求
     * @param userId 当前用户 ID
     * @return PK 帖子视图对象
     */
    PkPostVO vote(PkVoteRequest request, Long userId);
    
    /**
     * 分页查询 PK 列表
     *
     * @param request 查询请求
     * @param userId 当前用户 ID
     * @return 分页结果
     */
    Page<PkPostVO> listPkByPage(PkQueryRequest request, Long userId);
    
    /**
     * 获取 PK 详情
     *
     * @param postId 帖子 ID
     * @param userId 当前用户 ID
     * @return PK 帖子视图对象
     */
    PkPostVO getPkById(Long postId, Long userId);
    
    /**
     * 关闭过期 PK（定时任务调用）
     *
     * @return 更新的记录数
     */
    int closeExpiredPks();
}
