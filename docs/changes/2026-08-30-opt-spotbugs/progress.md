# progress：2026-08-30-opt-spotbugs

## 状态

- [x] 分析 + 设计（design.md）
- [x] 用户评审（含「严格空指针约束」方向调整）
- [x] 实现（pom.xml + spotbugs-exclude.xml）
- [x] 首轮 triage（修真 bug / 填 exclude）
- [x] 验证（mvn clean test 全绿，4 模块 104 测试）
- [x] 文档同步（static-rules-review.md / progress.md）

## 执行记录

### 配置（根 pom.xml）

- `spotbugs-maven-plugin 4.8.6.8`（自带 SpotBugs 4.8.6；4.9+ 运行需 Java 17，项目构建于 Java 11 → 锁 4.8.x 线）
- `check` 绑 `process-classes`：跑在 `mvn clean test` 生命周期内，无需新命令
- effort=Max + threshold=Low + excludeFilterFile=根 `spotbugs-exclude.xml`

### 首轮 triage 实证（38 → 67 → 0 项）

- **修复的真 bug / 契约错误**（严格空指针方向，用户拍板）：
  - 死代码：`DruidDdlParser.convertMySqlIndex`（UPM，重构遗留）删除
  - 未读字段：`TypeMapper.naming`（URF）删除字段与构造器参数
  - 根目录配置文件：`getParent()` 为 null → `PropertiesConfigLoader.load` 与 `cli.Main.execute` 判空抛明确异常（真实 NPE 路径）
  - 空值契约标注：`Literal` 构造器 value、`CompileUnit.getSourceFile/getLineMap/getPackageName`、`Method.getReturnType/getBody`、`Variable.getVariableKind`、`JavaTreeConverter.toModelModifiers` 返回、`convertConstraint` 返回、`primaryKey/idColumn` 返回、`findField/findMethod/parse` 返回、`AnnotationProcessor.process` comment、`DdlAnnotationHandler.parse` value、`PathResolver` module 参数、`Class/Variable/Method.setJavadoc` 等 → @Nullable
  - 消灭可空：`Docs.html` 3-arg 删 null 容忍（2-arg 传 Map.of()）、`toModelModifiers` 删 null 分支（javac getModifiers() 契约非 null）、`JavaTreeConverter(DocTrees)` 构造器化（消除共享状态竞态 + UWF）、`TypeParameter` final 字段 + 私有构造器、`DdlConfig.getRoot` requireNonNull、`TableContext.packageName`/`GenerationContext.artifactFqn` 显式判空 throw
  - 索引列不存在 → `findByMethod`/`QueryMethodFactory.findBy`/`selectXml`/`bridgeMethod` 判空 throw（DDL 数据不一致明确失败）
  - `StatementApplier` 7 处 contains+getTable 双查找 → getTable 判空单查找
  - `ImportManager.compare`/`TypeReference.getQualifiedName` 删除死判空（toString/getPath 契约非 null）
  - `Bridge` 内聚 getter（convert=true 时 converterFqn/convertMethod 判空 throw）
  - `AbstractJavaArtifactGenerator.signature` 缓存 getter 局部变量（spotbugs 不假设 getter 两次调用值不变）

- **误报/接受项 → spotbugs-exclude.xml**（每条带 Justification）：
  - EI_EXPOSE_REP/EI_EXPOSE_REP2 ×2 包（tree 可修改 AST；core 管线内部引用共享 + Meta 开放读写）
  - RCN lombok equals/hashCode（Identifier/Package/TypeReference，生成代码 vs checkerframework 字节码默认 @NonNull）
  - EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC（TypeReference 值类型独立相等设计）
  - RCN MapperXmlGenerator（insertXml 短路保护，spotbugs 跨方法误报）

### 工具联动记录

- 发现 **checkerframework 编译期把默认 @NonNull 写进字节码**（javap 实证），SpotBugs 读取后暴露「源码真相 vs 字节码真相」不一致——这是本次检出空指针问题的主因之一（另一主因是 `-Awarns` 关掉了强制力）
- `Objects.requireNonNull` 是 SpotBugs 雷区（参数被当 @Nonnull）→ 改用显式 if 判空 throw
- convertAlter 判空使圈复杂度 25（阈值 20）→ 类级 @SuppressWarnings("CyclomaticComplexity")，按 §5/§6 分发器依据，已记录 static-rules-review.md §3

### checkerframework error 级调整（用户拍板，随本变更）

- pom 移除 `-Awarns`：NullnessChecker 全量 error，强制修复或带理由抑制
- **初始化检查注解化（零抑制残留）**：
  - 真可空字段 → `@Nullable`：`DdlConfig.root`、`Class.pkg/extend`、`Method.returnType/body/receiverParameter/defaultValue`、`Variable.kind/nameExpr/initExpr`、`CompileUnit.pkg`
  - 构建后必有字段 → `@MonotonicNonNull` + 读取端判空 throw：`Class.name/kind/modifiers`、`Method.name/modifiers`、`Variable.name/type/modifiers`、`Column.Builder.name/sqlType`、`Index.Builder.name`、`GenerationContext.Builder.config/naming/typeMapper/annotationRegistry`；builder 构造器/build 判空 throw（fail-fast）
  - 顺带修复真实缺陷：`Class.Builder`/`Method.Builder` 构造器缺 modifiers 默认值 → 补 `new Modifiers()`（测试暴露）
  - 框架注入字段（不假设注入）：JUnit `@TempDir` → `@Nullable` + `tempDir()` getter 校验；`@BeforeEach` 字段 → `@Nullable` + `config()`/`generator()` getter；Maven `@Parameter` → `@Nullable` + `rootPath()` 兜底/`resolveDdl` 判空展开
- **KeyFor 局部化**：移除全局 `-AsuppressWarnings=keyfor`，改为 10 个报错类类级 `@SuppressWarnings("keyfor")` + WHY（与 Map 无关的 JDK 通配符误报；AnnotationRegistry.names 的 keySet 返回值为真实 KeyFor 类型，方法级抑制）
- JUnit 断言契约：`checker/junit-assertions.astub`（`@EnsuresNonNull("#1")`，-Astubs 引入），测试的 assertNotNull 后非 null 由 checkerframework 认可
- 测试代码：getColumn/getTable/getIndex 后直接使用 → assertNotNull；e.getMessage() → 判空；EndToEndTest.add 的 pkg/suffix 参数 @Nullable（XML 产物传 null）
