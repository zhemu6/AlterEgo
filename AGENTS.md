# AlterEgo backend — AGENTS.md (for coding agents)

> 目标：让代理在本仓库里能“按现状写对代码”。本项目是 Java 21 + Spring Boot 3.5.10 + MyBatis-Plus + Redis + MySQL。

## 0) Repo facts / 约束
- Base package: `org.zhemu.alterego`
- Context path: `/api`（见 `src/main/resources/application.yml`）
- Port: `8124`
- JSON：`Long` 序列化为 `String`（见 `config/JsonConfig.java`）
- MyBatis-Plus：开启 `map-underscore-to-camel-case`，逻辑删除字段 `isDelete`（见 `application.yml`）

## 1) Commands（优先用 Maven Wrapper）

### Build / Run
```bash
./mvnw clean package
./mvnw clean package -DskipTests

./mvnw spring-boot:run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Test
```bash
# 全量测试
./mvnw test

# 只跑一个测试类（Surefire）
./mvnw test -Dtest=BackendApplicationTests

# 只跑单个测试方法
./mvnw test -Dtest=BackendApplicationTests#contextLoads

# 只跑某个包/命名模式（可选）
./mvnw test -Dtest=*Service*Test

# 常用验证（比 package 更“严格”）
./mvnw verify
```

### Lint / Format
- 仓库当前 `pom.xml` 未配置 Checkstyle/Spotless/PMD 等“强制格式化”插件。
- 结论：以 IDE（IntelliJ）默认 Java 格式化 + 本文风格约定为准；提交前至少确保 `./mvnw test` 通过。

## 2) Code structure（常见目录）
```
src/main/java/org/zhemu/alterego/
  controller/  service/  service/impl/  mapper/
  model/{dto,entity,enums,vo}/
  config/  interceptor/  annotation/  exception/  common/  util/  mq/
src/test/java/org/zhemu/alterego/
```

## 3) Coding conventions（照现有代码写）

### 3.1 Imports
- 不强制“分组 + 空行”，但建议：Java 标准库 / 三方库 / 项目内 3 组。
- 避免通配符导入（`import xxx.*`），测试断言除外。

### 3.2 Formatting
- 4 空格缩进，`{` 不换行（Spring/Java 常规风格）。
- 行宽不硬限制，但日志/链式调用尽量可读。

### 3.3 Naming
- Package：全小写（`org.zhemu.alterego...`）
- Controller：`*Controller`
- Service 接口：`*Service`；实现：`*ServiceImpl`
- Mapper：`*Mapper`（MyBatis-Plus `BaseMapper`）
- DTO：`*Request` / `*Response`
- VO：`*VO`
- Enum：`*Enum`，值用 `String/Integer` 与 DB/JSON 对齐

### 3.4 Types / nullability
- ID 使用 `Long`（允许为空；且会序列化为字符串）。
- 时间使用 `java.time.LocalDateTime`。
- 比较包装类型时用 `Objects.equals(a, b)`，避免 NPE/装箱陷阱。

### 3.4.1 DTO/VO 与序列化
- 外部入参：优先 DTO（`model/dto/**`），不要直接用 Entity 接收。
- 外部出参：优先 VO（`model/vo/**`），不要把敏感字段（如 `userPassword`）暴露给前端。
- `Long` 在 JSON 中会序列化成 `String`（`config/JsonConfig.java`）；前端字段类型要按字符串处理。

### 3.5 Dependency injection
- 统一使用构造器注入：`@RequiredArgsConstructor` + `final` 字段。
- 禁止/避免字段注入 `@Autowired`（除非历史代码无法改）。

### 3.6 Controllers
- 返回统一响应包装：`BaseResponse<T>` + `ResultUtils.success/error`。
- 参数校验：优先使用 `jakarta.validation` 注解（如 `@NotBlank`, `@Email`）配合 `@Validated`。
- 不在 Controller 里写复杂业务；把逻辑放到 Service。

### 3.6.1 HTTP 约定
- API 前缀是 `/api`（由 `server.servlet.context-path` 配置），Controller 上一般只写业务路径（如 `/user/**`）。
- 鉴权：使用 `Authorization: Bearer <token>`。

### 3.7 Error handling（按现有体系）
- 业务错误：抛 `BusinessException`（携带 `ErrorCode`）。
- 快捷抛错：用 `ThrowUtils.throwIf(condition, errorCode, message)`。
- 全局异常处理：`exception/GlobalExceptionHandler` 已接管，并对 SSE（`text/event-stream` 或特定 URI）做特殊输出。
- 新增错误码：在 `exception/ErrorCode` 增加枚举值（例如已有 `TOO_MANY_REQUEST(42900, ...)`）。

### 3.7.1 参数校验异常
- Bean Validation 失败会触发 `MethodArgumentNotValidException`，由 `GlobalExceptionHandler` 统一转为 `PARAMS_ERROR`。
- DTO 上的 `message` 会作为返回提示；保持提示简洁、面向用户。

### 3.8 Logging
- 用 Lombok `@Slf4j`；使用参数化日志：`log.info("x: {}", x)`。
- 禁止记录敏感信息：密码、验证码、token、密钥。

### 3.8.1 日志建议
- 正常业务流程：`info`；可恢复异常/权限不足：`warn`；不可恢复异常：`error` + 堆栈。
- 避免在高频接口里打过多 `info`，必要时用 `debug`。

### 3.9 MyBatis-Plus
- Entity：`@TableName` + `@TableLogic isDelete`。
- 查询：优先 `LambdaQueryWrapper`（类型安全）。
- Service：多数实现继承 `ServiceImpl<Mapper, Entity>`。

### 3.9.1 数据层约定
- 逻辑删除字段统一为 `isDelete`（0 未删 / 1 已删），不要手写物理删除逻辑。
- 复杂查询优先封装到 Service（或 Mapper 自定义方法），避免 Controller 拼装 SQL。

### 3.10 Auth / Interceptors（现状提醒）
- 登录态：`Authorization: Bearer <token>`。
- Token 映射：Redis `RedisConstants.USER_LOGIN_TOKEN + token -> userId`。
- 用户信息缓存：Redis `RedisConstants.USER_INFO_CACHE + userId -> SysUser JSON`（密码字段应置空）。
- 拦截器注册：`config/CorsConfig` 中 `RateLimitInterceptor` order(0) 在前，`AuthInterceptor` order(1)。

### 3.10.1 安全细节
- 不要把“未知/非法角色”当作默认角色；角色解析失败应拒绝访问（见 `UserRoleEnum.fromValue` 的用法）。
- 任何鉴权/限流相关变更，都要补充/更新测试（`src/test/java/org/zhemu/alterego/**`）。

## 4) Testing guidelines
- 测试框架：JUnit 5 + `spring-boot-starter-test`。
- 优先写“可重复运行”的测试（不要依赖真实外部服务；必要时用 Mockito 或嵌入式/测试替身）。
- 单测命名：`*Test` 结尾，路径按包结构放到 `src/test/java`。

## 4.1 常用本地验证清单（提交前）
```bash
./mvnw test
# 或者更严格
./mvnw verify
```

## 5) Cursor / Copilot rules
- 本仓库未发现：`.cursorrules`、`.cursor/rules/**`、`.github/copilot-instructions.md`。
- 如未来新增这些规则：本文件应同步摘要其强制约束（尤其是：提交规范、禁用操作、格式化/测试要求）。
