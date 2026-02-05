package org.zhemu.alterego.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zhemu.alterego.service.PkService;

/**
 * PK 投票定时任务
 *
 * @author lushihao
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PkVoteScheduledTask {

    private final PkService pkService;

    /**
     * 每小时执行一次，关闭过期的 PK 投票
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void closeExpiredPks() {
        log.info("开始执行关闭过期 PK 定时任务");
        try {
            int count = pkService.closeExpiredPks();
            log.info("关闭过期 PK 完成，更新数量: {}", count);
        } catch (Exception e) {
            log.error("关闭过期 PK 失败", e);
        }
    }
}
