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

- ClassFanOutComplexity：阈值 20 保留；分发器类针对性 @SuppressWarnings + 实证注释（见 STATIC-RULES-REVIEW.md §5/§6）
- **Expr/Block 助手简化**（M0c 并入 M2）：助手为纯字符串组合，不做 import 魔法；方法体引用类型的 import 由生成器显式 addImport（避免状态化 import-sink API，更清晰）。M0c 拆入 M2（对着真实生成上下文构建，避免空想 API）。
- 其余开放问题按 DESIGN.md §17 默认值

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
