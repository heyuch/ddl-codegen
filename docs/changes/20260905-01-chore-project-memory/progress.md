# progress：chore-project-memory

## 状态：✅ 完成（2026-09-05 评审通过 → 当日实现收尾）

设计稿经多轮评审定稿（背景证据 / 取舍表 / 方案 0-6）。用户逐项拍板后确认执行，此处为仓库内批准痕迹（评审结论不留在对话）。

## 实现核对（工作流第 9 步：一致性核对）

对照 design.md「改动文件与影响面」「类职责与交互」核查：

- [x] 变更目录重命名 `20260905-01-chore-project-memory`（**偏差**：git mv 因文件未跟踪失败，改用 mv，已记录）
- [x] `docs/architecture.md` 对账产出（子代理，214 行；**10 条对账发现**入文末——EntityGenerator 不存在、`--force` 逃生口未实现、保留命名空间 = naming.*+annotations.*、CodeGenerator javadoc 与实现矛盾等）
- [x] `docs/changes/README.md` 索引 + 规则（8 存量行 + 作废行 + 固定节；本变更行已追加）
- [x] `new-change.sh` 重写（算名 + check + 模板注入；**沙箱实测创建路径通过**：正确分配 `20260905-01`、占位符全部注入）
- [x] `TEMPLATE.md` 占位符化 + 注释更新（移除过期 archive 说法）
- [x] SKILL.md 10 步流程升级（独立评审/一致性核对/蒸馏收尾/自检/check）
- [x] AGENTS.md 同步（10 步概要、命名规范、文档地图、架构节去拦截器）
- [x] **AGENTS.md 去冗余（用户追加意见）**：改为「单一事实源 + 索引」——移除与 architecture.md/SKILL.md 重复的架构、工作流细则、SPI/注解细节，只留硬性规则 + 文档索引表 + 概要（14.5KB → 9.9KB）；SKILL.md 首段同步为"AGENTS 不复述、细则以本文件为准"；glossary 出处列从已删锚点（关键机制/扩展点/数据流/架构）改指 architecture.md 对应节；TEMPLATE 流程引用节名精确化
- [x] glossary / tasks / design.md 头部降级 / repo progress.md 决策追加
- [x] README.md 冲突修订 → **无冲突**（扫描确认 README 无拦截器/use=/--force 描述，未改）
- [x] 删除 `2026-08-29-opt-annotation-interceptors/`（索引行先落）

## 验证结果

- `new-change.sh check`：**通过**（8 个变更，含存量格式与新格式共存）
- 全量 `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`：**BUILD SUCCESS**（5 模块全绿，jacoco/spotbugs 门禁通过；无 Java 改动）
- **记忆文档自检走查（关键验收）**：新上下文 explore 实例只喂 AGENTS.md + architecture.md + changes/README.md + progress.md 作答 10 问——**全部答对**；并额外发现 3 处文档内部不一致，已修复：
  1. repo `progress.md` 阶段状态表残留两行过期占位（M3 ×7 ⬜ / M4 --sync ⬜）→ 删除并加注
  2. `AGENTS.md` 顶层产物清单措辞（Entity 为产物名，由 pojo 生成器产出）→ 澄清
  3. 本 README 状态说明含进行中示例 → 泛化；本变更索引行已追加

## 实现偏差（记入本节）

- 目录重命名用 `mv` 而非 `git mv`（源未跟踪）
- 对账发现 10 条**均未改代码**（超出本次文档治理边界）：CodeGenerator javadoc 与实现矛盾、源码 5 处"拦截器"javadoc 残留、`--force` 未实现等——留待后续代码清理变更处理（已在 architecture.md「对账发现」与 repo progress.md 决策记录中留档）
- repo `progress.md`/`tasks.md` 中"拦截器时代"的 M2/M3 历史叙述保留（历史台账，架构已由 architecture.md 覆盖纠正）

## 蒸馏（工作流第 10 步）

- 10a 决策 → repo `docs/progress.md`「关键决策记录」已追加（2026-09-05 条）
- 10b 现状 → `docs/architecture.md`（本次为初建；后续变更按架构节 10b 更新条目）
- 10c 索引行 → `docs/changes/README.md` 本变更行已追加（状态 完成）
- 10d 目录处置：本变更完成保留；作废目录 `annotation-interceptors` 已删除（索引行「作废-已删」）
- 10e 记忆文档自检：本变更即首次实弹——走查 10 问通过（见上）
- 10f `new-change.sh check`：通过

## 追加范围（用户后续意见，2026-09-05：顶层历史文档归档）

- **决策**：顶层不再维护 `docs/progress.md`（进度台账/决策台账）、`docs/tasks.md`（任务清单）、`docs/design.md`（历史基线）——一并归档为初始建设期变更目录。
- **归档动作**：`new-change.sh` 加 `--date YYYYMMDD`（历史归档目录用，规则一致）；建 `20260801-01-feat-project-foundation/`，原 docs/design.md（→design.md）+ docs/progress.md（→progress.md，正文前加归档说明头）+ docs/tasks.md（→tasks.md）**原样迁入**。
- **前向信息保活**：原 progress「已知限制」前向条目迁入 `docs/architecture.md`「已知限制（现状缺口）」节（--sync 未实现 / enum 旧文件清理 / merge import 保守 / ALTER·FK·CHECK·分区·FULLTEXT/SPATIAL warning 跳过 / 源码 javadoc 残留清理项）。
- **决策史落点调整**：删除顶层台账后，决策史 = changes/README 索引 + 各变更目录 + 初始归档（早期 M0-M4 与 2026-08-30 无变更目录的执行期决策随全文入档）；SKILL/AGENTS/TEMPLATE 的 10a「决策 → docs/progress.md」措辞全部改为「决策与偏差 → 所在变更目录 progress.md」。
- **涟漪同步**：AGENTS 文档索引表（删 design/progress/tasks 行、加初始归档行）、README 引言、SKILL 步骤 1/5/10a、TEMPLATE、glossary（出处 + PIT 出处 + 初始归档词条）、architecture.md 头部历史基线路径、changes/README（加归档行 + 生命周期措辞）。
- **一致性核对**：全仓 grep 确认顶层已无 docs/{design,progress,tasks}.md 活引用（仅历史文档内部旧链接与已冻结变更稿保留原写法）；`new-change.sh check` 通过（9 个变更）。

## 追加范围（用户后续意见，2026-09-05：存量变更目录命名统一）

- **决策（推翻早期「存量不改名」边界，用户拍板：项目早期迁移成本低）**：存量 7 个 `YYYY-MM-DD-*` 变更目录全部迁移为统一格式 `{YYYYMMDD}-{NN}-…`。
- **迁移方式**：`git mv` 保留历史；NN 按 git 创建顺序分配（08-29 由 maven-plugin 最早 → NN01，annotation-interceptors[已删] 占 NN04 留洞不重用，core-first NN05；08-30 前缀组按实际创建序 spotbugs=01 / jacoco=02 / applyone=03）。
- **映射表**：已写入 `docs/changes/README.md`「命名迁移」节（供旧文档引用对照 + git log 溯源）。
- **涟漪同步**：changes/README 全部索引行更新为新 slug + 顺序按变更号、新增「命名迁移」节、引用规则去掉"存量保留旧格式"句；`static-rules-review.md` 实证行旧名替换；`new-change.sh check` **收紧为仅接受统一格式**（旧格式豁免随迁移删除——无豁免即无回归）；AGENTS/SKILL/TEMPLATE/glossary/architecture 无存量旧名引用（已 grep 验证）。
- **历史原文保留**：初始归档、各变更目录内、本变更 design/progress 中引用的旧目录名不改（属当时历史，映射表对照）。
- **一致性核对**：`new-change.sh check` 通过（9 个变更：含 08-29 的 04 洞）；活文档 grep 无残留旧格式目录名（static-rules-review 已改，余均为历史原文）。

## 追加范围（用户后续意见，2026-09-05：AGENTS.md 精简）

- **决策（用户拍板）**：代码质量由静态工具链强制（已实证：checkstyle 强制全部命名大小写/import/空行等 20+ Name 规则 + CustomImportOrder + AvoidStarImport + EmptyLineSeparator；spotless 管格式/import 排序）→ AGENTS.md 不复述工具已强制的内容（消除双写漂移源）；**命名风格保留**（用户青睐简洁明确一致）。
- **精简结果**（约 167 → 60 行）：删 Code Style Guidelines 大部（General/JavaVersion/Imports/Types/Null 样板/Error/Lombok/CodeStructure）与 Quality Bar（静态检查清单复述 pom 门禁）；保留并压缩——命名哲学（含反例与实证）、注释语言（中文业务/英文技术/WHY）、@Nullable 统一包 + 禁 Optional、测试可读性（AAA/extract/参数化取舍，工具盲区）、垂直间距一句话；命令压缩为 3 条（全量门禁/-Pquick/单测过滤）。
- **门禁保留**：常驻索引表补回 SKILL / 初始归档 / static-rules-review 行 + 工作流硬性规则 3 条（防 skill 自动触发不可靠导致流程绕过——业界 CLAUDE.md 最佳实践：常驻文件只放"删掉会犯错"的规则，工具层是确定性门禁）。
- **涟漪**：glossary「门禁」出处自 AGENTS「Quality Bar」（已删小节）改指「Build, Lint, and Test Commands」；其余锚点（开发工作流（硬性规则）等）保留。
- **一致性核对**：全仓 grep 确认无对已删 AGENTS 小节的悬空引用。

    - 二轮微调：移除 AGENTS 中全部元叙事（单一事实源自证、漂移历史、大小写由工具强制的补注等）——常驻文件只放操作规则；理由已在本变更记录，不重复驻留。43 → 31 行。

    - 三轮微调：architecture.md 头部删对账时间戳、changes/README 删自检规则"同源对症"尾巴（保留诊断句）、SKILL 删"单一事实源"定义句（保留"改规则只改本文件"维护指令）。
