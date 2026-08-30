# 开发进度台账

> 自动执行记录（用户睡眠期间）。每阶段：worker 交付 → 复跑 validate+test → review diff → 记录。

## 阶段状态

| 阶段 | 内容 | 状态 | 验证 |
|---|---|---|---|
| M0.1 | 模块化 + tree 迁移 + 质量修正 | ✅ 收口 | validate+test 全绿（14/14） |
| M0b | 保真层四修 + round-trip 断言测试 | ✅ 收口 | validate+test 全绿（16/16）；Demo 字节全等，复杂文件语义等价+幂等 |
| M0.7 余项 | CodePrinter 重写 / U→Names / 死代码 / Expr·Block 助手 | ✅ 收口 | validate+test 全绿（21/21） |
| M1a | core 模块：SchemaModel/Druid 解析/语句应用/注解体系 | ✅ 收口 | validate+test 全绿（27/27）；Druid 怪癖清单已记录 |
| M1b | config/命名/类型映射/文件 IO | ✅ 收口 | validate+test 全绿（56/56）；修复：保留字含 SQL 保留字、方法名补 By、config 顺序保持 |
| M2 | 生成核心：SPI/基类 reconcile/拦截器/两阶段/编排 | ✅ 收口 | validate+test 全绿（64/64）；生命周期测试通过（create→alter→drop→rename→用户代码保留）；修复 tree 三处 addAnnotation 丢失注解 bug + Modifiers 空集崩溃 + 转换器 modifiers 模型化 |
| M3 | 内置生成器 ×8 + 端到端验收 | ✅ 收口 | validate+test 全绿（63/63）；EndToEndTest 覆盖 @type/@as/@ignore/索引/拦截器全链路 |
| M4 | CLI/报告/README/收尾 | ✅ 收口 | CLI 冒烟验证：create/幂等/dry-run/alter/用户代码保留/drop 全部通过 |
| M3 | 内置生成器 ×7 + golden | ⬜ | |
| M4 | CLI/报告/--sync/文档/端到端验收 | ⬜ | |

## 关键决策记录（执行中拍板）

- **2026-08：移除 OpenSpec，采用轻量工作流**。删除了 openspec 残留（含 add-ddl-codegen-framework 提案）；新工作流见 AGENTS.md「开发工作流」：先分析设计后实现，决策记入本节，复杂设计写 docs/changes/（模板 TEMPLATE.md）；顶层架构与数据流常驻 AGENTS.md「项目架构」节，随变更同步
- ClassFanOutComplexity：阈值 20 保留；分发器类针对性 @SuppressWarnings + 实证注释（见 docs/static-rules-review.md §5/§6）
- **2026-08-30：引入 SpotBugs（字节码级静态检查）**。锁 spotbugs-maven-plugin 4.8.6.8（Java 11 运行环境，4.9+ 需 Java 17）；check 绑 process-classes（沿用 `mvn clean test` 验证命令）；effort=Max + threshold=Low 全量检出，误报/接受项进 spotbugs-exclude.xml（每条带 Justification）。首轮实证：checkerframework -Awarns 存量警告 + 字节码默认注解真相不一致是检出主力，附带死代码/未读字段/equals 暴露等 checkerframework 不覆盖类别。详情见 docs/changes/2026-08-30-opt-spotbugs/design.md
- **Expr/Block 助手简化**（M0c 并入 M2）：助手为纯字符串组合，不做 import 魔法；方法体引用类型的 import 由生成器显式 addImport（避免状态化 import-sink API，更清晰）。M0c 拆入 M2（对着真实生成上下文构建，避免空想 API）。
- 其余开放问题按 docs/design.md §17 默认值

## 阶段详情

- **M0.1** 模块化 + tree 迁移：14/14 → M0b 保真层：16/16 → M0.7 质量（CodePrinter 四件套/Names/Expr·Block）：21/21
- **M1a** SchemaModel/Druid/注解：27/27（Druid 怪癖清单见 worker 报告，已固化到代码注释）
- **M1b** config/命名/类型/IO：56/56（保留字含 SQL 保留字、方法名补 By、config 顺序保持）
- **M2** 生成核心（reconcile/拦截器/编排）：64/64（生命周期测试：create→alter→drop→rename→用户代码保留）
- **M3** 内置生成器 ×8 + EndToEndTest：63/63（@type/@as/@ignore/索引拆分/拦截器全链路）
- **M4** CLI + README：CLI 冒烟全通过（create/幂等/dry-run/alter/用户代码保留/drop）

## PIT 变异测试（2026-08 引入）

运行：`JAVA_HOME=... mvn org.pitest:pitest-maven:mutationCoverage -pl ddl-codegen-core -Dmaven.compiler-plugin.debug=true`（约 1 分钟）

- **首轮 817 变异，杀死 604，73%**；补测后 74%
- **实际价值：是。** PIT 抓到的真缺口（行覆盖率看不到）：
  1. 方法级 reconcile（reconcileMethods）从未被测试——TestGenerator 只生成字段 → 已补（ReconcileLifecycleTest 增加 describe() 方法 + 断言）
  2. merge 路径的包/import 保留未断言 → 已补（package com.test 断言 + 幂等断言）
  3. ALTER ADD INDEX 的注解处理路径未测试 → 已补（DruidDdlParserTest.addIndexWithIgnoreAnnotation）
- **剩余存活变异分类**：
  - MapperXmlGenerator 50%：e2e 用 contains() 子串断言，XML 结构内部变异（stripTrailingComma/边界条件）杀不死——改为结构断言/全文 golden 可进一步提升
  - POJO getter/record、System.Logger 调用、防御性代码 → 噪声（可用 mutator 分组/排除配置过滤）
- 建议：核心逻辑类（gen/ddl 包）可设 PIT 门槛（如 ≥70%），XML 生成器待 golden 化后再纳入

## 已知限制（留给后续）

- `--sync` 未实现（需要文件归属标记才能对账磁盘，见 DESIGN §4）
- enum 列失去 enum 类型后旧枚举文件不自动清理（shouldGenerate=false 只删当前类名文件）
- merge 时删除成员不清理其 import（保守策略：不删可能被用户代码引用的 import）
- ALTER COLUMN SET/DROP DEFAULT、FK/CHECK、分区、FULLTEXT/SPATIAL 索引 → warning 跳过（M1a 已记录）
- 方法体引用类型的 import 由生成器显式登记（Expr 助手无状态，见 PROGRESS 决策）
- **2026-08-30：静态检查治理收尾（随 opt-spotbugs 变更）**。① KeyFor 局部化→清零：`@UnknownKeyFor` 显式标注/PECS 签名/keySet 拷贝替代类级抑制（实验证实 `@KeyFor` 对非 key 数据不适用）；② lombok 生成成员自动 `@SuppressFBWarnings`（lombok.config），RCN 排除删除；③ jsr305 移除、注解统一为 checkerframework `@Nullable`（纯 type-use，spotbugs TIGHTENS 对比缺陷随迁移消失）；④ TypeReference `callSuper=true` + 字段遮蔽消除（equals 契约修复）；⑤ core EI 就地 `@SuppressFBWarnings(justification)`（判据：写入通道/引用传递语义就地注解，只读快照语义浅拷贝修复）。**最终 spotbugs-exclude 仅 1 条（tree 包可修改 AST 设计）**。
- **2026-08-30：StatementApplier 多态分发重构（随 chore-applyone-polymorphism 变更）**。`DdlOperation` 接口加 `apply(Schema, ApplyResult)`，10 个操作类自实现应用语义（迁移 applyOne 分支逻辑），`applyOne` 10 分支 if-else → 1 行分发（OCP：新增操作类型 = 新类实现 apply，应用器零修改）。列/索引操作自含表存在判空（显式重复，YAGNI 不抽基类）。纯结构重构，StatementApplierTest 行为断言不变。
- **2026-08-30：引入 JaCoCo 覆盖率（随 opt-jacoco 变更）**。jacoco 0.8.12：prepare-agent 绑 initialize、report/check 绑 test 阶段（进 `mvn clean test`）；全局 line ≥75% 门槛（BUNDLE 规则）。基线实证暴露 cli 模块 0% 覆盖（无业务测试）→ 补 MainTest（8 测试），cli 覆盖率 0% → 87.7%；门槛生效实证（0.99 阈值触发构建失败）。与 PIT 互补：JaCoCo 每次构建量化保底、PIT 定期深度验证。
- **2026-08-30：checkerframework 升级 error 级 + 注解化治理（随 opt-spotbugs 变更）**。移除 `-Awarns`：所有空指针问题强制修复或带理由抑制。处理原则（用户拍板）：① 真可空字段 → `@Nullable`（javadoc/extend/pkg/root/注解值等）；② 构建后必有字段 → `@MonotonicNonNull` + 读取端判空 throw（tree 可修改 AST 的 name/kind/modifiers、builder 字段，**builder 构造器/build 判空 fail-fast 顺带修复真实缺陷**：Class/Method.Builder 缺 modifiers 默认值）；③ 框架注入字段（JUnit @TempDir/@BeforeEach、Maven @Parameter）→ 不假设注入，标 `@Nullable` + getter/使用点显式校验（tempDir()/config()/rootPath()）；④ KeyFor 子检查（NullnessChecker 伴生不可关闭）对 JDK 泛型通配符误报 → 局部 `@SuppressWarnings("keyfor")` 10 个类（非全局抑制）；⑤ initialization 检查抑制零残留。JUnit 断言契约经 `checker/junit-assertions.astub`（@EnsuresNonNull）声明。
