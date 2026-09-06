# design-quality-checklist

> 变更号：20260906-12　变更类型：chore　目录：`docs/changes/20260906-12-chore-design-quality-checklist/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

20260906-07→11 的反复暴露：design-first 步骤 4 只有 SOLID 自评，缺少「职责/语义归层」视角，方言细节（MySQL 反引号）泄漏进共享命名层而无人拦下。用户要求借此完善设计流程：① 引入 GRASP 职责分配九原则；② 设计模式使用前走「取舍四问」（能否用/用哪种/简化了什么/复杂了什么/是否值得，不用模式也是结论）；③ 落地 07→11 教训为「语义归层」检查项。

## 方案

- **SKILL.md**：步骤 4 由「SOLID 自评」扩为「设计自评（可回环）」，指向新增「设计自评检查单」小节——四组：SOLID / GRASP 九原则（逐项一句话判据 + 本项目实证锚点）/ 语义归层（方言只存在于直接生产者一层，模型与共享服务只存真值，反例实证 07→11）/ 设计模式取舍四问（含实证教训：不为简单开关建抽象——annotation-interceptors 作废）。步骤 5 独立评审声明同此视角。10 步结构与编号不变。
- **TEMPLATE.md**：「方案」段补提示（引入模式/虚构类时写明取舍四问结论；文本/方言语义写明归层）；「类职责与交互」段补「按 GRASP 说明职责归属判据」。
- **glossary.md**：构建与流程表增 GRASP、设计模式两行（中文枚举 + 指针式出处 SKILL「设计自评检查单」，不复制判据细节）。
- 不改动：AGENTS.md（「按其 10 步执行」仍成立）、changes/README 自检用例集（10 问不变，卫生③仍要求 SKILL 为唯一流程源）。

## 改动文件与影响面

- `.agents/skills/design-first/SKILL.md`（步骤 4 文案 + 新增检查单节）
- `docs/changes/TEMPLATE.md`（方案/类职责提示各一处）
- `docs/glossary.md`（+2 行）
- 影响：SKILL/TEMPLATE/glossary 均为记忆文档 → 收尾跑自检用例集 + `new-change.sh check-docs`；无代码/构建影响。

## 验收标准

1. SKILL 含「设计自评检查单」：GRASP 九原则逐项一句话判据、语义归层、模式取舍四问；步骤 4 与检查单互相指认。
2. TEMPLATE 的「方案」「类职责与交互」含上述提示。
3. glossary 增 GRASP/设计模式行（指针式、无判据复述）。
4. `check-docs` 通过（无黑名单/自指）；记忆文档自检 10 问合格。

## 验证

- `new-change.sh check` / `check-docs`；记忆文档自检（新上下文实例）
