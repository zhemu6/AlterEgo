# PK Vote 功能实现工作计划

## TL;DR

> **Quick Summary**: 实现完整的 PK 投票功能，包括 Agent 发起 PK（AI 生成话题+选项）、Agent 投票（AI 选择立场+生成评论）、查询 PK 列表/详情、定时关闭过期 PK。参考现有的 Post/Comment 服务模式。
> 
> **Deliverables**:
> - 4 个 DTO 类（PkCreateRequest, PkVoteRequest, PkQueryRequest, AiPkGenerateResult）
> - 2 个 VO 类（PkVoteOptionVO, PkPostVO）
> - 2 个 AI 服务（AiPkGeneratorService, AiPkVoteGeneratorService）
> - 1 个核心业务服务（PkService + PkServiceImpl）
> - 1 个 Controller（PkVoteController 完善）
> - 1 个定时任务（PkVoteScheduledTask）
> - 常量扩展（Constants.java）
> 
> **Estimated Effort**: Medium（4-6 小时）
> **Parallel Execution**: YES - 3 waves
> **Critical Path**: Wave 1 (DTO/VO) → Wave 2 (AI 服务 + 常量) → Wave 3 (业务服务 + Controller + 定时任务)

---

## Context

### Original Request
实现完整的 PK 投票功能，参考现有的发帖流程：
- Agent 发起 PK（AI 生成话题+选项）
- Agent 投票（AI 选择立场+生成评论）
- 查询 PK 列表和详情
- 定时关闭过期 PK

### Interview Summary
**Key Discussions**:
- **测试策略**: 不写单元测试，由 Agent-Executed QA 验证
- **AI 生成模式**: AI 全自动生成话题 + 选项（用户不指定话题）
- **投票后行为**: 投票 + 生成评论（评论保存到 comment 表）
- **定时关闭机制**: Spring `@Scheduled`，每小时执行

**Research Findings**:
- `PostServiceImpl.aiGeneratePost`: 事务边界 + 校验顺序 + 原子更新模式
- `AiPostGeneratorServiceImpl`: AI 调用模式（ReActAgent + MySQL Session 持久化）
- 能量消耗：发 PK 15 点，投票 5 点
- 防重复投票：`agent_vote_record` 表唯一索引 `uk_agent_post`

### Confirmed Decisions
- **能量原子更新**：在 UPDATE 的 WHERE 子句中加入 `energy >= cost` 条件（修复现有并发问题）
- **防重复投票**：捕获 `DuplicateKeyException` 返回友好错误
- **PK 关闭后**：Service 层校验 `status = 'closed'` 拒绝投票
- **投票评论**：作为普通评论保存，`parent_comment_id = null`

---

## Work Objectives

### Core Objective
实现 Agent 发起 PK、投票、查询、定时关闭的完整功能链路，复用现有 AI 服务模式。

### Concrete Deliverables
| 类型 | 文件路径 |
|------|----------|
| DTO | `model/dto/pk/PkCreateRequest.java` |
| DTO | `model/dto/pk/PkVoteRequest.java` |
| DTO | `model/dto/pk/PkQueryRequest.java` |
| DTO | `model/dto/pk/AiPkGenerateResult.java` |
| DTO | `model/dto/pk/AiPkVoteResult.java` |
| VO | `model/vo/PkVoteOptionVO.java` |
| VO | `model/vo/PkPostVO.java` |
| Service | `service/AiPkGeneratorService.java` |
| Service | `service/impl/AiPkGeneratorServiceImpl.java` |
| Service | `service/AiPkVoteGeneratorService.java` |
| Service | `service/impl/AiPkVoteGeneratorServiceImpl.java` |
| Service | `service/PkService.java` |
| Service | `service/impl/PkServiceImpl.java` |
| Controller | `controller/PkVoteController.java`（完善） |
| Scheduled | `scheduled/PkVoteScheduledTask.java` |
| Constant | `constant/Constants.java`（扩展） |

### Definition of Done
- [ ] `./mvnw clean package -DskipTests` 编译成功
- [ ] `./mvnw spring-boot:run` 启动成功
- [ ] 4 个 API 端点可访问且返回正确响应格式
- [ ] Agent 发起 PK 后，`post` 表新增 `post_type='pk'` 记录
- [ ] Agent 投票后，`agent_vote_record` 和 `comment` 表新增记录
- [ ] 能量正确扣除（发 PK: 15，投票: 5）
- [ ] 重复投票返回友好错误
- [ ] 定时任务可手动触发验证

### Must Have
- 所有 API 返回 `BaseResponse<T>` 统一格式
- 参数校验使用 `jakarta.validation` 注解
- 事务边界使用 `@Transactional(rollbackFor = Exception.class)`
- 能量扣除使用原子更新 + WHERE 条件
- 日志使用 `@Slf4j` 参数化格式

### Must NOT Have (Guardrails)
- ❌ 不使用字段注入 `@Autowired`
- ❌ 不在 Controller 写业务逻辑
- ❌ 不直接暴露 Entity 给前端（必须用 VO）
- ❌ 不记录敏感信息到日志
- ❌ 不写单元测试（本次需求）
- ❌ 不实现"用户指定话题"功能（AI 全自动）

---

## Verification Strategy (MANDATORY)

> **UNIVERSAL RULE: ZERO HUMAN INTERVENTION**
>
> ALL tasks in this plan MUST be verifiable WITHOUT any human action.
> Every criterion MUST be verifiable by running a command or using a tool.

### Test Decision
- **Infrastructure exists**: YES（项目有 `./mvnw test`）
- **Automated tests**: NO（本次不写单元测试）
- **Framework**: N/A
- **Agent-Executed QA**: ALWAYS（mandatory for all tasks）

### Agent-Executed QA Scenarios (MANDATORY — ALL tasks)

验证工具选择：
| Type | Tool | How Agent Verifies |
|------|------|-------------------|
| **编译验证** | Bash (mvnw) | `./mvnw clean package -DskipTests` |
| **启动验证** | Bash (mvnw) | `./mvnw spring-boot:run` 观察日志 |
| **API 验证** | Bash (curl) | 发送请求，断言 HTTP 状态码和响应体 |
| **数据库验证** | Bash (mysql) | 查询表记录验证数据 |

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately) - DTO/VO 创建（可并行）:
├── Task 1: PkCreateRequest DTO
├── Task 2: PkVoteRequest DTO
├── Task 3: PkQueryRequest DTO
├── Task 4: AiPkGenerateResult DTO
├── Task 5: AiPkVoteResult DTO
├── Task 6: PkVoteOptionVO
└── Task 7: PkPostVO

Wave 2 (After Wave 1) - AI 服务 + 常量:
├── Task 8: Constants 扩展（添加 PK Session 前缀）
├── Task 9: AiPkGeneratorService 接口 + 实现
└── Task 10: AiPkVoteGeneratorService 接口 + 实现

Wave 3 (After Wave 2) - 核心业务:
├── Task 11: PkService 接口 + PkServiceImpl 实现
├── Task 12: PkVoteController 完善
└── Task 13: PkVoteScheduledTask 定时任务

Wave 4 (After Wave 3) - 集成验证:
└── Task 14: 编译 + 启动 + API 验证
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 1-7 | None | 9, 10, 11 | Each other (Wave 1) |
| 8 | None | 9, 10 | 1-7 (Wave 1 末期) |
| 9 | 4, 8 | 11 | 10 |
| 10 | 5, 8 | 11 | 9 |
| 11 | 6, 7, 9, 10 | 12, 13 | None |
| 12 | 1, 2, 3, 11 | 14 | 13 |
| 13 | 11 | 14 | 12 |
| 14 | 12, 13 | None | None (final) |

### Agent Dispatch Summary

| Wave | Tasks | Recommended Category |
|------|-------|---------------------|
| 1 | 1-7 | `quick`（简单 POJO 类） |
| 2 | 8-10 | `unspecified-low`（AI 服务需参考现有模式） |
| 3 | 11-13 | `unspecified-high`（核心业务逻辑，复杂度高） |
| 4 | 14 | `quick`（验证命令执行） |

---

## TODOs

### Wave 1: DTO/VO 创建（可并行）

- [ ] 1. 创建 PkCreateRequest DTO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/dto/pk/PkCreateRequest.java`
  - 字段：`agentId` (Long, @NotNull)
  - 添加 Swagger `@Schema` 注解
  - 添加 `@Data`, `Serializable`

  **Must NOT do**:
  - 不添加多余字段（用户不指定话题）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 简单 POJO 类，无复杂逻辑
  - **Skills**: `[]`
    - 无需特殊技能
  - **Skills Evaluated but Omitted**:
    - `brainstorming`: 需求已明确，无需创意探索

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2-7)
  - **Blocks**: Task 11, 12
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/dto/post/AgentPostGenerateRequest.java` - 参考 DTO 结构和注解风格
  - `AGENTS.md:3.6` - Controller 参数校验约定

  **Acceptance Criteria**:

  **Agent-Executed QA Scenarios:**

  ```
  Scenario: DTO 文件创建且编译通过
    Tool: Bash (mvnw)
    Preconditions: 项目根目录
    Steps:
      1. 检查文件存在：ls src/main/java/org/zhemu/alterego/model/dto/pk/PkCreateRequest.java
      2. 编译验证：./mvnw compile -q
    Expected Result: 文件存在且编译成功（exit code 0）
    Evidence: 命令输出
  ```

  **Commit**: YES (groups with 2-7)
  - Message: `feat(pk): add PK vote DTOs and VOs`
  - Files: `model/dto/pk/*.java`, `model/vo/Pk*.java`
  - Pre-commit: `./mvnw compile -q`

---

- [ ] 2. 创建 PkVoteRequest DTO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/dto/pk/PkVoteRequest.java`
  - 字段：
    - `agentId` (Long, @NotNull) - 投票的 Agent
    - `postId` (Long, @NotNull) - PK 帖子 ID
  - 添加 Swagger `@Schema` 注解

  **Must NOT do**:
  - 不添加 `optionId` 字段（由 AI 自动选择）

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 11, 12
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/dto/comment/AgentCommentGenerateRequest.java` - 类似的 Agent 操作请求结构

  **Acceptance Criteria**:

  ```
  Scenario: DTO 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: exit code 0
  ```

  **Commit**: YES (groups with 1, 3-7)

---

- [ ] 3. 创建 PkQueryRequest DTO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/dto/pk/PkQueryRequest.java`
  - 继承或包含分页字段（参考 `PostQueryRequest`）：
    - `pageNum` (long, default 1)
    - `pageSize` (long, default 10)
    - `sortField` (String)
    - `sortOrder` (String)
  - 可选过滤字段：
    - `status` (String) - 'active' / 'closed' / null(全部)
    - `agentId` (Long) - 按发起者筛选

  **Must NOT do**:
  - 不硬编码分页限制

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 12
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/dto/post/PostQueryRequest.java` - 分页查询 DTO 模板

  **Acceptance Criteria**:

  ```
  Scenario: DTO 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: exit code 0
  ```

  **Commit**: YES (groups with 1-2, 4-7)

---

- [ ] 4. 创建 AiPkGenerateResult DTO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/dto/pk/AiPkGenerateResult.java`
  - 字段（public，用于 AI JSON 解析）：
    - `topic` (String) - 话题标题
    - `description` (String) - 话题描述
    - `optionA` (String) - 选项 A 文字
    - `optionB` (String) - 选项 B 文字
    - `tags` (List<String>) - 标签列表
  - 添加 `@Data`, `Serializable`

  **Must NOT do**:
  - 不使用 private 字段（AI SDK 需要 public）

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 9
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/dto/post/AiPostGenerateResult.java` - AI 生成结果 DTO 模板（注意 public 字段）

  **Acceptance Criteria**:

  ```
  Scenario: DTO 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: exit code 0
  ```

  **Commit**: YES (groups with 1-3, 5-7)

---

- [ ] 5. 创建 AiPkVoteResult DTO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/dto/pk/AiPkVoteResult.java`
  - 字段（public）：
    - `selectedOption` (String) - "A" 或 "B"
    - `reason` (String) - 投票理由（将作为评论内容）
  - 添加 `@Data`, `Serializable`

  **Must NOT do**:
  - 不验证 selectedOption 值（AI 可能返回非预期值，Service 层处理）

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 10
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/dto/post/AiPostGenerateResult.java` - public 字段风格

  **Acceptance Criteria**:

  ```
  Scenario: DTO 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: exit code 0
  ```

  **Commit**: YES (groups with 1-4, 6-7)

---

- [ ] 6. 创建 PkVoteOptionVO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/vo/PkVoteOptionVO.java`
  - 字段：
    - `id` (Long)
    - `postId` (Long)
    - `optionText` (String)
    - `voteCount` (Integer)
    - `votePercentage` (Double) - 计算字段，票数占比
  - 添加静态方法 `objToVo(PkVoteOption option, int totalVotes)`
  - 添加 Swagger `@Schema` 注解

  **Must NOT do**:
  - 不暴露 `status`, `endTime`, `isDelete` 等内部字段

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 11
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/vo/PostVO.java` - VO 结构和 `objToVo` 方法模式
  - `src/main/java/org/zhemu/alterego/model/entity/PkVoteOption.java` - 实体字段参考

  **Acceptance Criteria**:

  ```
  Scenario: VO 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: exit code 0
  ```

  **Commit**: YES (groups with 1-5, 7)

---

- [ ] 7. 创建 PkPostVO

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/model/vo/PkPostVO.java`
  - 继承或组合 `PostVO`，额外字段：
    - `options` (List<PkVoteOptionVO>) - 两个选项
    - `status` (String) - 'active' / 'closed'
    - `endTime` (LocalDateTime) - 投票结束时间
    - `totalVotes` (Integer) - 总票数
    - `hasVoted` (Boolean) - 当前用户的 Agent 是否已投票
    - `votedOptionId` (Long) - 已投票的选项 ID（如果已投票）
  - 添加静态方法 `fromPostVO(PostVO postVO)`
  - 添加 Swagger `@Schema` 注解

  **Must NOT do**:
  - 不重复定义 PostVO 已有字段

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 11
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/model/vo/PostVO.java` - 基础 VO 结构
  - `src/main/java/org/zhemu/alterego/model/entity/PkVoteOption.java` - 选项实体

  **Acceptance Criteria**:

  ```
  Scenario: VO 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: exit code 0
  ```

  **Commit**: YES (groups with 1-6)

---

### Wave 2: AI 服务 + 常量

- [ ] 8. 扩展 Constants 常量

  **What to do**:
  - 编辑 `src/main/java/org/zhemu/alterego/constant/Constants.java`
  - 添加常量：
    ```java
    /** Agent PK 创建 Session 前缀 */
    String AGENT_PK_SESSION_PREFIX = "agent_pk_";
    
    /** Agent PK 投票 Session 前缀 */
    String AGENT_PK_VOTE_SESSION_PREFIX = "agent_pk_vote_";
    
    /** PK 发起消耗能量 */
    int PK_CREATE_ENERGY_COST = 15;
    
    /** PK 投票消耗能量 */
    int PK_VOTE_ENERGY_COST = 5;
    
    /** PK 持续时间（小时） */
    int PK_DURATION_HOURS = 24;
    ```

  **Must NOT do**:
  - 不修改现有常量

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (可与 Wave 1 末期并行)
  - **Blocks**: Task 9, 10, 11
  - **Blocked By**: None

  **References**:
  - `src/main/java/org/zhemu/alterego/constant/Constants.java` - 现有常量定义

  **Acceptance Criteria**:

  ```
  Scenario: 常量添加且编译通过
    Tool: Bash
    Steps:
      1. grep "PK_CREATE_ENERGY_COST" src/main/java/org/zhemu/alterego/constant/Constants.java
      2. ./mvnw compile -q
    Expected Result: grep 找到常量，编译成功
  ```

  **Commit**: YES
  - Message: `feat(pk): add PK constants`
  - Files: `constant/Constants.java`

---

- [ ] 9. 创建 AiPkGeneratorService 接口和实现

  **What to do**:
  - 创建接口 `src/main/java/org/zhemu/alterego/service/AiPkGeneratorService.java`
    ```java
    public interface AiPkGeneratorService {
        AiPkGenerateResult generatePk(Agent agent, Species species);
    }
    ```
  - 创建实现 `src/main/java/org/zhemu/alterego/service/impl/AiPkGeneratorServiceImpl.java`
    - 注入 `Model dashScopeModel`, `Session mysqlSession`
    - 使用 `AGENT_PK_SESSION_PREFIX + agentId` 作为 session ID
    - Prompt 结构（参考用户确认）：
      ```
      你的身份：物种：{species}，名字：{name}，性格：{personality}
      你想发起一个有趣的 PK 投票话题。
      要求：
      1. 话题要有趣、有争议性
      2. 两个选项要对立但都有道理
      3. 选项文字简短（10字以内），可用emoji
      输出 JSON：
      {
        "topic": "话题标题（30字以内）",
        "description": "话题描述（100字以内）",
        "optionA": "选项A文字",
        "optionB": "选项B文字",
        "tags": ["标签1", "标签2"]
      }
      ```
    - 调用 `ReActAgent.call(userMsg, AiPkGenerateResult.class)`
    - 降级方案：返回默认 PK 话题
    - 保存会话到 MySQL Session

  **Must NOT do**:
  - 不在 AI 服务中处理业务逻辑（能量、保存等）
  - 不使用字段注入

  **Recommended Agent Profile**:
  - **Category**: `unspecified-low`
    - Reason: 需要理解 AgentScope SDK 调用模式，但有明确参考
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `brainstorming`: 实现模式已明确

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Task 10)
  - **Blocks**: Task 11
  - **Blocked By**: Task 4, 8

  **References**:
  - `src/main/java/org/zhemu/alterego/service/AiPostGeneratorService.java` - 接口定义模式
  - `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java:41-131` - 完整的 AI 调用流程：Prompt 构建 → ReActAgent 创建 → Session 加载 → 调用 → 解析 → Session 保存
  - `src/main/java/org/zhemu/alterego/model/dto/pk/AiPkGenerateResult.java` - 返回类型

  **Acceptance Criteria**:

  ```
  Scenario: AI 服务编译通过
    Tool: Bash
    Steps:
      1. ls src/main/java/org/zhemu/alterego/service/AiPkGeneratorService.java
      2. ls src/main/java/org/zhemu/alterego/service/impl/AiPkGeneratorServiceImpl.java
      3. ./mvnw compile -q
    Expected Result: 文件存在，编译成功
  ```

  **Commit**: YES
  - Message: `feat(pk): add AI PK generator service`
  - Files: `service/AiPkGeneratorService.java`, `service/impl/AiPkGeneratorServiceImpl.java`

---

- [ ] 10. 创建 AiPkVoteGeneratorService 接口和实现

  **What to do**:
  - 创建接口 `src/main/java/org/zhemu/alterego/service/AiPkVoteGeneratorService.java`
    ```java
    public interface AiPkVoteGeneratorService {
        AiPkVoteResult generateVote(Agent agent, Species species, Post pkPost, 
                                     PkVoteOption optionA, PkVoteOption optionB);
    }
    ```
  - 创建实现 `src/main/java/org/zhemu/alterego/service/impl/AiPkVoteGeneratorServiceImpl.java`
    - 注入 `Model dashScopeModel`, `Session mysqlSession`
    - 使用 `AGENT_PK_VOTE_SESSION_PREFIX + agentId` 作为 session ID
    - Prompt 结构（参考用户确认）：
      ```
      你的身份：{species}，{name}，{personality}
      PK 话题：{post.title}
      话题描述：{post.content}
      选项 A：{optionA.optionText}
      选项 B：{optionB.optionText}
      
      要求：
      1. 你必须选择支持其中一个选项（A 或 B）
      2. 生成 50 字以内的投票理由，符合你的性格
      3. 输出 JSON：
      {
        "selectedOption": "A" or "B",
        "reason": "你的投票理由"
      }
      ```
    - 调用 `ReActAgent.call(userMsg, AiPkVoteResult.class)`
    - 降级方案：随机选择 A/B，返回默认理由
    - 保存会话到 MySQL Session

  **Must NOT do**:
  - 不在 AI 服务中保存投票记录
  - 不处理重复投票检查

  **Recommended Agent Profile**:
  - **Category**: `unspecified-low`
    - Reason: 与 Task 9 类似，有明确参考
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Task 9)
  - **Blocks**: Task 11
  - **Blocked By**: Task 5, 8

  **References**:
  - `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java` - AI 调用模式
  - `src/main/java/org/zhemu/alterego/service/impl/AiCommentGeneratorServiceImpl.java` - 评论生成参考（如果存在）
  - `src/main/java/org/zhemu/alterego/model/dto/pk/AiPkVoteResult.java` - 返回类型

  **Acceptance Criteria**:

  ```
  Scenario: AI 投票服务编译通过
    Tool: Bash
    Steps:
      1. ls src/main/java/org/zhemu/alterego/service/AiPkVoteGeneratorService.java
      2. ls src/main/java/org/zhemu/alterego/service/impl/AiPkVoteGeneratorServiceImpl.java
      3. ./mvnw compile -q
    Expected Result: 文件存在，编译成功
  ```

  **Commit**: YES
  - Message: `feat(pk): add AI PK vote generator service`
  - Files: `service/AiPkVoteGeneratorService.java`, `service/impl/AiPkVoteGeneratorServiceImpl.java`

---

### Wave 3: 核心业务

- [ ] 11. 创建 PkService 接口和 PkServiceImpl 实现

  **What to do**:

  **接口** `src/main/java/org/zhemu/alterego/service/PkService.java`:
  ```java
  public interface PkService {
      /** Agent 发起 PK */
      PkPostVO createPk(PkCreateRequest request, Long userId);
      
      /** Agent 投票 */
      PkPostVO vote(PkVoteRequest request, Long userId);
      
      /** 分页查询 PK 列表 */
      Page<PkPostVO> listPkByPage(PkQueryRequest request, Long userId);
      
      /** 获取 PK 详情 */
      PkPostVO getPkById(Long postId, Long userId);
      
      /** 关闭过期 PK（定时任务调用） */
      int closeExpiredPks();
  }
  ```

  **实现** `src/main/java/org/zhemu/alterego/service/impl/PkServiceImpl.java`:

  **依赖注入**（构造器注入）:
  - `AgentService`, `SpeciesService`, `PostService`
  - `PkVoteOptionService`, `AgentVoteRecordService`
  - `AiPkGeneratorService`, `AiPkVoteGeneratorService`
  - `CommentService`, `TagService`, `PostTagService`

  **createPk 方法** (核心流程):
  1. 校验 Agent 存在性
  2. 校验 Agent 归属（userId）
  3. **原子能量检查+扣除**：
     ```java
     boolean ok = agentService.lambdaUpdate()
         .eq(Agent::getId, agentId)
         .ge(Agent::getEnergy, PK_CREATE_ENERGY_COST)
         .setSql("energy = energy - " + PK_CREATE_ENERGY_COST + ", post_count = post_count + 1")
         .update();
     ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "能量不足或并发冲突");
     ```
  4. 获取物种信息
  5. 调用 AI 生成 PK 话题
  6. 保存 Post（`postType = 'pk'`）
  7. 保存两个 PkVoteOption（status='active', endTime=now+24h）
  8. 保存标签（复用 PostServiceImpl.savePostTags 逻辑）
  9. 返回 PkPostVO

  **vote 方法** (核心流程):
  1. 校验 Agent 存在性和归属
  2. 校验 Post 存在且为 PK 类型
  3. 获取 PkVoteOption 列表，校验 status='active'
  4. 校验未过期（endTime > now）
  5. **原子能量检查+扣除**
  6. 调用 AI 生成投票选择
  7. 解析 selectedOption（A/B → optionId）
  8. **保存投票记录**（捕获 DuplicateKeyException）:
     ```java
     try {
         agentVoteRecordService.save(AgentVoteRecord.builder()
             .agentId(agentId)
             .postId(postId)
             .optionId(selectedOptionId)
             .build());
     } catch (DuplicateKeyException e) {
         throw new BusinessException(ErrorCode.OPERATION_ERROR, "你的 Agent 已经投过票了");
     }
     ```
  9. **原子更新选项票数**:
     ```java
     pkVoteOptionService.lambdaUpdate()
         .eq(PkVoteOption::getId, selectedOptionId)
         .setSql("vote_count = vote_count + 1")
         .update();
     ```
  10. **保存评论**（AI 生成的 reason 作为评论内容）:
      ```java
      Comment comment = Comment.builder()
          .postId(postId)
          .agentId(agentId)
          .content(aiResult.getReason())
          .parentCommentId(null)  // 顶级评论
          .build();
      commentService.save(comment);
      // 更新 post.comment_count
      postService.lambdaUpdate()
          .eq(Post::getId, postId)
          .setSql("comment_count = comment_count + 1")
          .update();
      ```
  11. 返回 PkPostVO

  **listPkByPage 方法**:
  1. 构建查询条件（postType='pk', status 过滤）
  2. 分页查询 Post
  3. 批量查询关联的 PkVoteOption
  4. 填充 PkPostVO（包括 hasVoted 状态）
  5. 返回分页结果

  **getPkById 方法**:
  1. 查询 Post（校验 postType='pk'）
  2. 查询两个 PkVoteOption
  3. 查询当前用户 Agent 的投票记录
  4. 组装 PkPostVO
  5. 返回

  **closeExpiredPks 方法**:
  ```java
  return pkVoteOptionService.lambdaUpdate()
      .eq(PkVoteOption::getStatus, "active")
      .lt(PkVoteOption::getEndTime, LocalDateTime.now())
      .set(PkVoteOption::getStatus, "closed")
      .update() ? 1 : 0;  // 或返回实际更新行数
  ```

  **Must NOT do**:
  - 不在 Controller 写业务逻辑
  - 不使用 `agent.getEnergy() < cost` 的非原子检查
  - 不暴露 Entity 给前端

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 核心业务逻辑，包含事务管理、并发控制、多表操作
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `brainstorming`: 实现细节已在计划中明确

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 3 (sequential within wave)
  - **Blocks**: Task 12, 13
  - **Blocked By**: Task 6, 7, 9, 10

  **References**:
  - `src/main/java/org/zhemu/alterego/service/impl/PostServiceImpl.java:59-108` - aiGeneratePost 完整流程：校验 → AI 调用 → 保存 → 原子更新
  - `src/main/java/org/zhemu/alterego/service/impl/PostServiceImpl.java:221-252` - savePostTags 标签保存逻辑
  - `src/main/java/org/zhemu/alterego/service/impl/CommentServiceImpl.java` - 评论保存参考
  - `src/main/java/org/zhemu/alterego/model/entity/PkVoteOption.java` - 选项实体字段
  - `src/main/java/org/zhemu/alterego/model/entity/AgentVoteRecord.java` - 投票记录实体
  - `src/main/java/org/zhemu/alterego/exception/ThrowUtils.java` - 快捷抛错工具
  - `src/main/java/org/zhemu/alterego/constant/Constants.java` - 能量常量

  **Acceptance Criteria**:

  ```
  Scenario: PkService 编译通过
    Tool: Bash
    Steps:
      1. ls src/main/java/org/zhemu/alterego/service/PkService.java
      2. ls src/main/java/org/zhemu/alterego/service/impl/PkServiceImpl.java
      3. ./mvnw compile -q
    Expected Result: 文件存在，编译成功
  ```

  **Commit**: YES
  - Message: `feat(pk): implement PkService with create/vote/query/close`
  - Files: `service/PkService.java`, `service/impl/PkServiceImpl.java`
  - Pre-commit: `./mvnw compile -q`

---

- [ ] 12. 完善 PkVoteController

  **What to do**:
  - 编辑 `src/main/java/org/zhemu/alterego/controller/PkVoteController.java`
  - 注入 `PkService`（替换现有的 `PkVoteOptionService`）
  - 实现 4 个端点：

  ```java
  @RestController
  @RequiredArgsConstructor
  @RequestMapping("/pk")
  @Slf4j
  public class PkVoteController {
  
      private final PkService pkService;
  
      /**
       * Agent 发起 PK
       */
      @PostMapping("/create")
      public BaseResponse<PkPostVO> createPk(
              @RequestBody @Validated PkCreateRequest request,
              HttpServletRequest httpRequest) {
          Long userId = (Long) httpRequest.getAttribute("userId");
          ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
          PkPostVO result = pkService.createPk(request, userId);
          return ResultUtils.success(result);
      }
  
      /**
       * Agent 投票
       */
      @PostMapping("/vote")
      public BaseResponse<PkPostVO> vote(
              @RequestBody @Validated PkVoteRequest request,
              HttpServletRequest httpRequest) {
          Long userId = (Long) httpRequest.getAttribute("userId");
          ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
          PkPostVO result = pkService.vote(request, userId);
          return ResultUtils.success(result);
      }
  
      /**
       * 分页查询 PK 列表
       */
      @PostMapping("/list/page")
      public BaseResponse<Page<PkPostVO>> listPkByPage(
              @RequestBody PkQueryRequest request,
              HttpServletRequest httpRequest) {
          Long userId = (Long) httpRequest.getAttribute("userId");
          Page<PkPostVO> result = pkService.listPkByPage(request, userId);
          return ResultUtils.success(result);
      }
  
      /**
       * 获取 PK 详情
       */
      @GetMapping("/get")
      public BaseResponse<PkPostVO> getPkById(
              @RequestParam Long postId,
              HttpServletRequest httpRequest) {
          Long userId = (Long) httpRequest.getAttribute("userId");
          PkPostVO result = pkService.getPkById(postId, userId);
          return ResultUtils.success(result);
      }
  }
  ```

  **Must NOT do**:
  - 不在 Controller 写业务逻辑
  - 不直接返回 Entity

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Controller 层代码简单，只是调用 Service
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Task 13)
  - **Blocks**: Task 14
  - **Blocked By**: Task 1, 2, 3, 11

  **References**:
  - `src/main/java/org/zhemu/alterego/controller/PostController.java` - Controller 结构参考
  - `src/main/java/org/zhemu/alterego/common/BaseResponse.java` - 统一响应
  - `src/main/java/org/zhemu/alterego/common/ResultUtils.java` - 响应工具类
  - `src/main/java/org/zhemu/alterego/exception/ThrowUtils.java` - 快捷抛错

  **Acceptance Criteria**:

  ```
  Scenario: Controller 编译通过
    Tool: Bash
    Steps:
      1. ./mvnw compile -q
    Expected Result: 编译成功
  
  Scenario: Controller 端点存在
    Tool: Bash
    Steps:
      1. grep -E "@(Post|Get)Mapping" src/main/java/org/zhemu/alterego/controller/PkVoteController.java | wc -l
    Expected Result: 输出 4（4 个端点）
  ```

  **Commit**: YES
  - Message: `feat(pk): implement PkVoteController endpoints`
  - Files: `controller/PkVoteController.java`

---

- [ ] 13. 创建 PkVoteScheduledTask 定时任务

  **What to do**:
  - 创建 `src/main/java/org/zhemu/alterego/scheduled/PkVoteScheduledTask.java`
  - 使用 `@Scheduled` 每小时执行
  - 调用 `PkService.closeExpiredPks()`

  ```java
  package org.zhemu.alterego.scheduled;
  
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.scheduling.annotation.Scheduled;
  import org.springframework.stereotype.Component;
  import org.zhemu.alterego.service.PkService;
  
  /**
   * PK 投票定时任务
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
  ```

  **Must NOT do**:
  - 不在定时任务中写复杂业务逻辑
  - 不忽略异常（至少记录日志）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 简单的定时任务配置
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Task 12)
  - **Blocks**: Task 14
  - **Blocked By**: Task 11

  **References**:
  - Spring `@Scheduled` 官方文档
  - 确认 `@EnableScheduling` 已在 Application 或 Config 中启用

  **Acceptance Criteria**:

  ```
  Scenario: 定时任务编译通过
    Tool: Bash
    Steps:
      1. ls src/main/java/org/zhemu/alterego/scheduled/PkVoteScheduledTask.java
      2. ./mvnw compile -q
    Expected Result: 文件存在，编译成功
  
  Scenario: @Scheduled 注解存在
    Tool: Bash
    Steps:
      1. grep "@Scheduled" src/main/java/org/zhemu/alterego/scheduled/PkVoteScheduledTask.java
    Expected Result: 找到 @Scheduled 注解
  ```

  **Commit**: YES
  - Message: `feat(pk): add scheduled task to close expired PKs`
  - Files: `scheduled/PkVoteScheduledTask.java`

---

### Wave 4: 集成验证

- [ ] 14. 编译 + 启动 + API 验证

  **What to do**:
  1. 完整编译项目
  2. 启动应用
  3. 验证 4 个 API 端点可访问

  **Must NOT do**:
  - 不跳过任何验证步骤

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 执行验证命令
  - **Skills**: `["verification-before-completion"]`
    - `verification-before-completion`: 确保验证完整性

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 4 (final)
  - **Blocks**: None
  - **Blocked By**: Task 12, 13

  **References**:
  - `AGENTS.md:1` - Build/Run 命令

  **Acceptance Criteria**:

  **Agent-Executed QA Scenarios:**

  ```
  Scenario: 项目编译成功
    Tool: Bash (mvnw)
    Preconditions: 项目根目录
    Steps:
      1. ./mvnw clean package -DskipTests
    Expected Result: BUILD SUCCESS, exit code 0
    Failure Indicators: BUILD FAILURE, compilation error
    Evidence: 命令输出保存到 .sisyphus/evidence/task-14-compile.txt

  Scenario: 应用启动成功
    Tool: Bash (后台启动 + 日志检查)
    Preconditions: 编译成功
    Steps:
      1. 启动应用（后台）：./mvnw spring-boot:run &
      2. 等待 60 秒
      3. 检查日志：grep "Started BackendApplication" 或 curl health endpoint
      4. 停止应用
    Expected Result: 应用启动成功，日志包含 "Started"
    Failure Indicators: 启动失败，端口占用，Bean 创建失败
    Evidence: 启动日志

  Scenario: API 端点可访问 - /pk/list/page
    Tool: Bash (curl)
    Preconditions: 应用运行中
    Steps:
      1. curl -s -w "\n%{http_code}" -X POST http://localhost:8124/api/pk/list/page \
           -H "Content-Type: application/json" \
           -d '{"pageNum":1,"pageSize":10}'
    Expected Result: HTTP 200, 响应包含 "code":0
    Failure Indicators: HTTP 4xx/5xx, 连接拒绝
    Evidence: 响应体保存

  Scenario: API 端点可访问 - /pk/get
    Tool: Bash (curl)
    Preconditions: 应用运行中
    Steps:
      1. curl -s -w "\n%{http_code}" "http://localhost:8124/api/pk/get?postId=1"
    Expected Result: HTTP 200（即使数据不存在，也应返回 4xx 而非 5xx）
    Evidence: 响应体保存

  Scenario: API 端点需要登录 - /pk/create
    Tool: Bash (curl)
    Preconditions: 应用运行中
    Steps:
      1. curl -s -w "\n%{http_code}" -X POST http://localhost:8124/api/pk/create \
           -H "Content-Type: application/json" \
           -d '{"agentId":1}'
    Expected Result: HTTP 401 或响应包含登录错误码（未带 token）
    Evidence: 响应体保存

  Scenario: API 端点需要登录 - /pk/vote
    Tool: Bash (curl)
    Preconditions: 应用运行中
    Steps:
      1. curl -s -w "\n%{http_code}" -X POST http://localhost:8124/api/pk/vote \
           -H "Content-Type: application/json" \
           -d '{"agentId":1,"postId":1}'
    Expected Result: HTTP 401 或响应包含登录错误码
    Evidence: 响应体保存
  ```

  **Commit**: NO（验证任务不产生代码变更）

---

## Commit Strategy

| After Task | Message | Files | Verification |
|------------|---------|-------|--------------|
| 1-7 | `feat(pk): add PK vote DTOs and VOs` | `model/dto/pk/*.java`, `model/vo/Pk*.java` | `./mvnw compile -q` |
| 8 | `feat(pk): add PK constants` | `constant/Constants.java` | `./mvnw compile -q` |
| 9 | `feat(pk): add AI PK generator service` | `service/AiPkGeneratorService.java`, `service/impl/AiPkGeneratorServiceImpl.java` | `./mvnw compile -q` |
| 10 | `feat(pk): add AI PK vote generator service` | `service/AiPkVoteGeneratorService.java`, `service/impl/AiPkVoteGeneratorServiceImpl.java` | `./mvnw compile -q` |
| 11 | `feat(pk): implement PkService with create/vote/query/close` | `service/PkService.java`, `service/impl/PkServiceImpl.java` | `./mvnw compile -q` |
| 12 | `feat(pk): implement PkVoteController endpoints` | `controller/PkVoteController.java` | `./mvnw compile -q` |
| 13 | `feat(pk): add scheduled task to close expired PKs` | `scheduled/PkVoteScheduledTask.java` | `./mvnw compile -q` |

---

## Success Criteria

### Verification Commands
```bash
# 编译验证
./mvnw clean package -DskipTests
# Expected: BUILD SUCCESS

# 启动验证（前台，观察日志）
./mvnw spring-boot:run
# Expected: Started BackendApplication in X seconds

# API 验证（另开终端）
curl -X POST http://localhost:8124/api/pk/list/page \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10}'
# Expected: {"code":0,"data":{"records":[],...},"message":"ok"}
```

### Final Checklist
- [ ] 所有 14 个任务完成
- [ ] 编译成功（无错误）
- [ ] 应用启动成功
- [ ] 4 个 API 端点返回正确格式
- [ ] 无字段注入 `@Autowired`
- [ ] 无 Entity 直接暴露
- [ ] 能量扣除使用原子更新
- [ ] 防重复投票逻辑正确
- [ ] 定时任务注册成功

---

## Risk Mitigation

| 风险 | 缓解措施 |
|------|----------|
| **能量并发超扣** | 使用 `lambdaUpdate().ge(energy, cost).setSql(...)` 原子检查+扣除 |
| **重复投票** | 数据库唯一索引 + 捕获 `DuplicateKeyException` |
| **AI 调用超时** | 设置 ReActAgent `maxIters(3)`，提供降级方案返回默认内容 |
| **PK 关闭后投票** | Service 层校验 `status = 'active'` 和 `endTime > now` |
| **事务过长** | AI 调用在事务内，可接受（与现有 Post 服务一致）；后续可优化为异步 |
| **@EnableScheduling 未启用** | Task 13 验收标准中检查应用启动日志 |

---

## Notes for Executor

1. **Wave 1 可完全并行**：7 个 DTO/VO 任务互不依赖，可同时执行
2. **Task 11 是关键**：核心业务逻辑，代码量最大，确保理解所有引用
3. **能量更新模式**：严格使用 `.ge(Agent::getEnergy, cost)` 在 WHERE 中，不要用 Java 层 if 判断
4. **AI 返回值处理**：`selectedOption` 可能返回非预期值（如 "1"、"选项A"），需做容错处理
5. **评论计数更新**：投票产生的评论需更新 `post.comment_count`，不要遗漏
