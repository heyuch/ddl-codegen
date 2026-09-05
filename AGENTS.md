# AGENTS.md

## 文档与信息架构

| 文档 | 角色 | 何时读 / 写 |
|---|---|---|
| `docs/architecture.md` | 项目现状架构：模块/管线/SPI/注解/config schema/契约/已知限制/代码锚点 | 改动设计前读；变更收尾更新条目 |
| `.agents/skills/design-first/SKILL.md` | 开发工作流细则（10 步 + 检查单 + 变更号规则） | 非平凡改动开始时加载 |
| `docs/changes/README.md` | 变更索引 + 生命周期/引用规则 + 记忆文档自检用例集 | 设计前查曾做/曾否决；收尾追加索引行 |
| `docs/changes/20260801-01-feat-project-foundation/` | 初始建设期归档（设计定稿 + M0-M4 台账/任务/早期决策） | 仅历史溯源 |
| `docs/glossary.md` | 术语表（中英对照） | 写作/命名前查词 |
| `docs/static-rules-review.md` | 静态检查考察（阈值基线/实证/抑制准则） | 静态检查报错时 |
| `README.md` | 用户手册（快速开始 + config/注解参考） | 使用者 |

## 开发工作流（硬性规则）

1. **非平凡改动必须先分析设计、后实现**：加载 skill `design-first` 按其 10 步执行；未写设计 + 未经用户评审不得实现。（分级与小改动豁免见 SKILL.md「何时使用」「变更类型」）
2. **变更收尾必须蒸馏**：决策与偏差 → 所在变更 progress.md；现状 → architecture.md（更新条目）；索引行 → changes/README.md；改记忆文档 → 跑自检用例集与 `new-change.sh check-docs`；`new-change.sh check`。
3. 内容与源头文档冲突时以源头文档为准。

## 命名与写作约定

- **命名**：简洁明确一致——避免 `XxxManager`/`XxxHelper`/`XxxData` 后缀与上下文重复词；短作用域用短名（`ctx`/`p`/`i`）；禁 `U`/`Tmp` 无意义名；实证：`Codegen`、`Generator`
- **注释**：业务逻辑中文、技术文档英文；公共 API 写 javadoc；解释 WHY
- **可空性**：`@Nullable` 用 `org.checkerframework.checker.nullness.qual.Nullable`；禁用 Optional，可空性用 @Nullable 显式表达
- **测试可读性**：AAA 三段空行分隔；密切关联大块提取有名字的辅助方法；同逻辑多组输入用 `@ParameterizedTest`；流程性测试保持显式步骤、不参数化
- **垂直间距**：方法体内不同逻辑段落之间插入空行

## Build, Lint, and Test Commands

Maven 多模块（Java 11：cli / maven-plugin / core / tree）。

- 全量门禁：`mvn clean test`（`JAVA_HOME=/opt/homebrew/opt/openjdk@11`；spotless/checkstyle/error-prone/checkerframework/spotbugs/jacoco 全进）——提交/验收前必跑
- 日常迭代：`mvn -Pquick clean test`
- 单测：`mvn test -Dtest=XxxTest`；单模块：`mvn -pl ddl-codegen-core test`

## 项目（一句话定位）

DDL 驱动的 Java 代码生成框架（MySQL DDL → 增量生成 MyBatis 链路代码）；架构细节见 `docs/architecture.md`。
