# Tag System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace JSON tag storage on posts with normalized tag and post_tag tables, while keeping AI-generated tags.

**Architecture:** On post creation, normalize AI tags, upsert into tag table, and write post_tag relations. On read, join post_tag + tag to populate PostVO.tags. Statistics are stored in tag.post_count.

**Tech Stack:** Java 21, Spring Boot 3.5.10, MyBatis-Plus, MySQL, Redis.

---

### Task 1: Add tag domain model and services

**Files:**
- Create: src/main/java/org/zhemu/alterego/model/entity/Tag.java
- Create: src/main/java/org/zhemu/alterego/model/entity/PostTag.java
- Create: src/main/java/org/zhemu/alterego/mapper/TagMapper.java
- Create: src/main/java/org/zhemu/alterego/mapper/PostTagMapper.java
- Create: src/main/java/org/zhemu/alterego/service/TagService.java
- Create: src/main/java/org/zhemu/alterego/service/PostTagService.java
- Create: src/main/java/org/zhemu/alterego/service/impl/TagServiceImpl.java
- Create: src/main/java/org/zhemu/alterego/service/impl/PostTagServiceImpl.java

**Step 1: Write the failing test**

Create test skeleton to assert tag normalization and get-or-create behavior.

Test file: src/test/java/org/zhemu/alterego/service/TagServiceTest.java

```java
@Test
void normalizeTag_shouldLowercaseTrimCollapseSpaces() {
    assertEquals("hello world", tagService.normalize("  Hello   World "));
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TagServiceTest#normalizeTag_shouldLowercaseTrimCollapseSpaces`
Expected: FAIL (TagService not found)

**Step 3: Write minimal implementation**

- Add Tag and PostTag entities with fields:
  - Tag: id, name, nameNorm, postCount, likeCount, createTime, updateTime
  - PostTag: id, postId, tagId, createTime
- Add mappers extending BaseMapper.
- Add services and impls extending ServiceImpl.
- Add TagService.normalize(String raw) and TagService.getOrCreateTag(String raw).
  - normalize: trim, remove leading '#', lowercase, collapse whitespace to single space.
  - getOrCreateTag: select by nameNorm, insert if missing; on duplicate key, reselect.

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TagServiceTest#normalizeTag_shouldLowercaseTrimCollapseSpaces`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/zhemu/alterego/model/entity/Tag.java \
        src/main/java/org/zhemu/alterego/model/entity/PostTag.java \
        src/main/java/org/zhemu/alterego/mapper/TagMapper.java \
        src/main/java/org/zhemu/alterego/mapper/PostTagMapper.java \
        src/main/java/org/zhemu/alterego/service/TagService.java \
        src/main/java/org/zhemu/alterego/service/PostTagService.java \
        src/main/java/org/zhemu/alterego/service/impl/TagServiceImpl.java \
        src/main/java/org/zhemu/alterego/service/impl/PostTagServiceImpl.java \
        src/test/java/org/zhemu/alterego/service/TagServiceTest.java

git commit -m "feat(tag): add tag and post_tag domain"
```

---

### Task 2: Remove JSON tag field from Post entity and VO conversion

**Files:**
- Modify: src/main/java/org/zhemu/alterego/model/entity/Post.java
- Modify: src/main/java/org/zhemu/alterego/model/vo/PostVO.java

**Step 1: Write the failing test**

Update existing PostVO conversion test or add new one to ensure tags are not parsed from Post.

```java
@Test
void postVo_shouldNotParseTagsFromPostEntity() {
    Post post = new Post();
    post.setId(1L);
    PostVO vo = PostVO.objToVo(post);
    assertNull(vo.getTags());
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=PostVOTest#postVo_shouldNotParseTagsFromPostEntity`
Expected: FAIL (objToVo parses JSON)

**Step 3: Write minimal implementation**

- Remove Post.tags field.
- Update PostVO.objToVo to not parse JSON tags.

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=PostVOTest#postVo_shouldNotParseTagsFromPostEntity`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/zhemu/alterego/model/entity/Post.java \
        src/main/java/org/zhemu/alterego/model/vo/PostVO.java \
        src/test/java/org/zhemu/alterego/model/vo/PostVOTest.java

git commit -m "refactor(post): remove json tags field"
```

---

### Task 3: Persist tags on post creation

**Files:**
- Modify: src/main/java/org/zhemu/alterego/service/impl/PostServiceImpl.java
- Modify: src/main/java/org/zhemu/alterego/service/PostService.java (if signatures change)

**Step 1: Write the failing test**

Create test to verify post creation writes post_tag relations.

```java
@Test
void aiGeneratePost_shouldPersistTags() {
    PostVO vo = postService.aiGeneratePost(request, userId);
    List<PostTag> relations = postTagService.lambdaQuery().eq(PostTag::getPostId, vo.getId()).list();
    assertFalse(relations.isEmpty());
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=PostServiceTest#aiGeneratePost_shouldPersistTags`
Expected: FAIL (no post_tag rows)

**Step 3: Write minimal implementation**

- Inject TagService and PostTagService into PostServiceImpl.
- After saving Post, normalize AI tags and:
  - For each tag: tagService.getOrCreateTag
  - Insert post_tag
  - Increment tag.post_count by 1 (atomic update)
- Ignore empty or null tags.

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=PostServiceTest#aiGeneratePost_shouldPersistTags`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/zhemu/alterego/service/impl/PostServiceImpl.java \
        src/main/java/org/zhemu/alterego/service/PostService.java \
        src/test/java/org/zhemu/alterego/service/PostServiceTest.java

git commit -m "feat(post): persist tags to post_tag"
```

---

### Task 4: Populate tags in PostVO reads

**Files:**
- Modify: src/main/java/org/zhemu/alterego/service/impl/PostServiceImpl.java

**Step 1: Write the failing test**

```java
@Test
void listPostByPage_shouldIncludeTags() {
    Page<PostVO> page = postService.listPostByPage(query);
    assertNotNull(page.getRecords().get(0).getTags());
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=PostServiceTest#listPostByPage_shouldIncludeTags`
Expected: FAIL (tags null)

**Step 3: Write minimal implementation**

- Query post_tag by postIds, join tags by tagIds.
- Build Map<postId, List<String>> and set on PostVO.
- Apply in both listPostByPage and getPostVOById.

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=PostServiceTest#listPostByPage_shouldIncludeTags`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/zhemu/alterego/service/impl/PostServiceImpl.java \
        src/test/java/org/zhemu/alterego/service/PostServiceTest.java

git commit -m "feat(post): read tags from post_tag"
```

---

### Task 5: Update SQL schema docs

**Files:**
- Modify: sql/create_table.sql

**Step 1: Update SQL**

- Remove post.tags
- Add tag and post_tag tables

**Step 2: Commit**

```bash
git add sql/create_table.sql

git commit -m "docs(sql): update tag schema"
```

---

## Testing Notes

- Unit tests above use existing Spring Boot test setup.
- If full integration tests are heavy, run targeted tests for TagService and PostService.

---

Plan complete and saved to `docs/plans/2026-02-04-tag-system.md`. Two execution options:

1. Subagent-Driven (this session) - I dispatch fresh subagent per task, review between tasks, fast iteration
2. Parallel Session (separate) - Open new session with executing-plans, batch execution with checkpoints

Which approach?
