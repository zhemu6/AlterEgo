# Memory Integration - Codebase Search Results

## Overview
This document summarizes the findings from searching the AlterEgo backend codebase for Memory interface and InMemoryMemory class usage, as well as existing memory-related database structures.

---

## 1. Current Memory Usage

### Files Using Memory Classes
**Location:** `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java`

**Current Implementation:**
```java
import io.agentscope.core.memory.InMemoryMemory;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPostGeneratorServiceImpl implements AiPostGeneratorService {
    
    @Override
    public AiPostGenerateResult generatePost(Agent agent, Species species) {
        // Line 66: Creates in-memory agent
        ReActAgent aiAgent = ReActAgent.builder()
                .name("PostGenerator")
                .sysPrompt("你是一个擅长角色扮演的 AI，能够完美代入各种角色的性格和说话方式。")
                .model(dashScopeModel)
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())  // <-- In-memory memory used here
                .maxIters(3)
                .build();
        // ... rest of implementation
    }
}
```

**Impact:**
- Uses `InMemoryMemory` from `io.agentscope.core.memory` package
- This is the ONLY place where memory is currently initialized in the codebase
- The memory is created fresh for each post generation, so it doesn't persist

---

## 2. Existing Database Schema

### Current Memory-Related Tables
**NO dedicated memory table exists yet.**

### Existing Entity Classes and Tables

1. **Agent Entity** (`agent` table)
   - Fields: id, user_id, species_id, agent_name, personality, energy, post_count, comment_count, like_count, dislike_count, last_energy_reset, create_time, update_time, is_delete
   - No memory-related fields
   - File: `src/main/java/org/zhemu/alterego/model/entity/Agent.java`

2. **Post Entity** (`post` table)
   - Fields: id, agent_id, post_type, title, content, tags, like_count, dislike_count, comment_count, create_time, update_time, is_delete
   - No memory-related fields
   - File: `src/main/java/org/zhemu/alterego/model/entity/Post.java`

3. **Comment Entity** (`comment` table)
   - Fields: id, post_id, agent_id, parent_comment_id, root_comment_id, content, reply_count, create_time, update_time, is_delete
   - No memory-related fields

4. **Species Entity** (`species` table)
   - Fields: id, name, icon, description, create_time, update_time, is_delete

5. **AgentVoteRecord Entity** (`agent_vote_record` table)

6. **PostLike Entity** (`post_like` table)

7. **PkVoteOption Entity** (`pk_vote_option` table)

8. **SysUser Entity** (`sys_user` table)

---

## 3. Project Architecture

### Directory Structure
```
src/main/java/org/zhemu/alterego/
├── annotation/         (Custom annotations)
├── aop/               (Aspect-Oriented Programming)
├── common/            (Common utilities)
├── config/            (Spring configuration)
├── constant/          (Constants)
├── controller/        (REST controllers)
├── exception/         (Exception handling)
├── interceptor/       (HTTP interceptors)
├── manager/           (Business managers)
├── mapper/            (MyBatis+ data access)
│   ├── AgentMapper.java
│   ├── PostMapper.java
│   ├── CommentMapper.java
│   ├── AgentVoteRecordMapper.java
│   ├── PostLikeMapper.java
│   ├── SpeciesMapper.java
│   ├── PkVoteOptionMapper.java
│   └── SysUserMapper.java
├── model/
│   ├── entity/        (JPA entities - Database tables)
│   │   ├── Agent.java
│   │   ├── Post.java
│   │   ├── Comment.java
│   │   ├── Species.java
│   │   ├── AgentVoteRecord.java
│   │   ├── PostLike.java
│   │   ├── PkVoteOption.java
│   │   └── SysUser.java
│   ├── dto/          (Data Transfer Objects - API requests/responses)
│   ├── vo/           (Value Objects - View models)
│   └── enums/        (Enumerations)
├── mq/               (Message Queue)
├── service/          (Business logic)
│   └── impl/
│       └── AiPostGeneratorServiceImpl.java  <-- Uses InMemoryMemory
├── util/             (Utility classes)
└── BackendApplication.java
```

### Technology Stack
- **Framework:** Spring Boot
- **ORM:** MyBatis Plus
- **Database:** MySQL (utf8mb4)
- **AI Agent Framework:** AgentScope (from Ant Group)
- **Build:** Maven (based on pom.xml presence)
- **Serialization:** Lombok

---

## 4. Database Layer Integration

### MyBatis Plus Configuration
- Uses `BaseMapper<T>` interface for all entities
- XML mapper files located in: `src/main/resources/mapper/`
- Example mapper: `src/main/resources/mapper/AgentMapper.xml`

### Sample Mapper Structure
```xml
<mapper namespace="org.zhemu.alterego.mapper.AgentMapper">
    <resultMap id="BaseResultMap" type="org.zhemu.alterego.model.entity.Agent">
        <!-- Column mappings -->
    </resultMap>
    <sql id="Base_Column_List">
        <!-- Column definitions -->
    </sql>
</mapper>
```

---

## 5. Recommendations for Memory Integration

### Changes Needed

#### 5.1 Create AgentMemory Entity
- **Location:** `src/main/java/org/zhemu/alterego/model/entity/AgentMemory.java`
- **Purpose:** Store agent conversation and interaction history
- **Fields to consider:**
  - id: Long (primary key)
  - agent_id: Long (foreign key to agent)
  - memory_type: String (conversation, interaction, context, etc.)
  - content: Text (JSON or serialized memory content)
  - message_count: Integer (track conversation length)
  - last_updated: LocalDateTime
  - create_time: LocalDateTime
  - update_time: LocalDateTime
  - is_delete: Integer (soft delete flag)

#### 5.2 Create Database Migration
- **Location:** `sql/add_agent_memory_table.sql`
- **SQL Command:** Create `agent_memory` table with proper indexes

#### 5.3 Create Mapper Interface
- **Location:** `src/main/java/org/zhemu/alterego/mapper/AgentMemoryMapper.java`
- **Extends:** `BaseMapper<AgentMemory>`

#### 5.4 Create Mapper XML
- **Location:** `src/main/resources/mapper/AgentMemoryMapper.xml`
- **Purpose:** Define result maps and custom queries

#### 5.5 Create Service Layer
- **Location:** `src/main/java/org/zhemu/alterego/service/AgentMemoryService.java`
- **Location:** `src/main/java/org/zhemu/alterego/service/impl/AgentMemoryServiceImpl.java`
- **Methods to implement:**
  - loadMemoryForAgent(Long agentId): AgentMemory
  - saveMemoryForAgent(Long agentId, AgentMemory memory): void
  - updateMemoryContent(Long agentId, String content): void
  - clearMemory(Long agentId): void
  - getMemoryHistory(Long agentId): List<AgentMemory>

#### 5.6 Modify AiPostGeneratorServiceImpl
- **Location:** `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java`
- **Changes:**
  - Inject `AgentMemoryService`
  - Load memory before creating agent
  - Save memory after agent execution
  - Replace `new InMemoryMemory()` with loaded memory
  - Add memory persistence logic

#### 5.7 Update Agent Entity (Optional)
- Add memory-related fields if needed (memory summary, memory size, etc.)

---

## 6. File Changes Summary

### Files to Create
1. `src/main/java/org/zhemu/alterego/model/entity/AgentMemory.java` - New entity
2. `src/main/java/org/zhemu/alterego/mapper/AgentMemoryMapper.java` - New mapper interface
3. `src/main/resources/mapper/AgentMemoryMapper.xml` - New MyBatis XML
4. `src/main/java/org/zhemu/alterego/service/AgentMemoryService.java` - New service interface
5. `src/main/java/org/zhemu/alterego/service/impl/AgentMemoryServiceImpl.java` - New service implementation
6. `sql/add_agent_memory_table.sql` - New database migration

### Files to Modify
1. `src/main/java/org/zhemu/alterego/service/impl/AiPostGeneratorServiceImpl.java` - Update memory usage
2. `sql/create_table.sql` - Add memory table creation script (optional, use separate migration)

### Files NOT Requiring Changes
- All other entity, mapper, and service files remain unchanged
- Configuration files remain unchanged
- Controller files remain unchanged (unless new endpoints needed)

---

## 7. Integration Points

### Where Memory is Used
1. **AiPostGeneratorServiceImpl**
   - Method: `generatePost(Agent agent, Species species)`
   - Current: Uses `new InMemoryMemory()`
   - Target: Load from database or persistent storage

### Where to Add Memory Persistence
1. Before calling agent: Load agent memory
2. After agent responds: Save updated memory
3. Optionally: Track memory size and implement cleanup strategies

---

## 8. Database Configuration

### Existing Configuration Files
- `src/main/resources/application.yml` - Main configuration
- `src/main/resources/application-dev.yml` - Development environment
- `src/main/resources/application-prod.yml` - Production environment

### Current MySQL Settings
- Database: `alterego`
- Character Set: `utf8mb4`
- Collation: `utf8mb4_unicode_ci`
- Engine: InnoDB

---

## Conclusion

The codebase has a clear, well-organized structure with:
- ✅ Proper entity-mapper-service layering
- ✅ Existing database schema with proper naming conventions
- ✅ MyBatis Plus integration
- ✅ Single point of memory usage (AiPostGeneratorServiceImpl)

**Action Items:**
1. Create AgentMemory entity and database table
2. Create AgentMemoryMapper and corresponding XML
3. Create AgentMemoryService layer
4. Update AiPostGeneratorServiceImpl to use persistent memory
5. Add database migration script
6. Test memory persistence across agent interactions
