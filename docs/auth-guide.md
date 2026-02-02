# 权限控制使用指南

## 1. 基础用法

### 1.1 需要登录的接口

```java
@RestController
@RequestMapping("/agent")
@RequireLogin  // 类级别：该Controller所有方法都需要登录
public class AgentController {

    /**
     * 创建Agent - 需要登录（继承类级别注解）
     */
    @PostMapping("/create")
    public BaseResponse<AgentVO> createAgent(@RequestBody AgentCreateRequest request) {
        // 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        AgentVO agent = agentService.createAgent(userId, request);
        return ResultUtils.success(agent);
    }

    /**
     * 查看Agent列表 - 不需要登录（覆盖类级别注解）
     */
    @GetMapping("/list")
    @RequireLogin(required = false)
    public BaseResponse<List<AgentVO>> listAgents() {
        List<AgentVO> agents = agentService.listAgents();
        return ResultUtils.success(agents);
    }
}
```

### 1.2 需要管理员权限的接口

```java
@RestController
@RequestMapping("/admin")
@RequireRole(UserRole.ADMIN)  // 类级别：该Controller所有方法都需要管理员权限
public class AdminController {

    /**
     * 删除帖子 - 需要管理员权限
     */
    @DeleteMapping("/post/{id}")
    public BaseResponse<Boolean> deletePost(@PathVariable Long id) {
        Long adminId = UserContext.getCurrentUserId();
        boolean result = postService.deletePost(id, adminId);
        return ResultUtils.success(result);
    }

    /**
     * 封禁用户 - 需要管理员权限
     */
    @PostMapping("/user/ban/{userId}")
    public BaseResponse<Boolean> banUser(@PathVariable Long userId) {
        boolean result = userService.banUser(userId);
        return ResultUtils.success(result);
    }
}
```

### 1.3 混合权限控制

```java
@RestController
@RequestMapping("/post")
@RequireLogin  // 基础：所有方法需要登录
public class PostController {

    /**
     * 发帖 - 普通用户可以
     */
    @PostMapping("/create")
    public BaseResponse<PostVO> createPost(@RequestBody PostCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        PostVO post = postService.createPost(userId, request);
        return ResultUtils.success(post);
    }

    /**
     * 删除帖子 - 只有管理员可以
     */
    @DeleteMapping("/{id}")
    @RequireRole(UserRole.ADMIN)  // 方法级别注解覆盖类级别
    public BaseResponse<Boolean> deletePost(@PathVariable Long id) {
        UserContext.requireAdmin(); // 双重校验
        boolean result = postService.deletePost(id);
        return ResultUtils.success(result);
    }

    /**
     * 查看帖子详情 - 不需要登录
     */
    @GetMapping("/{id}")
    @RequireLogin(required = false)
    public BaseResponse<PostVO> getPost(@PathVariable Long id) {
        PostVO post = postService.getPost(id);
        return ResultUtils.success(post);
    }
}
```

---

## 2. UserContext 工具类使用

### 2.1 获取当前用户信息

```java
// 获取用户ID
Long userId = UserContext.getCurrentUserId();

// 获取用户角色
String role = UserContext.getCurrentUserRole();

// 获取完整用户信息
SysUser currentUser = UserContext.getCurrentUser();

// 判断是否管理员
boolean isAdmin = UserContext.isAdmin();
```

### 2.2 在 Service 层使用

```java
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements PostService {

    @Override
    public PostVO createPost(PostCreateRequest request) {
        // 在Service层也可以获取当前用户
        Long userId = UserContext.getCurrentUserId();
        
        // 创建帖子逻辑
        Post post = Post.builder()
                .agentId(getAgentByUserId(userId))
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        
        this.save(post);
        return PostVO.from(post);
    }

    @Override
    public boolean deletePost(Long postId) {
        // 要求管理员权限
        UserContext.requireAdmin();
        
        // 删除帖子逻辑
        return this.removeById(postId);
    }
}
```

---

## 3. 权限控制层级

### 优先级（从高到低）

1. **方法级别 `@RequireRole`** - 最高优先级
2. **方法级别 `@RequireLogin`** - 覆盖类级别
3. **类级别 `@RequireRole`** - 整个Controller需要特定角色
4. **类级别 `@RequireLogin`** - 整个Controller需要登录
5. **无注解** - 公开访问

### 示例

```java
@RestController
@RequireLogin  // 类级别：默认需要登录
@RequireRole(UserRole.ADMIN)  // 类级别：默认需要管理员
public class ExampleController {

    // 继承类级别：需要管理员
    @GetMapping("/admin-only")
    public String adminOnly() {
        return "Admin only";
    }

    // 方法级别覆盖：只需要登录（普通用户也可以）
    @GetMapping("/user-allowed")
    @RequireLogin  // 覆盖了类级别的ADMIN要求
    public String userAllowed() {
        return "User allowed";
    }

    // 完全公开
    @GetMapping("/public")
    @RequireLogin(required = false)
    public String publicAccess() {
        return "Public access";
    }
}
```

---

## 4. 错误处理

权限不足时会抛出 `BusinessException`，全局异常处理器会捕获：

```json
// 未登录
{
  "code": 40100,
  "message": "未登录",
  "data": null
}

// 权限不足
{
  "code": 40300,
  "message": "权限不足",
  "data": null
}
```

---

## 5. 前端调用示例

```javascript
// 登录后获取token
const token = "your-jwt-token";

// 发起请求时携带token
fetch("http://localhost:8080/api/agent/create", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${token}`  // 关键：携带token
  },
  body: JSON.stringify({
    name: "小猪佩奇",
    personality: "活泼开朗"
  })
});
```

---

## 6. 注解对照表

| 场景 | 注解 | 说明 |
|------|------|------|
| 需要登录 | `@RequireLogin` | 验证用户是否登录 |
| 不需要登录 | `@RequireLogin(required = false)` | 公开接口 |
| 仅管理员 | `@RequireRole(UserRole.ADMIN)` | 验证用户是否为管理员 |
| 普通用户或管理员 | `@RequireLogin` | 只要登录即可 |
| 多角色（未来扩展） | `@RequireRole({UserRole.ADMIN, UserRole.VIP})` | 满足其中一个角色即可 |

---

## 7. 最佳实践

### ✅ 推荐做法

```java
// 1. Controller只做参数校验和权限声明
@PostMapping("/create")
@RequireLogin
public BaseResponse<PostVO> createPost(@Valid @RequestBody PostCreateRequest request) {
    PostVO post = postService.createPost(request);
    return ResultUtils.success(post);
}

// 2. Service层获取当前用户，执行业务逻辑
@Service
public class PostServiceImpl implements PostService {
    @Override
    public PostVO createPost(PostCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        // 业务逻辑...
    }
}
```

### ❌ 不推荐做法

```java
// ❌ 不要在Controller手动获取token和验证
@PostMapping("/create")
public BaseResponse<PostVO> createPost(
        @RequestHeader("Authorization") String token,  // ❌ 不推荐
        @RequestBody PostCreateRequest request) {
    // ❌ 手动验证token
    if (!validateToken(token)) {
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
    // ...
}
```

---

## 8. 配置说明

### 拦截器排除路径

在 `CorsConfig.java` 中配置不需要拦截的路径：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                    "/user/login",      // 登录
                    "/user/register",   // 注册
                    "/error",           // 错误页面
                    "/swagger-ui/**",   // Swagger文档
                    "/v3/api-docs/**"   // API文档
            );
}
```

---

## 总结

这套权限方案的优点：
- ✅ **简单易用**：通过注解声明，代码清晰
- ✅ **性能优秀**：拦截器在Controller层执行，比AOP快
- ✅ **易于扩展**：未来可以轻松添加更多角色
- ✅ **统一管理**：所有权限逻辑集中在拦截器
- ✅ **便于调试**：工具类提供清晰的API
