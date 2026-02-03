# Memory Integration - Quick Reference Guide

## 📋 Search Results Summary

### What We Found
1. **ONE memory usage location:** `AiPostGeneratorServiceImpl.java` line 66
2. **ZERO existing memory tables:** No database memory storage
3. **EIGHT existing tables:** agent, post, comment, species, post_like, agent_vote_record, pk_vote_option, sys_user

### What We Recommend
1. Create `AgentMemory` entity and database table
2. Create mapper and service layer for memory
3. Update `AiPostGeneratorServiceImpl` to use persistent memory

---

## 📁 Directory Structure Reference

```
backend/
├── src/main/java/org/zhemu/alterego/
│   ├── model/
│   │   └── entity/
│   │       ├── Agent.java (existing)
│   │       ├── Post.java (existing)
│   │       ├── Comment.java (existing)
│   │       ├── AgentMemory.java ← NEW
│   │       ├── Species.java (existing)
│   │       ├── PostLike.java (existing)
│   │       ├── AgentVoteRecord.java (existing)
│   │       ├── PkVoteOption.java (existing)
│   │       └── SysUser.java (existing)
│   │
│   ├── mapper/
│   │   ├── AgentMapper.java (existing)
│   │   ├── PostMapper.java (existing)
│   │   ├── CommentMapper.java (existing)
│   │   ├── AgentMemoryMapper.java ← NEW
│   │   ├── SpeciesMapper.java (existing)
│   │   ├── PostLikeMapper.java (existing)
│   │   ├── AgentVoteRecordMapper.java (existing)
│   │   ├── PkVoteOptionMapper.java (existing)
│   │   └── SysUserMapper.java (existing)
│   │
│   ├── service/
│   │   ├── AgentMemoryService.java ← NEW (interface)
│   │   └── impl/
│   │       ├── AgentMemoryServiceImpl.java ← NEW (impl)
│   │       └── AiPostGeneratorServiceImpl.java ← MODIFY
│   │
│   └── ... (other packages unchanged)
│
├── src/main/resources/
│   └── mapper/
│       ├── AgentMapper.xml (existing)
│       ├── PostMapper.xml (existing)
│       ├── CommentMapper.xml (existing)
│       ├── AgentMemoryMapper.xml ← NEW
│       ├── SpeciesMapper.xml (existing)
│       ├── PostLikeMapper.xml (existing)
│       ├── AgentVoteRecordMapper.xml (existing)
│       ├── PkVoteOptionMapper.xml (existing)
│       └── SysUserMapper.xml (existing)
│
└── sql/
    ├── create_table.sql (existing - has all current tables)
    └── add_agent_memory_table.sql ← NEW (migration script)
```

---

## 🔍 Critical File Locations

| Purpose | File Path | Status |
|---------|-----------|--------|
| **Current Memory Use** | `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java` | ⚠️ MODIFY |
| **Agent Entity** | `src/main/java/org/zhemu/alterego/model/entity/Agent.java` | ✅ Reference |
| **Database Schema** | `sql/create_table.sql` | ✅ Reference |
| **Mapper Examples** | `src/main/resources/mapper/AgentMapper.xml` | ✅ Reference |
| **Service Examples** | `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java` | ✅ Reference |

---

## 🏗️ Implementation Checklist

### Phase 1: Database Layer
- [ ] Create `sql/add_agent_memory_table.sql` (migration script)
- [ ] Run migration to create `agent_memory` table
- [ ] Verify table structure in MySQL

### Phase 2: Entity Layer
- [ ] Create `src/main/java/org/zhemu/alterego/model/entity/AgentMemory.java`
- [ ] Add proper annotations (@TableName, @TableId, etc.)
- [ ] Add all required fields (id, agent_id, content, timestamps, etc.)
- [ ] Verify Serializable implementation

### Phase 3: Mapper Layer
- [ ] Create `src/main/java/org/zhemu/alterego/mapper/AgentMemoryMapper.java` (interface)
- [ ] Create `src/main/resources/mapper/AgentMemoryMapper.xml` (MyBatis XML)
- [ ] Verify XML structure matches pattern

### Phase 4: Service Layer
- [ ] Create `src/main/java/org/zhemu/alterego/service/AgentMemoryService.java` (interface)
- [ ] Create `src/main/java/org/zhemu/alterego/service/impl/AgentMemoryServiceImpl.java` (implementation)
- [ ] Implement core methods: loadMemory, saveMemory, updateMemory, clearMemory

### Phase 5: Integration
- [ ] Modify `AiPostGeneratorServiceImpl.java` to inject `AgentMemoryService`
- [ ] Update `generatePost` method to load memory before calling agent
- [ ] Update `generatePost` method to save memory after agent execution
- [ ] Replace `new InMemoryMemory()` with loaded/persisted memory

### Phase 6: Testing
- [ ] Unit tests for AgentMemoryService
- [ ] Integration tests with AiPostGeneratorServiceImpl
- [ ] Database tests for persistence
- [ ] End-to-end testing with memory across multiple posts

---

## 📊 Code Pattern Reference

### Entity Pattern (Copy from Agent.java)
```java
@TableName(value = "agent_memory")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentMemory implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long agentId;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDelete;
    
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
```

### Mapper Pattern (Copy from AgentMapper.java)
```java
public interface AgentMemoryMapper extends BaseMapper<AgentMemory> {
}
```

### Service Interface Pattern
```java
public interface AgentMemoryService {
    AgentMemory loadMemoryForAgent(Long agentId);
    void saveMemoryForAgent(Long agentId, AgentMemory memory);
    void updateMemoryContent(Long agentId, String content);
    void clearMemory(Long agentId);
    List<AgentMemory> getMemoryHistory(Long agentId);
}
```

### Service Implementation Pattern (Copy from AiPostGeneratorServiceImpl.java)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMemoryServiceImpl implements AgentMemoryService {
    private final AgentMemoryMapper memoryMapper;
    
    @Override
    public AgentMemory loadMemoryForAgent(Long agentId) {
        // Implementation using QueryWrapper
    }
    
    // ... other methods
}
```

### XML Mapper Pattern (Copy from AgentMapper.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.zhemu.alterego.mapper.AgentMemoryMapper">

    <resultMap id="BaseResultMap" type="org.zhemu.alterego.model.entity.AgentMemory">
        <id column="id" jdbcType="BIGINT" property="id"/>
        <result column="agent_id" jdbcType="BIGINT" property="agentId"/>
        <result column="content" jdbcType="LONGVARCHAR" property="content"/>
        <result column="create_time" jdbcType="TIMESTAMP" property="createTime"/>
        <result column="update_time" jdbcType="TIMESTAMP" property="updateTime"/>
        <result column="is_delete" jdbcType="TINYINT" property="isDelete"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, agent_id, content, create_time, update_time, is_delete
    </sql>

</mapper>
```

---

## 💾 Database Table Schema

### Create Table SQL
```sql
CREATE TABLE IF NOT EXISTS `agent_memory`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `agent_id`    bigint       NOT NULL COMMENT 'Agent ID（外键）',
    `content`     longtext     DEFAULT NULL COMMENT '记忆内容（JSON格式）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Agent记忆表（存储Agent的会话历史和上下文）';
```

---

## 🔗 Dependency Injection Chain

```
AiPostGeneratorServiceImpl
  │
  ├─ dashScopeModel (existing)
  └─ agentMemoryService (NEW) ← Inject here
       │
       └─ agentMemoryMapper (AUTO injected by Spring)
            │
            └─ AgentMemory (Entity class)
```

### How to Add Dependency
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPostGeneratorServiceImpl implements AiPostGeneratorService {

    private final Model dashScopeModel;
    private final AgentMemoryService agentMemoryService;  // ← ADD THIS LINE
    
    @Override
    public AiPostGenerateResult generatePost(Agent agent, Species species) {
        // Load memory before processing
        AgentMemory agentMemory = agentMemoryService.loadMemoryForAgent(agent.getId());
        
        // Use memory in agent creation or pass it somehow
        ReActAgent aiAgent = ReActAgent.builder()
                .name("PostGenerator")
                .sysPrompt("...")
                .model(dashScopeModel)
                .toolkit(new Toolkit())
                .memory(convertToAgentScopeMemory(agentMemory))  // ← USE HERE
                .maxIters(3)
                .build();
        
        // After execution, save updated memory
        agentMemoryService.saveMemoryForAgent(agent.getId(), updatedMemory);
        
        return result;
    }
}
```

---

## ⚠️ Important Notes

1. **Naming Convention:** Always use snake_case for database tables, camelCase for Java
2. **Soft Delete:** Always use `is_delete` field pattern (already done in codebase)
3. **Timestamps:** Always use `create_time` and `update_time` with automatic updates
4. **Lombok:** Use @Data, @Builder, @RequiredArgsConstructor, @Slf4j
5. **MyBatis Plus:** All mappers extend `BaseMapper<T>` - no need for custom queries initially
6. **Charset:** Keep `utf8mb4` for all string fields to support emojis

---

## 📝 File Generation Order

1. **First:** `sql/add_agent_memory_table.sql` - Prepare database
2. **Second:** `AgentMemory.java` - Create entity
3. **Third:** `AgentMemoryMapper.java` + `AgentMemoryMapper.xml` - Create persistence layer
4. **Fourth:** `AgentMemoryService.java` + `AgentMemoryServiceImpl.java` - Create service layer
5. **Fifth:** Modify `AiPostGeneratorServiceImpl.java` - Integrate memory into application
6. **Finally:** Test everything

---

## 🧪 Testing Checklist

### Manual Testing
- [ ] Insert memory via service - verify in database
- [ ] Load memory via service - verify data correctness
- [ ] Update memory via service - verify updates persist
- [ ] Clear memory via service - verify soft delete works
- [ ] Generate post - verify memory loads and saves

### Unit Testing
- [ ] AgentMemoryService.loadMemoryForAgent()
- [ ] AgentMemoryService.saveMemoryForAgent()
- [ ] AgentMemoryService.updateMemoryContent()
- [ ] AgentMemoryService.clearMemory()
- [ ] AgentMemoryMapper CRUD operations

### Integration Testing
- [ ] Full generatePost flow with memory persistence
- [ ] Memory available in second post generation
- [ ] Multiple agents have separate memories

---

## 📚 Reference Documents

- `MEMORY_INTEGRATION_SEARCH_RESULTS.md` - Detailed findings
- `MEMORY_INTEGRATION_IMPACT_ANALYSIS.md` - Impact assessment
- `Product-Spec.md` - Original product requirements

---

## ✅ Completion Criteria

| Criterion | Status |
|-----------|--------|
| All 6 new files created | [ ] |
| 1 existing file modified | [ ] |
| Database migration applied | [ ] |
| All unit tests passing | [ ] |
| All integration tests passing | [ ] |
| Memory persists across post generations | [ ] |
| No breaking changes to existing APIs | [ ] |
| Code follows project conventions | [ ] |
| Documentation updated | [ ] |
| Ready for production deployment | [ ] |

---

## 🚀 Quick Start Command

After understanding the structure:
```bash
# In backend directory
cd F:/code/ai/project/AlterEgo/backend

# Build to verify no compilation errors
mvn clean compile

# Run tests
mvn test

# The actual implementation follows the checklist above
```

---

**Last Updated:** 2024-02-02  
**Search Completion:** ✅ Done  
**Ready for Implementation:** ✅ Yes  
**Estimated Implementation Time:** 2-3 hours  
**Complexity:** Low to Medium
