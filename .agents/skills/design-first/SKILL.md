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
4. **SOLID 自评（可回环）**：SRP/OCP/LSP/ISP/DIP 逐类 check；不满足回第 3 步
5. **独立评审（中/大，硬性）**：新上下文实例（新会话/subagent）只带 `design.md` + `docs/architecture.md` + `docs/changes/README.md`，输出「与现状/历史冲突清单」（含查索引：曾做/曾否决；影响面可验证性）；冲突回写 design.md，不通过回第 3 步
6. **用户评审（硬关卡）**：把 design.md 呈现给用户；**未获认可不得进入实现**；结论落痕到变更目录 `progress.md`（通过 ✅ / 否决 ❌+理由）
7. **实现**：按设计写代码
8. **验证**：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿 + 静态检查全绿
9. **一致性核对**：对照 design.md「改动文件与影响面」「类职责与交互」核查实现偏差 → 记 `progress.md`「实现偏差」；实质偏差回第 7 步或第 6 步
10. **蒸馏收尾**：10a 决策/偏差 → 本变更 `progress.md`；10b 现状 → `docs/architecture.md`（更新条目）；10c 索引行 → `docs/changes/README.md`；10d 目录处置（作废默认删除，先完成 10a-10c）；10e 改记忆文档 → 跑自检用例集；10f `new-change.sh check` + `check-docs`（改记忆文档时后者必跑）

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
