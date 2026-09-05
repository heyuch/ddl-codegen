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

## 追加范围（2026-09-06：变更类型语义定稿）

- **决策（用户拍板）**：类型集合保留 5 个（feat/optimize/refactor/fix/chore）+ 边界定义死；**refactor 取严格等价**（conventional：行为与契约不变、测试证明等价）；破坏性变化（breaking config/API）不标 refactor，须在影响面注明。
- **边界判据（写入 SKILL「变更类型」节）**：对外契约/产物输出变 → feat（新能力）或 fix（回归修复）；不变 → 按意图 refactor（等价重构）/ optimize（非功能性能/输出质量优化）/ chore（工程杂务含文档治理）。
- **观察期**：fix/optimize 为保留席位，长期零产出再并入/移除（不在当前砍）。
- **历史语义**：2026-08 早期目录按宽松语义标注（破坏性重构亦标 refactor/opt，如 core-first）；2026-09-06 起按新定义执行，旧目录冻结不改——SKILL 注记存档。
- **可选未采纳**：docs 单列、breaking 正交标注（breaking 目前靠类型外影响面注明，不另设机制）。

## 追加范围（2026-09-06：改动分级与类型单源收敛）

- **问题**：「非平凡/小改动」分级与类型词汇在 AGENTS / SKILL（frontmatter+何时使用+命名规则+硬性规则 4 处）/ TEMPLATE / new-change.sh 各有副本——改类型需动多处、多文档定义易让 agent 读到矛盾版本。
- **收敛**：语义单源 = SKILL.md——「何时使用」管**分级**（小改动豁免判定）、「变更类型」管**类型集与语义**；AGENTS rule1 与 TEMPLATE meta 改为指针不复述；SKILL 内命名规则不再重复 token 列表（用 `{type}` + 指向「变更类型」）；硬性规则豁免句指向「何时使用」。
- **保留的执行镜像（注明归属，无法消除）**：new-change.sh 白名单（已加"须与 SKILL「变更类型」同集"注释）、SKILL frontmatter 关键词（工具触发用）。改类型时实际改动点 = SKILL「变更类型」+ 脚本白名单 + frontmatter，其余文档零改动。

## 追加范围（2026-09-06：SKILL.md 自审精简）

- **原则**：SKILL 的功能 = 触发时指导执行设计流程；凡与此无关的内容移出（理由沉淀在本变更记录，SKILL 不驻留自证）。
- **移除**：① 首段文档体系自证（AGENTS 与 SKILL 关系、"改规则只改本文件"、"无顶层台账"——均为维护者注记）；② 「变更目录与变更号规范」中与 changes/README 重复的生命周期/引用细则（三态、后缀语义、README 角色）→ 压缩为指针；③ 「变更类型」节中观察期与历史注记（决策性 meta，已在本记录存档）。
- **保留**：触发范围（何时使用/小改动豁免）、10 步流程、类型语义判据、目录引用指针、硬性规则（均为执行所需）。66 → 约 46 行。

## 追加范围（2026-09-06：TEMPLATE 增「验收标准」节）

- **决策（用户拍板：加独立节）**：TEMPLATE 在「改动文件与影响面」与「验证」之间新增「验收标准」——完成态的可观察判据（WHAT），与「验证」（证明手段 HOW）分工；中/大必填。
- **格式指引**：断言式/可测，禁用"正常工作"话；覆盖 主路径 / 边界 / 负面错误（warning 不覆盖）/ 幂等与契约回归（@Generated 所有权、用户手写保留）；一条验收 → 第 8 步至少一个测试。
- **接点**：第 3 步写 → 第 6 步用户评审逐条核对 → 第 8 步测试派生 → 第 9 步一致性核对对照含验收。

## 追加范围（2026-09-06：TEMPLATE 自审精简）

- **原则**：TEMPLATE 只该是设计文档的填写骨架；过程性内容（流程步数、评审关卡、落痕位置、分级豁免指针）由 SKILL/AGENTS/changes-README 单源，不在此复述（步号引用会在流程改版时造成多处失效）。
- **移除**：头部"new-change.sh 生成/10 步流程/分级豁免" 3 行元注记（保留 1 行内容边界：只记设计与取舍）；「背景与问题」第 1 步先读指针；「类职责与交互」SOLID 自评与独立评审两段 blockquote；「对账与冲突清单」第 5 步字样；「改动文件与影响面」10e 步号；「验收标准/验证」中第 8/9 步引用；**整节删除「评审与收尾」（第 6/9/10 步关卡全为流程 meta，SKILL 管）**。
- **保留**：占位符头（脚本注入）、8 个内容节骨架 + 填写指引（含验收标准成品示例）。
- 67 → 约 42 行。

## 追加范围（2026-09-06：文档卫生约束固化 ①+②）

- **决策（用户拍板 ①+② 双层）**：
  - ① 过程层：`changes/README`「记忆文档自检用例集」新增**卫生类 3 问**（自证/历史叙事/治理注记、流程步号或工具重复风格、同事实多份定义）——AI 走查抓语义，随每次记忆文档改动（10e）执行。
  - ② 确定性层：`new-change.sh check-docs` + 黑名单 `doc-hygiene.patterns`（每模式 = 一次已清理实证；事故 → 永久回归用例）。局限：只防已知症状；步号指针因 SKILL 内部合法步号无法正则硬拦，靠 ① 抓。
- **接点**：SKILL 10f 与 AGENTS 硬规则 2 增加 `check-docs`（改记忆文档必跑）。
