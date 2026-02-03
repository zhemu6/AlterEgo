package org.zhemu.alterego.config;

import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.session.mysql.MysqlSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * AgentScope 配置类
 * 配置 DashScope（通义千问）模型和持久化 Session
 * @author lushihao
 */
@Configuration
@Slf4j
public class AgentScopeConfig {

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Value("${agentscope.dashscope.model-name:qwen-plus}")
    private String modelName;

    @Value("${agentscope.dashscope.stream:true}")
    private Boolean stream;

    @Value("${agentscope.dashscope.enable-thinking:false}")
    private Boolean enableThinking;

    /**
     * 创建 DashScope 模型 Bean
     * 用于 AI 生成 Agent 名称、评论等
     *
     * @return DashScope 模型实例
     */
    @Bean
    public Model dashScopeModel() {
        log.info("初始化 DashScope 模型: modelName={}, stream={}, enableThinking={}", 
                modelName, stream, enableThinking);
        
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(stream)
                .enableThinking(enableThinking)
                .build();
        
        log.info("DashScope 模型初始化成功");
        return model;
    }

    /**
     * 配置 MySQL Session 持久化
     * 用于存储 Agent 的长期记忆（对话历史）
     *
     * @param dataSource 数据源
     * @return Session 实例
     */
    @Bean
    public Session mysqlSession(DataSource dataSource) {
        log.info("初始化 MySQL Session 持久化存储");
        // 数据库名: alterego, 表名: agentscope_sessions, 自动建表: true
        return new MysqlSession(dataSource, "alterego", "agentscope_sessions", true);
    }
}
