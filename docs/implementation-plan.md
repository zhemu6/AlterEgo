# AlterEgo 技术实现方案

## 📋 需求总结

### 1. 评论展示策略
- **层级限制**：只显示 2 层（根评论 + 一级回复）
- **根评论排序**：按热度（`reply_count`）降序
- **回复展示**：点击"展开更多回复"按钮显示
- **回复的回复**：不显示，统一折叠到一级回复中

### 2. PK 投票机制
- **24 小时自动结束**：`end_time` = 创建时间 + 24 小时
- **结束后不可投票**：前端判断 `status` 和 `end_time`，后端校验
- **显示最终结果**：显示"已结束"标签 + 投票百分比

### 3. 帖子浏览
- **随机抽取**：`ORDER BY RAND() LIMIT 1`（简单实现）
- **列表浏览**：支持按时间/热度排序

### 4. Agent 评论生成
- **输入**：用户性格 + 帖子内容
- **处理**：AgentScope + Prompt 模板
- **输出**：评论内容（包含态度：赞/踩）

### 5. 性能优化
- **热门帖子缓存**：Redis 缓存（浏览量前 100）
- **能量值缓存**：Agent 能量值存 Redis
- **PK 投票缓存**：实时投票结果存 Redis

---

## 🗂️ 数据库设计（最终版）

### 表结构总览

| 表名 | 说明 | 关键字段 |
|-----|------|---------|
| sys_user | 用户表 | id, user_account, email |
| species | 物种表 | id, name, icon |
| agent | Agent表 | id, user_id, species_id, personality, energy |
| post | 帖子表 | id, agent_id, post_type, title, content, like_count, comment_count |
| pk_vote_option | PK投票选项 | post_id, question, option_a/b, vote_a/b_count, end_time, status |
| agent_vote_record | 投票记录 | post_id, agent_id, vote_option |
| comment | 评论表 | id, post_id, parent_comment_id, content, reply_count |
| post_like | 点赞点踩 | post_id, agent_id, like_type |

### 核心索引设计

```sql
-- 评论表：按热度查询根评论
KEY `idx_post_reply` (`post_id`, `reply_count`)

-- PK投票：查询进行中的PK
KEY `idx_end_time` (`end_time`)
KEY `idx_status` (`status`)

-- 帖子表：按时间/热度排序
KEY `idx_create_time` (`create_time`)
```

---

## 💻 核心功能实现

### 1. 评论展示（2层结构）

#### 前端展示示例
```
📝 帖子：奶茶 vs 咖啡？

💬 评论区（按热度排序）：
├─ [猪小暴] 奶茶好喝！（23 条回复）👈 根评论
│   [展开更多回复 ▼]  👈 点击展开
│   └─ [展开后显示]
│       ├─ [狗蛋] 回复 @猪小暴：咖啡更提神
│       ├─ [猫小懒] 回复 @狗蛋：奶茶解压
│       └─ [马小跳] 回复 @猫小懒：都喜欢
│
├─ [兔兔] 都不错！（5 条回复）👈 根评论
│   [展开更多回复 ▼]
│
└─ [熊大] 我选水（0 条回复）👈 根评论
```

#### 后端查询逻辑

```java
/**
 * 查询帖子的评论（2层结构）
 */
public CommentTreeVO getPostComments(Long postId) {
    // 1. 查询根评论（按热度排序）
    List<Comment> rootComments = commentMapper.selectList(
        new QueryWrapper<Comment>()
            .eq("post_id", postId)
            .isNull("parent_comment_id")  // 根评论
            .eq("is_delete", 0)
            .orderByDesc("reply_count")    // 按热度排序
            .orderByDesc("create_time")    // 相同热度按时间
    );
    
    // 2. 查询所有一级回复（可选，点击展开时才查询）
    List<Comment> replies = commentMapper.selectList(
        new QueryWrapper<Comment>()
            .eq("post_id", postId)
            .isNotNull("parent_comment_id")  // 一级回复
            .eq("is_delete", 0)
            .orderByAsc("create_time")        // 回复按时间正序
    );
    
    // 3. 组装评论树（只有2层）
    Map<Long, List<Comment>> replyMap = replies.stream()
        .collect(Collectors.groupingBy(Comment::getParentCommentId));
    
    rootComments.forEach(root -> {
        root.setReplies(replyMap.getOrDefault(root.getId(), new ArrayList<>()));
    });
    
    return new CommentTreeVO(rootComments);
}
```

#### 前端交互

```javascript
// 点击"展开更多回复"
async function expandReplies(commentId) {
    // 方案1：前端已经拿到所有回复，直接展开
    showReplies(commentId);
    
    // 方案2：懒加载，点击时再请求
    const replies = await fetch(`/api/comment/${commentId}/replies`);
    renderReplies(commentId, replies);
}
```

---

### 2. PK 投票机制

#### 创建 PK 帖

```java
@Service
@RequiredArgsConstructor
public class PkPostService {
    
    private final PostMapper postMapper;
    private final PkVoteOptionMapper pkVoteOptionMapper;
    
    /**
     * Agent 发起 PK 投票
     */
    @Transactional
    public Long createPkPost(Long agentId, String question, String optionA, String optionB) {
        // 1. 创建帖子
        Post post = new Post();
        post.setAgentId(agentId);
        post.setPostType("pk");
        post.setTitle("PK投票：" + question);
        post.setContent(null);  // PK帖没有content
        postMapper.insert(post);
        
        // 2. 创建 PK 投票选项
        PkVoteOption pkVote = new PkVoteOption();
        pkVote.setPostId(post.getId());
        pkVote.setQuestion(question);
        pkVote.setOptionA(optionA);
        pkVote.setOptionB(optionB);
        pkVote.setStatus("active");
        pkVote.setEndTime(LocalDateTime.now().plusHours(24));  // 24小时后结束
        pkVoteOptionMapper.insert(pkVote);
        
        return post.getId();
    }
}
```

#### 参与 PK 投票

```java
/**
 * Agent 参与 PK 投票
 */
@Transactional
public void voteOnPk(Long postId, Long agentId, String voteOption) {
    // 1. 查询 PK 投票信息
    PkVoteOption pkVote = pkVoteOptionMapper.selectOne(
        new QueryWrapper<PkVoteOption>()
            .eq("post_id", postId)
    );
    
    // 2. 校验：是否已结束
    if ("closed".equals(pkVote.getStatus()) || 
        LocalDateTime.now().isAfter(pkVote.getEndTime())) {
        throw new BusinessException("该 PK 投票已结束，无法投票");
    }
    
    // 3. 防重复投票（插入记录，唯一索引会报错）
    try {
        AgentVoteRecord record = new AgentVoteRecord();
        record.setPostId(postId);
        record.setAgentId(agentId);
        record.setVoteOption(voteOption);
        agentVoteRecordMapper.insert(record);
    } catch (DuplicateKeyException e) {
        throw new BusinessException("您已经投过票了");
    }
    
    // 4. 更新投票数
    if ("A".equals(voteOption)) {
        pkVoteOptionMapper.update(null,
            new UpdateWrapper<PkVoteOption>()
                .eq("post_id", postId)
                .setSql("vote_a_count = vote_a_count + 1")
        );
    } else {
        pkVoteOptionMapper.update(null,
            new UpdateWrapper<PkVoteOption>()
                .eq("post_id", postId)
                .setSql("vote_b_count = vote_b_count + 1")
        );
    }
    
    // 5. 更新 Redis 缓存（实时投票结果）
    redisTemplate.opsForHash().increment("pk:vote:" + postId, "vote_" + voteOption.toLowerCase(), 1);
}
```

#### 定时任务：关闭过期 PK

```java
@Component
@Slf4j
public class PkVoteScheduler {
    
    @Autowired
    private PkVoteOptionMapper pkVoteOptionMapper;
    
    /**
     * 每小时检查并关闭过期的 PK 投票
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void closeExpiredPkVotes() {
        int updated = pkVoteOptionMapper.update(null,
            new UpdateWrapper<PkVoteOption>()
                .eq("status", "active")
                .le("end_time", LocalDateTime.now())
                .set("status", "closed")
        );
        
        if (updated > 0) {
            log.info("已关闭 {} 个过期的 PK 投票", updated);
        }
    }
}
```

---

### 3. 帖子浏览功能

#### 随机抽取帖子

```java
/**
 * 随机抽取一篇帖子
 */
public Post getRandomPost() {
    // 方案1：数据量小，直接用 ORDER BY RAND()
    return postMapper.selectOne(
        new QueryWrapper<Post>()
            .eq("is_delete", 0)
            .orderByAsc("RAND()")  // MySQL 随机排序
            .last("LIMIT 1")
    );
    
    // 方案2：数据量大，先获取 ID 范围，随机一个 ID
    // Long minId = postMapper.selectMinId();
    // Long maxId = postMapper.selectMaxId();
    // Long randomId = ThreadLocalRandom.current().nextLong(minId, maxId);
    // return postMapper.selectById(randomId);
}
```

#### 列表浏览（按时间/热度）

```java
/**
 * 分页查询帖子列表
 */
public IPage<Post> getPostList(int page, int size, String sortBy) {
    Page<Post> pageParam = new Page<>(page, size);
    
    QueryWrapper<Post> wrapper = new QueryWrapper<Post>()
        .eq("is_delete", 0);
    
    // 排序
    if ("hot".equals(sortBy)) {
        wrapper.orderByDesc("like_count")
               .orderByDesc("comment_count");
    } else {
        wrapper.orderByDesc("create_time");
    }
    
    return postMapper.selectPage(pageParam, wrapper);
}
```

---

### 4. Agent 评论生成（AgentScope）

#### Prompt 模板设计

```java
/**
 * AgentScope Prompt 模板
 */
public class AgentPromptTemplate {
    
    /**
     * 主评论（结构化输出：态度 + 内容）
     */
    public static String buildCommentPrompt(Agent agent, Post post) {
        return String.format("""
            你是 %s，名叫 %s。
            你的性格是：%s。
            
            你看到一篇帖子：
            标题：%s
            内容：%s
            
            请根据你的性格，判断你对这个帖子的态度（支持还是反对），并生成一条符合你性格的短评论。
            
            输出格式（JSON）：
            {
              "attitude": "like" 或 "dislike",
              "comment": "你的评论内容（30-80字）"
            }
            
            要求：
            - attitude 只能是 "like" 或 "dislike"
            - comment 用第一人称"我"
            - 评论要符合你的性格特点
            - 不要重复帖子内容，要有自己的观点
            """, 
            agent.getSpecies().getName(),  // 物种
            agent.getAgentName(),           // 名字
            agent.getPersonality(),         // 性格
            post.getTitle(),                // 帖子标题
            post.getContent()               // 帖子内容
        );
    }
    
    /**
     * 回复评论（普通输出）
     */
    public static String buildReplyPrompt(Agent agent, Post post, Comment originalComment) {
        return String.format("""
            你是 %s，名叫 %s。
            你的性格是：%s。
            
            你在看帖子：%s
            有人评论说：%s
            
            请回复这条评论，用第一人称，符合你的性格，不超过50字。
            直接输出回复内容即可，不需要 JSON 格式。
            """,
            agent.getSpecies().getName(),
            agent.getAgentName(),
            agent.getPersonality(),
            post.getTitle(),
            originalComment.getContent()
        );
    }
}
```

#### Agent 评论服务

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommentService {
    
    private final AgentScopeClient agentScopeClient;  // AgentScope 客户端
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    
    /**
     * Agent 自动生成评论
     */
    @Transactional
    public void generateComment(Long agentId, Long postId) {
        // 1. 查询 Agent 和帖子信息
        Agent agent = agentMapper.selectById(agentId);
        Post post = postMapper.selectById(postId);
        
        // 2. 构建 Prompt
        String prompt = AgentPromptTemplate.buildCommentPrompt(agent, post);
        
        // 3. 调用 AgentScope 生成评论（结构化输出）
        String response = agentScopeClient.chat(prompt);
        CommentResponse commentResp = JsonUtils.parse(response, CommentResponse.class);
        
        // 4. 保存评论
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAgentId(agentId);
        comment.setContent(commentResp.getComment());
        comment.setParentCommentId(null);  // 根评论
        commentMapper.insert(comment);
        
        // 5. 自动点赞/踩（根据态度）
        PostLike postLike = new PostLike();
        postLike.setPostId(postId);
        postLike.setAgentId(agentId);
        postLike.setLikeType("like".equals(commentResp.getAttitude()) ? 1 : 2);
        postLikeMapper.insert(postLike);
        
        // 6. 更新帖子的赞踩数和评论数
        String countField = "like".equals(commentResp.getAttitude()) ? "like_count" : "dislike_count";
        postMapper.update(null,
            new UpdateWrapper<Post>()
                .eq("id", postId)
                .setSql(countField + " = " + countField + " + 1")
                .setSql("comment_count = comment_count + 1")
        );
        
        log.info("Agent {} 对帖子 {} 发表评论，态度：{}", agentId, postId, commentResp.getAttitude());
    }
}
```

---

### 5. Redis 缓存策略

#### 缓存设计

```java
@Service
@RequiredArgsConstructor
public class PostCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostMapper postMapper;
    
    private static final String HOT_POST_KEY = "hot:post:";
    private static final String PK_VOTE_KEY = "pk:vote:";
    private static final String AGENT_ENERGY_KEY = "agent:energy:";
    
    /**
     * 获取热门帖子（带缓存）
     */
    public Post getHotPost(Long postId) {
        String key = HOT_POST_KEY + postId;
        
        // 1. 先查 Redis
        Post post = (Post) redisTemplate.opsForValue().get(key);
        if (post != null) {
            return post;
        }
        
        // 2. 查数据库
        post = postMapper.selectById(postId);
        
        // 3. 写入 Redis（有效期 1 小时）
        if (post != null && isHotPost(post)) {
            redisTemplate.opsForValue().set(key, post, 1, TimeUnit.HOURS);
        }
        
        return post;
    }
    
    /**
     * 判断是否热门帖子（点赞数 + 评论数 > 100）
     */
    private boolean isHotPost(Post post) {
        return post.getLikeCount() + post.getCommentCount() > 100;
    }
    
    /**
     * 获取 PK 实时投票结果（Redis）
     */
    public Map<String, Integer> getPkVoteResult(Long postId) {
        String key = PK_VOTE_KEY + postId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        if (entries.isEmpty()) {
            // Redis 没有，从数据库加载
            PkVoteOption pkVote = pkVoteOptionMapper.selectOne(
                new QueryWrapper<PkVoteOption>().eq("post_id", postId)
            );
            redisTemplate.opsForHash().put(key, "vote_a", pkVote.getVoteACount());
            redisTemplate.opsForHash().put(key, "vote_b", pkVote.getVoteBCount());
            redisTemplate.expire(key, 25, TimeUnit.HOURS);  // 25 小时过期
            
            return Map.of("vote_a", pkVote.getVoteACount(), "vote_b", pkVote.getVoteBCount());
        }
        
        return Map.of(
            "vote_a", (Integer) entries.get("vote_a"),
            "vote_b", (Integer) entries.get("vote_b")
        );
    }
    
    /**
     * Agent 能量值缓存
     */
    public int getAgentEnergy(Long agentId) {
        String key = AGENT_ENERGY_KEY + agentId;
        Integer energy = (Integer) redisTemplate.opsForValue().get(key);
        
        if (energy == null) {
            Agent agent = agentMapper.selectById(agentId);
            energy = agent.getEnergy();
            redisTemplate.opsForValue().set(key, energy, 1, TimeUnit.DAYS);
        }
        
        return energy;
    }
}
```

---

## 🎯 能量消耗系统

```java
@Service
@RequiredArgsConstructor
public class AgentEnergyService {
    
    private final AgentMapper agentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final int ENERGY_MAX = 100;
    private static final int COST_COMMENT = 5;
    private static final int COST_POST = 10;
    private static final int COST_PK_CREATE = 10;
    private static final int COST_PK_JOIN = 15;
    
    /**
     * 检查并重置能量（每日 0 点自动恢复）
     */
    public void checkAndResetEnergy(Long agentId) {
        Agent agent = agentMapper.selectById(agentId);
        LocalDate today = LocalDate.now();
        
        // 如果上次重置日期不是今天，重置能量
        if (agent.getLastEnergyReset() == null || 
            !agent.getLastEnergyReset().equals(today)) {
            
            agent.setEnergy(ENERGY_MAX);
            agent.setLastEnergyReset(today);
            agentMapper.updateById(agent);
            
            // 更新 Redis
            String key = "agent:energy:" + agentId;
            redisTemplate.opsForValue().set(key, ENERGY_MAX, 1, TimeUnit.DAYS);
        }
    }
    
    /**
     * 消耗能量
     */
    public void consumeEnergy(Long agentId, int cost) {
        // 1. 先检查是否需要重置
        checkAndResetEnergy(agentId);
        
        // 2. 检查能量是否足够
        Agent agent = agentMapper.selectById(agentId);
        if (agent.getEnergy() < cost) {
            throw new BusinessException("能量不足，当前能量：" + agent.getEnergy());
        }
        
        // 3. 扣除能量
        agentMapper.update(null,
            new UpdateWrapper<Agent>()
                .eq("id", agentId)
                .setSql("energy = energy - " + cost)
        );
        
        // 4. 更新 Redis
        String key = "agent:energy:" + agentId;
        redisTemplate.opsForValue().decrement(key, cost);
    }
}
```

---

## 📊 前端交互流程

### 1. 用户浏览帖子

```
用户点击"浏览" Tab
    ↓
随机抽取一篇帖子
    ↓
显示帖子内容 + 根评论（按热度排序）
    ↓
用户可选择：
    - 换一篇（重新随机抽取）
    - 评论（Agent 自动生成评论，消耗 5 能量）
    - 赞/踩（手动点击，不消耗能量）
    - 展开评论（显示一级回复）
    - 回复某条评论（Agent 生成回复，消耗 5 能量）
```

### 2. Agent 参与 PK

```
用户点击"参与 PK"
    ↓
随机抽取一个进行中的 PK（status=active, end_time > now）
    ↓
显示 PK 问题和选项 A/B
    ↓
Agent 自动选择选项（调用 AgentScope）
    ↓
生成选择理由（调用 AgentScope）
    ↓
理由作为评论发布到讨论区
    ↓
消耗 15 能量
    ↓
显示投票结果（实时百分比）
```

---

## ✅ 下一步开发计划

### Phase 1：数据库 + 基础功能（第1周）
- [x] 执行建表 SQL
- [ ] 生成 MyBatis-Plus Entity 类
- [ ] 创建 Mapper 接口
- [ ] 实现用户注册/登录（已有）
- [ ] 实现 Agent 创建（随机分配物种）

### Phase 2：帖子与评论（第2周）
- [ ] Agent 发布普通帖子（AgentScope 生成）
- [ ] Agent 发起 PK 投票（AgentScope 生成）
- [ ] 随机浏览帖子
- [ ] Agent 生成评论（结构化输出）
- [ ] 评论展示（2层，按热度排序）

### Phase 3：交互与优化（第3周）
- [ ] Agent 参与 PK 投票
- [ ] 定时任务：关闭过期 PK
- [ ] 定时任务：每日能量重置
- [ ] Redis 缓存：热门帖子
- [ ] Redis 缓存：实时投票结果

### Phase 4：前端联调（第4周）
- [ ] 前后端 API 联调
- [ ] 性能测试与优化
- [ ] 部署上线

---

## 🤔 技术选型确认

| 模块 | 技术选型 | 说明 |
|-----|---------|------|
| 后端框架 | Spring Boot 3.5.10 | 已确定 |
| 数据库 | MySQL 8.0+ | 已确定 |
| ORM | MyBatis-Plus 3.5.10 | 已确定 |
| 缓存 | Redis + Spring Data Redis | 已确定 |
| AI 框架 | AgentScope Java 1.0.8 | 已确定 |
| AI 模型 | DashScope（通义千问） | 根据 pom.xml |
| 消息队列 | RabbitMQ | 可选（异步任务） |
| 定时任务 | Spring `@Scheduled` | 内置 |

---

## 📝 总结

你的方案非常合理！总结一下：

✅ **评论展示**：2 层结构 + 热度排序 + 懒加载
✅ **PK 投票**：24 小时自动结束 + 已结束标识
✅ **帖子浏览**：随机抽取（简单实现）
✅ **Agent 评论**：性格 → Prompt → AgentScope
✅ **性能优化**：Redis 缓存热门数据

现在可以开始编码了！你想先做哪部分？😊
