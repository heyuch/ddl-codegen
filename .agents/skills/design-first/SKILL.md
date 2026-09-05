---
name: design-first
description: 强制"先分析设计、沉淀文档、再实现"的开发工作流（10 步：含独立评审 pass、一致性核对、蒸馏收尾）。变更目录由 new-change.sh 分配变更号（命名权威），禁止手工创建。在新功能(feat)、重构(refactor)、优化(optimize)、问题修复(fix)、杂务(chore)开始时使用。
---

# Design-First 工作流（10 步）

本项目的工作流硬性规则（详见 `AGENTS.md`「开发工作流（硬性规则）」）。**AGENTS.md 只保留硬性规则与文档索引，细则一律以本文件为准**（改规则只改本文件）。现状架构以 `docs/architecture.md` 为准；变更史/决策以 `docs/changes/README.md` 索引 + 各变更目录 + 初始归档（`20260801-01-feat-project-foundation`）为准——**无顶层决策台账**。

## 何时使用

任何非平凡改动：新功能（feat）/ 优化（optimize）/ 问题修复（fix）/ 重构（refactor） / 杂务（chore）。
**小改动**（1-3 个文件、逻辑清晰）可省略设计文档与独立评审，但分析+取舍必须写进 commit message。

## 流程（顺序执行，禁止跳过）

1. **分析**：问题/需求是什么、为什么做、可选方案与取舍（每项一句话）；先读 `docs/architecture.md`（现状）+ `docs/changes/README.md`（曾做/曾否决 + 决策史入口）
2. **脚手架**：运行 `.agents/skills/design-first/scripts/new-change.sh <feat|optimize|refactor|fix|chore> <标题>` —— **命名权威**：自动分配变更号 `{YYYYMMDD}-{NN}`、注入 design.md 头部元信息；**禁止手工创建变更目录**（历史归档目录可用 `--date YYYYMMDD`）
3. **设计**：填写 `design.md`（模板 `docs/changes/TEMPLATE.md`）；**影响面写具体符号**（类名/文件/生成器，评审可 grep 验证）；**中/大改动必含「类职责与交互」节**（新类/改动类职责一句话——同时写入类 javadoc，及类间依赖方向）
4. **设计评审（SOLID，可回环）**：按 SRP/OCP/LSP/ISP/DIP 逐类 check——不满足回第 3 步调整
5. **独立评审 pass（中/大改动，硬性）**：用**新上下文实例**（新会话或 subagent，不带本设计会话上下文）评审，只带输入 = `design.md` + `docs/architecture.md` + `docs/changes/README.md`（决策经索引/变更目录查阅）；输出「与项目现状/历史冲突清单」——含查索引（本方案/相似方案是否曾做、曾否决）、影响面符号可验证性、与 architecture.md 现役机制冲突点；冲突清单回写 design.md，不通过回第 3 步
6. **评审（硬性关卡）**：把 `design.md` 呈现给用户评审；**未获认可不得进入实现**；结论落痕到变更目录 `progress.md`（通过 = 状态 ✅；否决/作废 = ❌ + 理由），不留在对话
7. **实现**：按设计写代码
8. **验证**：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿 + 静态检查全绿
9. **一致性核对**：对照 `design.md`「改动文件与影响面」「类职责与交互」逐条核查实现偏差（多做了/少做了/做法与设计不符），偏差记入 `progress.md`「实现偏差」；有实质偏差回第 7 步修正或回第 6 步改设计
10. **蒸馏收尾**：
    - 10a 决策与偏差 → 本变更目录 `progress.md`（执行期拍板不另立顶层台账；决策史 = changes/README 索引 + 各变更目录 + 初始归档）
    - 10b 现状变化 → `docs/architecture.md`（更新对应条目，非散文式追加）
    - 10c 索引行 → `docs/changes/README.md`（作废/被取代的既有目录按 README 生命周期规则处置）
    - 10d 目录处置（完成保留；作废默认删除，删除前先完成 10a-10c）
    - 10e 本次若改动记忆文档（AGENTS.md / SKILL.md / architecture.md / changes/README.md / glossary）→ 跑「记忆文档自检用例集」：新上下文实例只喂记忆文档作答，答错即不合格（问题集在 `docs/changes/README.md`）
    - 10f 运行 `new-change.sh check` 通过

## 变更目录与变更号规范

- 位置：`docs/changes/`；命名 `{YYYYMMDD}-{NN}-{feat|optimize|refactor|fix|chore}-{标题}`（同日冲突加序号，无 `-2` 补丁）；**变更号 = 目录 slug**（紧凑日期 + NN，NN 由脚本分配、作废/删除后不重用，留洞不补）
- **引用变更**用变更号 slug 或相对链接（slug 自带 type+title 可读）；已删除的作废变更不引用目录，用叙述标题或指索引行
- 生命周期（详见 `docs/changes/README.md`）：批准/否决痕迹仓库化（progress 状态 + 索引行）；作废处置三态——① 默认删除 + 索引留痕；② 部分被取代保留原名、索引注明；③ 保留陈列个案改名加 `-rejected`/`-superseded` 后缀（仅此场景用后缀）
- `docs/changes/README.md` = 变更索引（变更号↔目录）+ 生命周期规则 + 引用规则 + 记忆文档自检用例集
- 文档级校验：`new-change.sh check`（AGENTS 规则是 advisory，check 是确定性门禁）

## 硬性规则

- **未写设计文档 + 未经用户评审 → 不得实现**（小改动豁免，但 commit message 必须说明分析与取舍）
- **变更目录只允许 `new-change.sh` 创建，禁止手工 mkdir**
- 中/大改动必须过第 5 步独立评审 pass
- 跳过步骤直接改代码 = 违规
- 派发实现给 subagent 时，通过 subagent 的 `skill` 参数附加本 skill（`skill: "design-first"`），确保执行侧同样遵守
