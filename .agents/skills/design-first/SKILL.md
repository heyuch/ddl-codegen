---
name: design-first
description: 强制"先分析设计、沉淀文档、再实现"的开发工作流。在新功能(feat)、优化(opt)、问题修复(fix)、重构/杂务(chore)开始时使用：先脚手架创建变更目录与设计文档，经用户评审后再实现，完成后同步项目文档。
---

# Design-First 工作流

本项目的工作流硬性规则（详见 `AGENTS.md`「开发工作流」与「文档命名与组织规范」）。

## 何时使用

任何非平凡改动：新功能（feat）/ 优化（opt）/ 问题修复（fix）/ 重构、杂务（chore）。
**小改动**（1-3 个文件、逻辑清晰）可跳过设计文档，但分析+取舍必须写进 commit message。

## 流程（顺序执行，禁止跳过）

1. **分析**：问题/需求是什么、为什么做、可选方案与取舍（每项一句话）
2. **脚手架**：运行 `./scripts/new-change.sh <feat|opt|fix|chore> <标题>` 创建变更目录与 `design.md` 骨架
3. **设计**：填写 `design.md`（模板 `docs/changes/TEMPLATE.md` 的章节：背景与问题 / 可选方案与取舍 / 方案 / 改动文件与影响面 / 验证）；**中/大改动必含「类职责与交互」节**（新类/改动类职责一句话——同时写入类 javadoc，及类间依赖方向）
4. **设计评审（SOLID，可回环）**：按 SRP/OCP/LSP/ISP/DIP 逐类 check 职责与关系（职责单一？依赖方向可逆？接口内聚？继承不破坏替换？扩展不须改现有代码？）——不满足回第 3 步调整，通过后进入用户评审
5. **评审（硬性关卡）**：把 `design.md` 呈现给用户评审；**未获认可不得进入实现**
6. **实现**：按设计写代码
7. **验证**：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿 + 静态检查全绿
8. **文档同步**：架构/数据流/约定变化 → 更新 `AGENTS.md`「项目架构」节或 `docs/design.md`；关键决策追加到 `docs/progress.md`「关键决策记录」

## 变更目录规范

- 位置：`docs/changes/{YYYY-MM-DD}-{feat|opt|fix|chore}-{标题}/`（同日冲突加 `-2`）
- 固定文档：`design.md`（必填）、`progress.md`（可选，变更执行状态）
- 标题：小写字母/数字/连字符

## 硬性规则

- **未写设计文档 + 未经用户评审 → 不得实现**（小改动豁免，但 commit message 必须说明分析与取舍）
- 禁止跳过步骤直接改代码
- 派发实现给 subagent 时，通过 subagent 的 `skill` 参数附加本 skill（`skill: "design-first"`），确保执行侧同样遵守
