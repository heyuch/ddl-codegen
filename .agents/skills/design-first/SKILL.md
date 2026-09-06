---
name: design-first
description: 强制"先分析设计、沉淀文档、再实现"的开发工作流（10 步：含独立评审 pass、一致性核对、蒸馏收尾）。变更目录由 new-change.sh 分配变更号（命名权威），禁止手工创建。在新功能(feat)、重构(refactor)、优化(optimize)、问题修复(fix)、杂务(chore)开始时使用。
---

# Design-First 工作流（10 步）

## 何时使用

非平凡改动（类型：feat/optimize/refactor/fix/chore，语义见「变更类型」）必须走本流程。
**小改动**（1-3 个文件、逻辑清晰）可省略设计文档与独立评审，分析+取舍写进 commit message。

## 流程（顺序执行，禁止跳过）

1. **分析**：问题是什么、为什么做、可选方案与取舍（每项一句话）；读 `docs/architecture.md`（现状）+ `docs/changes/README.md`（曾做/曾否决）
2. **脚手架**：运行 `.agents/skills/design-first/scripts/new-change.sh <type> <标题>`——自动分配变更号 `{YYYYMMDD}-{NN}` 并注入 design.md 头部元信息；**禁止手工创建变更目录**（历史归档用 `--date YYYYMMDD`）
3. **设计**：填 `design.md`（模板 `docs/changes/TEMPLATE.md`）；影响面写具体符号（类/文件/生成器，可 grep 验证）；中/大改动必含「类职责与交互」（类职责一句话入 javadoc + 依赖方向）
4. **设计自评（可回环）**：按「设计自评检查单」四组核对——SOLID / GRASP 职责分配 / 语义归层 / 设计模式取舍；不满足回第 3 步
5. **独立评审（中/大，硬性）**：新上下文实例（新会话/subagent）只带 `design.md` + `docs/architecture.md` + `docs/changes/README.md`，输出「与现状/历史冲突清单」（含查索引：曾做/曾否决；影响面可验证性），并按「设计自评检查单」四组复核设计；冲突回写 design.md，不通过回第 3 步
6. **用户评审（硬关卡）**：把 design.md 呈现给用户；**未获认可不得进入实现**；结论落痕到变更目录 `progress.md`（通过 ✅ / 否决 ❌+理由）
7. **实现**：按设计写代码
8. **验证**：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿 + 静态检查全绿
9. **一致性核对**：对照 design.md「改动文件与影响面」「类职责与交互」核查实现偏差 → 记 `progress.md`「实现偏差」；实质偏差回第 7 步或第 6 步
10. **蒸馏收尾**：10a 决策/偏差 → 本变更 `progress.md`；10b 现状 → `docs/architecture.md`（更新条目）；10c 索引行 → `docs/changes/README.md`；10d 目录处置（作废默认删除，先完成 10a-10c）；10e 改记忆文档 → 跑自检用例集；10f `new-change.sh check` + `check-docs`（改记忆文档时后者必跑）

## 设计自评检查单（步骤 4 执行细则；步骤 5 独立评审同此视角）

逐类/逐职责核对；任一不满足回第 3 步。

**① SOLID**：SRP（这个类为什么同时承担 X/Y？）/ OCP（扩展点是否闭合）/ LSP / ISP / DIP（依赖是否朝向抽象）。

**② GRASP 职责分配**（每个新职责自问「归谁、为什么归它」；领域判据源：Larman《Applying UML and Patterns》，术语表 glossary 有指针条目）：
- 信息专家 Information Expert：数据与行为是否同居——谁持有数据，谁提供操作
- 创建者 Creator：谁持有创建所需上下文/聚合数据，谁创建
- 低耦合 Low Coupling / 高内聚 High Cohesion：依赖面最小、职责内聚（「逻辑混杂 vs 元素繁多」判别见 static-rules-review §6）
- 控制器 Controller：系统事件入口是否收敛，避免入口/分发层直接操作领域
- 多态 Polymorphism：按类型分叉用多态而非 instanceof 链（实证：StatementApplier.applyOne 20260830-03）
- 纯虚构 Pure Fabrication：无合适领域归属时引入服务/工厂类，不为「真实」硬塞职责
- 间接 Indirection：中介/分发是否真的降耦且值得
- 预防变化 Protected Variations：易变点是否包在稳定接口/抽象后

**③ 语义归层**（方言/文本语义）：引号、保留字、命名规则、格式等「方言/文本语义」**只存在于其直接生产者/消费者一层**；模型与共享服务只存真值。反例实证：20260906-07-fix-generator-leftover-fixes 把 MySQL 反引号语义泄漏进 NamingService 与模型 → 20260906-11-fix-identifier-hygiene 改 parse 层剥引号 + MapperXmlGenerator 单点加引（职责归层修正）。

**④ 设计模式取舍**（任何模式/纯虚构类的使用前必答；不用模式也是结论）：① 能否用设计模式？② 用哪种（说出名字）？③ 它简化了什么？④ 它复杂了什么（间接层/样板/可读性代价）？⑤ 值得吗？——实证教训：不为简单开关建抽象（annotation-interceptors 作废，见 changes/README 索引行）。用了模式就把 ③④⑤ 结论写进 design.md。

## 变更类型

判据：**对外契约/产物输出是否变化？**
- 变 → `feat`（新能力/新用法：新产物、config、DDL 支持）或 `fix`（回归修复）。**破坏性变化不标 refactor**，design 影响面注明 breaking
- 不变 → 按意图：`refactor`（行为等价重构，靠测试证明）/ `optimize`（非功能性能/输出质量优化）/ `chore`（工程杂务：工具链/构建/依赖/文档治理/命名）

## 变更目录与引用

- 目录由 `new-change.sh` 创建；**变更号 = 目录 slug**；引用变更用 slug 或其相对链接
- 目录生命周期（批准/否决痕迹、作废三态、`-rejected`/`-superseded` 状态后缀）、记忆文档自检用例集：见 `docs/changes/README.md`

## 硬性规则

- **未写设计文档 + 未经用户评审 → 不得实现**（小改动豁免见「何时使用」）
- **变更目录只允许 `new-change.sh` 创建**
- 中/大改动必须过第 5 步独立评审
- 跳过步骤直接改代码 = 违规
- 派发 subagent 时以 `skill: "design-first"` 附加本 skill
