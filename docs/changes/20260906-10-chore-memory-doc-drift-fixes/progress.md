# 变更台账：20260906-10-chore-memory-doc-drift-fixes

## 状态

**实现完成**（2026-09-06：纯文档；提交待落）

## 评审记录

- 2026-09-06 用户确认处理 08 自检遗留 3 处既有漂移（architecture 速查键名笔误 / glossary 复述 SKILL / AGENTS 模块清单缺 it-springboot）。纯文档小改动，design.md 记录取舍与验收。

## 实现记录

- `docs/architecture.md` 命名速查：`naming.enumClassName` → `naming.enum.style`（与 config 键表 :144、代码 PropertiesConfigLoader/DdlConfig 一致）。保留 :93 的 `TableContext.enumClassName`——那是 Java 方法名（`TableContext.java` public 方法），非 config 键，属合法引用。
- `docs/glossary.md` design-first 词条：10 步流程复述精简为「流程细节只存源头 skill，此处不复述」+ 出处双指针（AGENTS.md 硬规则节 + SKILL.md 路径）。
- `AGENTS.md` 模块清单：`cli / maven-plugin / core / tree` → 补 `it-springboot`（注明 = 真实 Spring Boot 消费工程样例、门禁分级见 20260906-03），与根 pom reactor 5 模块对齐。

## 边界（不做，随记录）

- 历史归档（docs/changes/*/design|progress 及 08 progress 的「留待用户决定」记录）不改（append-only）；本索引行即闭合记录。
- 自检观察项（不动作）：architecture it-springboot 依赖列未穷举 jakarta.annotation-api（描述性未宣称穷举）；glossary「此处不复述」与黑名单词形相近但语义正确、check-docs 不拦截。

## 收尾与自检

- 记忆文档自检（新上下文实例 10 问走查）：5 份记忆文档与代码全部自洽；`naming.enumClassName` 作为 config 键的表述已清零（残留仅为方法名/归档）。
- `new-change.sh check` / `check-docs` 通过。
