# memory-doc-drift-fixes

> 变更号：20260906-10　变更类型：chore　目录：`docs/changes/20260906-10-chore-memory-doc-drift-fixes/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

20260906-08 收尾记忆文档自检（新上下文实例 10 问走查）顺带发现 3 处**既有漂移**（均非 08 引入，当时留待用户决定；2026-09-06 用户确认一并处理）：

1. `docs/architecture.md` 命名速查引用 `naming.enumClassName`——不是 config 键，实际键为 `naming.enum.style`（同文件 :144 config 键表正确，速查处笔误）。
2. `docs/glossary.md` design-first 词条复述 SKILL 步骤流程——违反「同一事实只存源头文档」卫生规则（步号/流程只属于 `.agents/skills/design-first/SKILL.md`）。
3. `AGENTS.md` 「Build, Lint, and Test Commands」模块清单 `cli / maven-plugin / core / tree` 缺 20260906-03 新增的 `ddl-codegen-it-springboot`（已是 reactor 成员）。

## 方案

- architecture.md：速查键名 `naming.enumClassName` → `naming.enum.style`（以 config 键表/代码为准）。
- glossary.md：design-first 词条流程复述精简为「10 步 + 检查单」指针（指向 SKILL.md），保留术语辨析职责。
- AGENTS.md：模块清单补 `ddl-codegen-it-springboot`（注明其门禁分级同 03 设计——独立消费工程、生成代码由 spotless/checkstyle/spotbugs 守护）。模块清单属「命令」说明区，改写要避免与「文档与信息架构」表重复治理信息。

## 改动文件与影响面

- `docs/architecture.md`（速查 1 键名）
- `docs/glossary.md`（design-first 词条精简）
- `AGENTS.md`（模块清单 1 行）
- 影响：纯文档；改记忆文档 → 收尾跑自检用例集 + `check-docs`。无代码/构建影响。

## 验收标准

1. architecture.md 全文无 `naming.enumClassName`（grep 为空），速查与 config 键表一致为 `naming.enum.style`。
2. glossary.md design-first 词条不再复述步骤号/检查单细节，仅指针 + 出处。
3. AGENTS.md 模块清单含 it-springboot。
4. `new-change.sh check-docs` 通过（无黑名单模式/自指）。

## 验证

- grep 实证 + `check-docs` + 记忆文档自检用例集（新上下文实例）
