# project-memory：项目记忆体系（现状架构 + 变更索引 + 文档生命周期 + 变更号 + 独立评审）

> 变更类型：chore（文档治理与工作流重构）
> 状态：设计稿（用户评审中，未实现）

## 背景与问题

对账发现项目文档体系系统性漂移，根因三处：

1. **现状无单一事实源**：`docs/design.md` 定位为"已定稿、勿再推翻"的实现规格，内含任务清单与已知缺口（§14），随 `opt-core-first` 等后续变更已部分失效却无人认领；`AGENTS.md`「项目架构」节与其职责重叠，声称"已迁移"实际只迁一半。证据矩阵：

   | 事实 | 代码真相 | 文档现状 |
   |---|---|---|
   | GeneratorInterceptor / use 链 | 已删除（无 interceptor 包、无引用） | `AGENTS.md` 管线仍写「→ 拦截器 →」；`docs/design.md:57,236-249` §4/§11 整节描述；`tasks.md:72` 列着任务 |
   | 注解处理 | type/as/ignore 三 handler 注册；@as 列/表级生效（`TableContext.java:57,75`）；@ignore 模型剪枝（`StatementApplier.pruneIgnored`） | `glossary.md` 引用漂移中的 design.md §9；`annotation-interceptors/progress.md` 自称"@as 移除"（未落地） |

2. **变更目录无生命周期**：8 个目录完成后全部滞留；TEMPLATE.md:6 写着"可归档到 archive/"但无规则、无 archive/。作废目录 `2026-08-29-opt-annotation-interceptors/` 的"保留决策"（@as 移除）**未核验即声称生效**——实为未落地，此类目录成了不可信的过期记忆。

3. **变更目录命名无唯一性保障**：同日多个变更靠标题区分（`2026-08-29` 有 5 个、`2026-08-30` 有 3 个），"同日冲突加 `-2`"是事后补丁，无稳定引用句柄——索引、决策记录、cross-link 只能引用易变的长路径。

4. **无检索入口**：AI/人无法低成本回答"项目现在长什么样 / 以前试过什么、为何否决"，每个需求从零看代码 → 视野窄、重复提案已否决方案、文档继续漂移。

用户确认的目标取舍：清理作废目录（删除 + 决策索引化）；新建**现状架构文档**（architecture.md）；新建**变更索引文档**；**变更号**命名规范（紧凑日期 + 序号唯一标识，**变更号即目录 slug、引用直接可读**）；命名经 **`new-change.sh` 固化**（脚本算名 + `check` 校验）；`design-first` skill 补**独立评审**与**蒸馏收尾**；**目录名状态后缀**约定（混合制）；本轮先出设计稿供评审，实现另起一轮。

设计参照：Anthropic [AI-Native SDLC playbook](https://claude.com/blog/the-ai-native-sdlc-playbook)（intent home / verifier subagent / evals / 审计痕迹、advisory vs deterministic 分层），用户评审拍板吸收其中轻量增量（见 §3/§6 标注），其余重机制明确不引入（见「边界」）。

## 可选方案与取舍

| 议题 | 方案 | 取舍 |
|---|---|---|
| 现状文档载体 | A：**新建 `docs/architecture.md`**（现状/历史分离，design.md 退役） | **A（用户已选）**：单一角色清晰；design.md 保留历史价值 |
| | B：改造 design.md 为活文档 | 与"定稿勿再推翻"定位冲突，且正文混任务清单/缺口，改造面大 |
| 作废目录处置 | A：**删除 + 索引一行**（git 已留痕） | **A（用户已选）**：决策先入索引再删，防重复提案 |
| | B：移 archive/ | 多一层归档结构，8 个完成目录是否同迁无标准 |
| 作废目录名标识 | A：删除为主，目录名不设状态位 | — |
| | B：死目录一律保留并改名加状态后缀 | 目录树成完整历史陈列，但死文本残留需靠标记防误读 |
| | C：**混合**——默认删除 + 索引留痕；特殊需陈列的个案保留并改名加后缀 | **C（用户已选）**：常态干净零残留，特例可见；后缀不破坏排序 |
| 变更目录命名 | A：现状 `{date}-{type}-{title}` + 同日冲突加 `-2` | 无唯一性保障、无稳定引用句柄、补丁式 |
| | B：**`{YYYYMMDD}-{NN}-{type}-{title}`**（同日紧凑日期 + 序号 + 类型 + 标题） | **B（用户已选）**：NN 保证同日唯一；紧凑日期保持排序；type/title 供人读；NN 由 `new-change.sh` 分配，废弃不重用 |
| 变更号形态 | A：纯净号 `{YYYYMMDD}-{NN}` + 引用必配标题（ticket/SHA 模式） | 稳定最强，但引用处不可读、需「禁裸号」纪律；纯号模式服务于标题频繁变动的对象（issue/ticket），本项目引用对象 = 已冻结的完成变更 |
| | B：**变更号 = 目录 slug（`{YYYYMMDD}-{NN}-{type}-{title}`），引用直接用 slug** | **B（用户已选）**：引用自可读、零纪律成本；本项目生命周期锁死"完成目录冻结不改名"，slug 失稳概率极低——为小概率付"每次引用不可读"代价不划算；接受 ③ 改名/删除令旧引用失效（低频，作废引用走叙述/索引行） |
| 命名固化深度 | A：仅脚本自动命名 + AGENTS 硬规则 | 规则靠自觉（advisory）——本项目实测过规则文本被静默违反（AGENTS 迁移只做一半） |
| | B：**脚本算名 + `check` 校验子命令**（创建前自检 + 可独立调用） | **B（用户已选）**：确定性门禁层（playbook advisory vs deterministic 在 docs 侧的落地），约 30 行 bash 零依赖 |
| | C：另加 git pre-commit hook | 每次提交自动拦截，最严；需装 hook 且与现有无 hook 工作流不一致——预留为未来选项，本次不做 |
| 变更索引位置 | A：**`docs/changes/README.md`**（目录就地自然入口） | **A**：目录即入口，文档地图只需指目录 |
| | B：顶层 `docs/changelog.md` | 顶层文档 +1，与文档地图/命名规范多一处维护 |
| design.md 正文漂移 | A：**仅加头部降级声明**（YAGNI，退役文档不逐节修） | **A**：时间花在 architecture.md 而非维护退役文档 |
| | B：全量修订正文 | 大投入低回报，修订完即退役 |
| 独立评审实现 | A：**流程内硬步骤**（新上下文实例评审，本仓内建） | **A**：零外部依赖；与 playbook verifier subagent（fresh context 防自评偏置）一致 |
| | B：引入外部 AI reviewer 服务 | 依赖外部工具，与本地工作流耦合度低 |
| 现状事实来源 | A：**从代码对账重建** | **A**：防"漂移复读"——从旧文档抄会把错带进新文档 |
| | B：从 design.md 抄录现状 | 当前漂移即此类复读产物 |

## 方案

### 0. 删除作废目录（先索引化）

删除 `docs/changes/2026-08-29-opt-annotation-interceptors/`（design.md + progress.md）——**处置维持删除（用户两次确认）**，该目录属「未实现即被否决」形态，删除最干净（git 保留全文，索引行承担记忆，目录名标记对此无适用对象）。删除前将其一行摘要写入新索引（§2），注明：状态=作废-已删；被 `opt-core-first` 取代；否决教训"不为简单开关建抽象（拦截器钩子方案）"；**更正**：其自称保留决策"@as 移除"未落地（代码保留 @as），作废变更的决策一律以 architecture.md/代码为准。**作废目录删除后不再被引用**——后续提及一律用叙述标题或指索引行（§2 引用规则）。

### 1. 新建 `docs/architecture.md`（现状单一事实源）

- **定位**：只描述当前代码真相。头部声明：随每次变更收尾同步（蒸馏步骤）；`docs/design.md` 为 2026-08 历史定稿基线，冲突以本文件为准。
- 内容（**从代码对账产出**，非从旧文档抄）：
  1. 模块与依赖方向：`cli → core → tree`
  2. 运行管线（现版，无拦截器）：DDL → `DruidDdlParser` → `StatementApplier`（apply + @ignore 模型剪枝 `pruneIgnored`）→ `CodeGenerator`（产物配置顺序，逐表×逐产物）→ 生成器（查询契约 `fieldType`；`AbstractJavaGenerator` 只 reconcile @Generated 成员）→ `FileWriter`（字节比对，无变化不写盘）→ 文件 + ChangeReport；drop 删文件 / rename 保留旧文件
  3. 生成器体系：`Generator` SPI 签名；内置生成器清单（Entity/Enum/Pojo/Mapper/MapperXml/Repository/RepositoryImpl/Converter）；注册与多实例/缺省规则
  4. 注解体系：语法 `@name[:value]`；内置 handler type/as/ignore（`AnnotationRegistry.builtin`）；`@type` 由产物 type 特性消费；`@as` 列/表级命名覆盖（`TableContext`）；`@ignore` 模型级剪枝；`annotations.custom` 自定义
  5. 产物与特性开关 config schema：`artifact.*.generator/package/suffix/target/source/mapper/converter` + 布尔特性（lombok/jsr303/enums/type/serializable…）；`naming.*` 保留命名空间
  6. 关键契约（现役）：config+DDL 为准 / 删除无条件 / @Generated 成员所有权 / 解析失败不覆盖（--force 逃生口）/ 无 manifest / rename 保留用户代码
  7. 命名与类型映射速查；**关键代码锚点表**（类 → 路径，供 AI 跳读与对账）

### 2. 新建 `docs/changes/README.md`（变更索引 + 生命周期规则 + 引用规则 + 自检用例集）

- 表列：**变更号（slug，链到变更目录）** | 状态 | 一句话摘要 / 关键决策
- 预填现有变更（对账时从各目录 design/progress 提炼一行）：**存量 8 个目录以原目录名为其变更号**（保留 `YYYY-MM-DD` 原名不改——append-only 决策记录引用兼容、避免 churn），不另造编号；`annotation-interceptors` 行保留（状态=作废-已删，无目录链接，教训入行）
- 固定节：
  - **本目录生命周期规则**：
    - 变更收尾 → 追加一行并蒸馏；**批准/否决痕迹仓库化**——用户评审通过 = 变更目录 progress.md 状态 ✅ + 索引行状态「完成」；否决/作废 = 索引行状态「否决/作废-已删」+ 一句教训（评审结论不留在对话里，仓库即审计记录）
    - **作废处置三态**：① 未实现即否决 / 整体作废 → 默认**删除** + 索引行（git 保全文；死文本零残留）；② 完成但被**部分**取代 → 目录保留原名（部分失效无法二元标记，改名还会破坏 append-only 历史引用），索引行注明「部分被 X 取代」+ 指向取代变更的变更号；③ 特殊需要**保留陈列**的死目录 → 改名加**状态后缀** `-rejected`（否决/作废未实现）或 `-superseded`（完成后被取代）——后缀不破坏排序、符合小写连字符命名。**目录名状态后缀仅 ③ 一种适用场景**，是叠加在索引行之上的防误读信号，不替代索引
  - **引用规则**：文档内引用变更用**变更号（目录 slug）或其相对链接**——slug 自带 type+title，引用处自可读，无"裸号"问题。**已删除的作废变更不引用目录**（目录已不存在）：提及时用叙述标题或指索引行（索引行永存，教训不丢）
  - **记忆文档自检用例集**（playbook continuous-evals 的轻量版，用户拍板吸收）：固定一组走查问题——
    *现状类*：当前注解集与各注解语义？内置生成器清单与 SPI？运行管线各环节类名？config 顶层键 schema？核心契约（@Generated 所有权等）？
    *历史类*：曾否决/曾作废的方案与教训？最近几次变更改了什么？
    执行规则：**任何记忆文档（AGENTS.md / SKILL.md / architecture.md / 本索引 / glossary）被改动后，由新上下文实例只喂记忆文档作答，答案与 architecture.md/代码不符即不合格**，修复后才算收尾。答错 = 要么文档没触发、要么文本已漂移——与本项目实测漂移（拦截器已删仍在文档、@as 决策未落地）同源对症
  - **读取指引**：新变更设计前先读本文件（曾做/曾否决检索；变更号 = 目录 slug 一览）；变更目录的创建与命名校验走 `new-change.sh`（§6），**禁止手工 mkdir**

### 3. `design-first` SKILL.md 升级 + AGENTS.md 同步

新流程 10 步：1 分析 → 2 脚手架 → 3 设计 → 4 SOLID 自评 → **5 独立评审 pass** → 6 用户评审（硬关卡）→ 7 实现 → 8 验证 → **9 一致性核对** → **10 蒸馏收尾**

- **第 2 步（脚手架）强化**：变更目录一律由 `new-change.sh <type> <标题>` 创建（脚本算名、分配变更号、注入头部元信息）；**禁止手工创建变更目录**
- **第 5 步规格**：
  - 触发：中/大改动（小改动豁免，commit message 的分析+取舍不受影响）
  - 方式：**新上下文实例**（新会话或 subagent）评审，只带输入 = `design.md` + `docs/architecture.md` + `docs/progress.md`（决策记录）+ `docs/changes/README.md`
  - 任务：输出「与项目现状/历史冲突清单」——含查索引（本方案/相似方案是否曾做、曾否决）、影响面符号是否可 grep 验证、是否与 architecture.md 现役机制冲突
  - 结果：冲突清单回写 design.md；不通过回第 3 步
- **第 6 步落痕**：用户评审结论写入变更目录 progress.md（通过 = 状态 ✅；否决/作废 = ❌ + 理由），不留在对话
- **第 9 步规格**（一致性核对，playbook compliance-pass 轻量版）：对照 design.md「改动文件与影响面」「类职责与交互」逐条核查实现偏差（多做了/少做了/做法与设计不符），偏差记入 progress.md「实现偏差」；有实质偏差须回第 7 步修正或回第 6 步改设计
- **第 10 步规格**（蒸馏收尾）：10a 决策 → `docs/progress.md`「关键决策记录」（引用相关变更用变更号 slug）；10b 现状变化 → `docs/architecture.md`（更新对应条目，非散文追加）；10c 索引行 → `docs/changes/README.md`（作废/被取代的既有目录按 §2 三态规则处置）；10d 目录处置（完成保留；作废默认删除，删除前先完成 10a-10c）；10e 本次若改动记忆文档 → 跑「记忆文档自检用例集」（新上下文作答）通过；10f 跑 `new-change.sh check`（命名校验，确定性门禁）
- 新增「设计评审检查单」节（SOLID 之外）：影响面写具体符号（类名/文件/生成器，评审可 grep）；命名查 glossary；复用查证（既有生成器/工具/类型）；查变更索引历史

AGENTS.md 同步点：
- 「开发工作流」：与 SKILL.md 对齐为 10 步概要（SKILL.md 是细则、AGENTS.md 是概要，双写不打架）
- 「文档命名与组织规范」：变更目录命名改为 `{YYYYMMDD}-{NN}-{type}-{标题}`（**变更号 = 目录 slug**；NN = 当日序号，由 `new-change.sh` 分配，废弃不重用；**移除"同日冲突加 -2"**；**变更目录只允许 `new-change.sh` 创建，禁止手工 mkdir**，命名与创建即脚本固化）；补变更目录生命周期规则（批准/否决痕迹、作废处置三态、**状态后缀保留词 `-rejected`/`-superseded` 仅供保留陈列个案，默认作废删除**）；`docs/changes/README.md` 固定角色（索引 + 生命周期规则 + 引用规则 + 自检用例集）
- 「文档地图」表：加 `architecture.md`（现状架构，变更收尾同步）、`changes/README.md`（变更索引）；`design.md` 定位改「2026-08 历史定稿基线，现状以 architecture.md 为准」
- 「项目架构」节：管线去「拦截器」字样；DDL 注解行与现状对齐（@as 在列/表级仍生效的语义）；扩展点表核对

### 4. 降级与漂移修复（精确清单在对账后产出）

- `docs/design.md`：头部加声明块——历史定稿（2026-08）、部分已被后续变更取代、现状以 `architecture.md` 为准、勿作现状依据。正文不逐节修订
- `AGENTS.md`：如上 §3
- `docs/glossary.md`：DDL 注解等条目引用从 design.md §9 改指 architecture.md
- `docs/tasks.md`：头部加注「M0-M4 历史任务清单（已完成）；与现行架构冲突处以 architecture.md 为准」
- `README.md`：仅当对账发现与代码冲突时修订（用户手册，保持现状准确）
- `docs/progress.md`：追加本次决策记录（三分离文档角色 + 生命周期 + 变更号 slug 规范 + 命名固化 + 独立评审 + 一致性核对 + 蒸馏收尾 + 自检用例集 + 目录名状态后缀约定）

### 5. 本次变更目录

`docs/changes/20260905-01-chore-project-memory/`（实现首步把现目录 `2026-09-05-chore-project-memory` git mv 到新命名，成为新规范的第一个采用者；评审期间仍用现名）。收尾时执行第 9/10 步：一致性核对 + 蒸馏 + 索引行 + 自检 + 目录保留。

### 6. 变更目录命名、变更号与脚本固化规范

- **目录名 = 变更号（slug）**：`{YYYYMMDD}-{NN}-{feat|opt|fix|chore}-{标题}`，唯一标识一个变更，同时承载可读性（type/title）。**NN = 当日两位数序号**（01 起，同日内按创建顺序递增；**一经分配不可变，作废/删除后不重用**——留洞不补齐，保证同一日不出现两个同号变更）
- **引用 = 变更号（slug）或其相对链接**：slug 自带标题，引用处自可读，无需配对规则或裸号纪律。稳定性成立的前提是本项目生命周期规则——**完成目录冻结不改名**（存量目录不补改名、完成目录不更名），引用对象基本不变；业界纯号模式（ticket/SHA）服务于标题频繁变动的对象，本项目不需要为此付可读性代价
- **日期紧凑表示**：`YYYYMMDD` 无分隔（如 `20260905`），更贴合"编号"语义且仍保持字典序（同日序号断点清晰）；`new-change.sh` 日期取 `$(date +%Y%m%d)`
- **已知失效风险（接受）**：目录唯二变异 = ③ 保留陈列加后缀（改名当次即更新引用）与作废删除（引用规则：删除后不引用目录，走叙述/索引行）。两者均低频；③ 改名会让更早的 append-only 引用失效，作为历史叙述接受（此类引用本就指向当时存在的目录）
- **存量迁移**：现存 8 个目录**不改名**（保留 `YYYY-MM-DD` 原名，原名即其变更号；目录树呈现新旧两种日期格式是迁移期预期）；索引行以原目录名为标识，不另造编号
- **`new-change.sh` 固化（命名权威）**：目录名由脚本**算出**而非人手拼出——消除"手工拼名 + 事后补丁"整类错误：
  - 日期 `$(date +%Y%m%d)`；NN = 扫 `docs/changes/{DATE}-*` 正则解析（`^{DATE}-([0-9]{2})-`）取当日最大 + 1，`%02d` 补零；同名存在报错
  - **模板注入变更号元信息**：生成的 design.md 头部自动写入「变更号 / 目录 / 变更类型」，后续实现、评审、蒸馏引用同一 slug，不靠手抄
  - 输出变更号、路径与下一步；命名校验兼容 2 位序号与 type 白名单（feat|opt|fix|chore）
  - `check` 子命令（`new-change.sh check`）：校验全仓 `docs/changes/` 目录命名——新格式 `\d{8}-\d{2}-(feat|opt|fix|chore)-[a-z0-9-]+` 或存量 `\d{4}-\d{2}-\d{2}-(feat|opt|fix|chore)-[a-z0-9-]+`、同日 NN 唯一、无漏网的手工目录；**创建前自动自检 + 可独立调用**（第 10 步收尾跑）。确定性门禁层（playbook「skill advisory / hook deterministic」在 docs 侧的落地）：AGENTS 规则是 advisory，`check` 是 deterministic
  - 硬规则进 AGENTS/SKILL：变更目录只允许 `new-change.sh` 创建，禁止手工 mkdir（§3）
  - pre-commit hook **本次不做**（边界，未来可挂 `check`）
- 上限：同日 >99 个变更超出两位数时升位（实际不可能，不处理）

## 文档职责与关系（「类职责与交互」的文档化对应）

| 文档 | 一句话职责 | 更新时机 | 依赖/指向 |
|---|---|---|---|
| `AGENTS.md` | 团队/工作流/命名惯例 + 文档地图（入口） | 低频（流程/约定变化） | 指向 architecture.md、changes/README.md |
| `docs/architecture.md` | **现状单一事实源（is）** | 每次变更蒸馏收尾 | 事实来源 = 代码对账（非 design.md）；被 design.md 声明指向 |
| `docs/design.md` | 2026-08 设计定稿历史基线（was） | 不再更新（除头部声明） | 指向 architecture.md |
| `docs/progress.md` | 关键决策台账（why，append-only，引用用变更号 slug） | 每次变更蒸馏收尾 | — |
| `docs/changes/README.md` | 变更索引（what/when，变更号↔目录一览）+ 生命周期规则 + 引用规则 + **记忆文档自检用例集** | 每次变更收尾追加行；记忆文档改动后跑自检 | 链接各变更目录；作废变更的常驻记录 |
| `docs/changes/{变更号}/` | 单次变更工作文档（设计/执行/状态/实现偏差）；目录名即变更号 | 变更期；批准/否决痕迹落 progress.md | 收尾蒸馏（内容向上沉淀），随后处置 |
| 死目录（保留陈列个案） | 历史陈列；目录名加 `-{rejected\|superseded}` 后缀 | 改名当次 | 索引行是主通道，目录名标记是叠加信号 |
| `new-change.sh` | 变更目录命名权威 + 校验（`check` 子命令） | 命名规范变更时同步 | 生成 design.md（模板注入变更号）；AGENTS/SKILL 硬规则指向它 |

数据流：
- 读取路径（AI）：会话启动 AGENTS.md → 设计/实现前 architecture.md（现状）+ changes/README.md（历史/变更号一览）+ progress.md（决策）
- 写入路径（变更收尾）：一致性核对（实现 vs design.md）→ 蒸馏（决策→progress.md；现状→architecture.md；索引行→README.md）→ 自检（涉及记忆文档时）+ `new-change.sh check` → 目录处置
- 引用路径：文档内变更号 slug（或其链接）→ 直接定位变更目录；作废变更 → 叙述标题或索引行

SOLID 类比自评：
- **SRP**：每份文档单一角色，现状/决策/索引/惯例不混装——解决 design.md 一人多角（规格+任务+缺口+决策）
- **OCP**：新增变更 = `new-change.sh` 创建 + 索引追加行 + 现状条目更新，历史 append-only，不改旧结构
- **DIP**：architecture.md 的事实来源是代码（对账），不是 design.md——防漂移复读；蒸馏方向单向自变更目录向上，低层不决定高层
- **替换一致性**：design.md 退役后不再假装权威，所有引用统一改指 architecture.md（无残留旧指针）；变更引用统一走变更号 slug

## 改动文件与影响面

- 删除：`docs/changes/2026-08-29-opt-annotation-interceptors/`（先索引化）
- 新增：`docs/architecture.md`、`docs/changes/README.md`
- 重命名：`docs/changes/2026-09-05-chore-project-memory/` → `docs/changes/20260905-01-chore-project-memory/`（实现首步）
- 修改：`.agents/skills/design-first/scripts/new-change.sh`（`%Y%m%d` 日期 + NN 扫描分配 + 模板注入变更号 + 输出 + `check` 子命令）、`.agents/skills/design-first/SKILL.md`（第 2 步强化 + 10 步流程 + 检查单）、`AGENTS.md`、`docs/design.md`（头部）、`docs/glossary.md`（指针）、`docs/tasks.md`（头部注记）、`docs/progress.md`（决策追加）、`README.md`（按对账结果）
- 影响：
  - 变更目录命名规范变更：新变更一律 `{YYYYMMDD}-{NN}-{type}-{title}`（变更号 = slug，脚本分配）；同日冲突从补丁（-2）变为结构化序号
  - 引用方式变更：决策记录/cross-link 引用变更号 slug（可读、无需配对规则）；存量目录原名即其变更号；作废变更删除后走叙述/索引行
  - 命名固化：`new-change.sh` 成为命名权威（算名 + check），手工建目录被 AGENTS 硬规则禁止、被 check 拦截
  - 未来所有变更：中/大改动新增「独立评审」与「一致性核对」，所有变更收尾新增「蒸馏」+「自检」+「check」步骤（SKILL.md 与 AGENTS.md 保持同步）
  - 无 Java/构建影响；docs 不参与 mvn 门禁，但 AGENTS 文档规范须自洽
- 边界（非目标）：
  - 不改 design.md 正文、不做 archive/ 归档（直接删除 + 索引留痕）、README/tasks/glossary 仅修与代码冲突处
  - **不为存量 8 个目录补改名**（原名即变更号，保留 `YYYY-MM-DD` 格式；append-only 引用兼容）；不为已完成目录补加状态后缀
  - **不引入 git pre-commit hook**（`check` 子命令 + 收尾步骤已覆盖，hook 是未来可选加固，本仓无 hook 工作流）
  - 不引入外部工具/依赖；不吸收 playbook 的重机制——CI evals 基建（20-50 任务 + API key + 定时跑，用轻量自检用例集替代）、hooks 批准门禁、managed settings/sandbox（企业监管场景）、托管 Code Review/扫描服务、Stage 6 自治维护环、Bugs/Security/Compliance 多 pass 分工（单人 + agent 规模无此维度）

## 验证

1. **对账自查**：按 architecture.md「代码锚点表」逐条核对（类存在；管线/注解集/config 键/契约与代码一致）
2. **交叉一致性**：grep 全仓 md——「拦截器/use 链」不再作为现状被描述（仅允许历史注记）；对 design.md §9 等旧指针引用清零
3. **命名校验**：`new-change.sh check` 全绿——存量目录以存量格式通过、新目录符合 `{YYYYMMDD}-{NN}-{type}-{title}`、同日 NN 唯一；全文无对已删目录（annotation-interceptors）的活链
4. **自检用例集建立并跑通（关键验收）**：在 changes/README.md 建好「记忆文档自检用例集」，用新上下文实例只喂 AGENTS.md + architecture.md + changes/README.md + progress.md 跑一遍——答错即文档不合格（文档服务的对象就是 AI，须用 AI 验收）；此后每次记忆文档改动即回归
5. mvn 门禁：无 Java 改动，全量 `mvn clean test` 回归确认未动代码
6. 本次变更自身执行第 9/10 步：一致性核对 + 蒸馏 + 索引行 + 自检 + `check` + 目录保留，作为新流程首次实弹

## 执行进度（progress.md 占位）

- [ ] 设计稿用户评审通过（本轮待审）
- [ ] 后续另起一轮实现（对账 → 写文档 → 改 skill/new-change.sh/AGENTS → 目录改名 → 验证 → 一致性核对与蒸馏收尾）
