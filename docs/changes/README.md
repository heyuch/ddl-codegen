# docs/changes 变更索引

本目录存放**单次变更的工作文档**（`{变更号}/design.md` + `progress.md`，变更号 = 目录 slug）。

状态取值：`完成` / `否决` / `作废-已删` / `完成（归档）`（纯历史归档，如初始建设期）。进行中的变更不在表中，收尾时由工作流自行追加索引行。

## 变更索引

| 变更号（slug，链到变更目录）                                                                          | 状态 | 一句话摘要 / 关键决策 |
|-------------------------------------------------------------------------------------------|---|---|
| [20260801-01-feat-project-foundation](20260801-01-feat-project-foundation/)               | 完成（归档） | **初始建设期归档**（2026-08：M0-M4 + 技术设计定稿）：2026-09-05 随 `20260905-01-chore-project-memory` 归档动作，原 `docs/design.md`（技术设计定稿）+ `docs/progress.md`（进度台账/关键决策/PIT/已知限制原文）+ `docs/tasks.md`（M0-M4 任务清单）自顶层迁入；前向「已知限制」迁 `docs/architecture.md`「已知限制」节；决策史 = 早期于本目录、2026-08-29 起各变更目录 + 本索引（无顶层台账） |
| [20260829-01-feat-maven-plugin](20260829-01-feat-maven-plugin/)                           | 完成 | 新模块 `ddl-codegen-maven-plugin`：`GenerateMojo`（projectRoot / configFile / ddl / ddlFile:行范围 / dryRun / skip，不绑生命周期，显式 `mvn ddl-codegen:generate`）；从 CLI `Main` 提取 `Codegen` 门面入 core——CLI/插件共用单一管线，修一处两边生效；config 默认名统一 `ddl-codegen.properties`；3 个 maven-invoker 集成测试（it-simple / it-range / it-inline） |
| [20260829-02-chore-archunit-rules](20260829-02-chore-archunit-rules/)                     | 完成 | 引入 ArchUnit 架构测试 4 规则入 mvn test（叶子无依赖 / 单向分层 / 实现层约束 / 无循环）；`GeneratorInterceptor` 接口从 interceptor 包归位 `gen`，打破实测的 gen↔interceptor 包循环；cli/plugin 门面防绕过规则（不得直连 gen/tree）随后随 maven-plugin 变更补齐 |
| [20260829-03-feat-parameterized-artifacts](20260829-03-feat-parameterized-artifacts/)     | 完成 | artifact = 生成器具名实例 + 配置：config 顶层键改为产物名（`generator` / `source` / `target`，`naming.*`/`annotations.*` 为保留命名空间）；跨产物引用显式或「该生成器唯一实例」缺省，无唯一实例明确报错；新增 `enums`/`jsr305` 拦截器；删 `TypeMapper.isEnumArtifact`、`TableContext.typeOf` 等硬编码（breaking config，单轨迁移） |
| ~~20260829-04~~（洞，不重用）                                                                    | — | 占位 = 已删除的 `2026-08-29-opt-annotation-interceptors`（作废，见下行；NN 留洞不重用的首次实证） |
| annotation-interceptors（原 `2026-08-29-opt-annotation-interceptors`）                       | 作废-已删 | 目录已删除（git 保留全文）。**被 refactor-core-first 取代**（未实现即否决）；否决教训：**不为简单开关建抽象**（拦截器钩子方案）；**更正**：其 progress.md 自称保留决策「@as 移除」**未落地**——代码保留 @as（AsHandler 与 meta 读取仍在），其另两条保留决策（注解全存不处理、@type 独立于 enums）由 refactor-core-first 落地；**作废变更的决策一律以 architecture.md / 代码为准，不以其目录为准** |
| [20260829-05-refactor-core-first](20260829-05-refactor-core-first/)                       | 完成 | 第一性原理简化（核心优先）：删 `GeneratorInterceptor` SPI + use 链 + 4 个拦截器，lombok/jsr303/jsr305/enums/type/serializable 改为产物特性开关（`entity.lombok=true` 式）由生成器内部应用；@ignore 模型级剪枝（解析后移除）；查询契约 className/fieldName/fieldType 统一跨产物类型查询；唯一扩展点收敛为 ArtifactGenerator；annotation-interceptors 设计作废 |
| [20260830-01-chore-spotbugs](20260830-01-chore-spotbugs/)                                 | 完成 | 引入 spotbugs-maven-plugin 4.8.6.8（SpotBugs 4.9+ 需 Java 17，构建跑 Java 11 → 锁 4.8.x 线），effort=Max / threshold=Low / check 绑 process-classes 进 mvn test；首轮 triage 修真 bug（NPE 路径、死代码）+ exclude 逐条带理由（终态仅 1 条）；随迁 checkerframework 强化为 error 级（去 `-Awarns`、@Nullable/@MonotonicNonNull 注解化、jsr305 迁 checkerframework @Nullable） |
| [20260830-02-chore-jacoco](20260830-02-chore-jacoco/)                                     | 完成 | 引入 JaCoCo 覆盖率门槛：prepare-agent/report/check 全进 `mvn clean test` 生命周期；阈值全局 BUNDLE line ≥ 75%（基线实证后定稿呈报）；cli 原覆盖率 0% → 补 MainTest 拉到 87.7%；与 PIT 互补（JaCoCo 量化保底、PIT 深度验证） |
| [20260830-03-refactor-applyone-polymorphism](20260830-03-refactor-applyone-polymorphism/) | 完成 | `StatementApplier.applyOne` 10 分支 instanceof if-else → 多态分发：`DdlOperation` 接口加 `apply(Schema, ApplyResult)`，10 个操作类各自实现 apply、应用逻辑与操作同居，applyOne 收敛为一行；纯结构重构语义等价，StatementApplierTest 行为断言不变 |
| [20260905-01-chore-project-memory](20260905-01-chore-project-memory/)                     | 完成 | 项目记忆体系重构：文档角色三分离（`docs/architecture.md` 现状单一事实源[代码对账产出，10 条对账发现入档] / `docs/design.md` 降级历史基线 / `docs/progress.md` 决策台账）；本 README = 变更索引 + 生命周期规则 + 引用规则 + 自检用例集；变更号 = 目录 slug `{YYYYMMDD}-{NN}-{type}-{title}`（new-change.sh 命名权威 + `check` 门禁，禁手工 mkdir）；design-first skill 升级 10 步（独立评审 pass / 一致性核对 / 蒸馏收尾）；作废处置三态 + 状态后缀；删除作废目录 annotation-interceptors 并更正其"@as 移除"未落地；顶层 design/progress/tasks 归档入 `20260801-01`；存量目录统一迁移为新格式 |
| [20260906-01-feat-enum-annotation-code-desc-template](20260906-01-feat-enum-annotation-code-desc-template/) | 完成 | **`@enum` 注解枚举列 + code/desc 模板（breaking）**：非 SQL-enum 列经列注释 `@enum[:类名]` + 枚举项列表 `{code}={desc}({name})` 生成枚举类（内置注解 `@enum`/EnumHandler，`Column.isEnumColumn()`）；SQL `enum(...)` 列统一迁移 code/desc 模板（常量 code=存储值 + desc，`fromCodeNullable`/`fromCode` 反查对，`@Nullable` 用 `annotations.nullable`）；产物选项 `enum.lombok`（true → `@Getter`+`@RequiredArgsConstructor`，缺省手写构造/getter）；跨产物接入 `enums=true`/converter（视图判定改枚举类 FQN 精确比对 + `fromCode`/`getCode` 桥接）；reconcile 扩展：enum 常量签名含 init 原位替换、手写同名成员跳过新增守卫；tree：解析侧常量 kind、空枚举 `;`、init 规范化（含 unicode 解码）；用户多轮调整（lombok 双形态/裸 @enum 与 enum column 同名/反查方法对/@Nullable 可配/ALTER 增量） |
| [20260906-02-chore-generator-focused-tests](20260906-02-chore-generator-focused-tests/) | 完成 | **生成器 golden 契约测试（7 生成器全覆盖）**：`GeneratorTestSupport`（整文件比对 + `-Dgolden.update=true` authoring 不静默）；Mapper/MapperXml/Repository/MybatisRepositoryImpl/Converter/Pojo/Enum 各建 golden 场景（多列最左前缀拆分、List 返回、无主键/无唯一键、非自增 insert、`di=constructor`、converter 半边负例、特性单开矩阵、SQL-enum 列表补全、lombok 两形态、错误组）；`EnumAnnotationTest` 职责收敛为 `@enum` 注解特性（视图/桥接、ALTER 增量+手写保留）；发现并记录：保留字反引号未剥（另立 fix）；PIT gen survived 49→13（XML 25→3 / Pojo 9→2 / Converter 5→1） |
| [20260906-04-fix-generated-code-compilable](20260906-04-fix-generated-code-compilable/) | 完成 | **生成代码可编译修复**（03 模块真实 javac 驱动发现）：`javax.annotation.processing.Generated.value` 必填 → `GeneratedSupport.mark` 显式 `@Generated("ddl-codegen")`（此前裸注解全部生成成员编译失败）；`AbstractJavaGenerator` 增 `finalClass()`，impl/converter 生成 final 类（DesignForExtension）；02 fixtures 全量重出（124 行产物契约更新）；core 119 绿；记录边界：类级修饰符不 reconcile，升级需删文件重生成 |
| [20260906-05-chore-generator-test-ddl-fixtures](20260906-05-chore-generator-test-ddl-fixtures/) | 完成 | **golden 测试 DDL 输入入 fixtures**：`fixtures/gen/<case>/input/*.sql`（按文件名序、共享 Schema）为各 case 输入事实源；harness 增 `generateAndAssert(Subset)`；7 套件成功场景 DDL 全部迁入（30 文件），方法瘦身为 config+断言；错误路径 DDL 保留内联；core 119 绿 |
| [20260906-07-fix-generator-leftover-fixes](20260906-07-fix-generator-leftover-fixes/) | 完成 | **02/04 遗留 fix 一并解决**：反引号标识符 Java 侧剥除（`NamingService`，`` `order` `` → `order_`，保留字检测恢复；XML/SQL 列原文保留）；pk 列名≠`id` 链路（Mapper `deleteById` 参数注解改字段名派生、XML 去硬编码 `findById` select，PRIMARY spec 天然产 `findBy<IdPascal>`）；impl 单值 findBy 空安全桥接（初版 `call == null ? null : toX(call)`，后经 20260906-03 细化为临时变量一次调用守卫）；恢复 Pojo 保留字 golden + NamingServiceTest 反引号用例；core 121 绿。**A 部分（反引号/保留字处置）后经 20260906-11 取代**：引号在 DDL parse 层剥除、Java 命名只对 Java 关键字负责、MySQL 保留字由 MapperXmlGenerator 加引 |
| [20260906-11-fix-identifier-hygiene](20260906-11-fix-identifier-hygiene/) | 完成 | **标识符卫生职责分层（取代 07 的 A 部分处置）**：反引号在 DDL parse 层单点剥除（`DruidAst.nameOf/columnNames` SQLIdentifierExpr 分支实证保留引号、补剥；新增 `DruidAst.columnName`），模型恒存真名；`NamingService.RESERVED_WORDS` 收窄为 Java 关键字 `JAVA_KEYWORDS`（删 MySQL 词表与全部反引号 strip，`order` 字段原名直出、`class` → `class_`）；MySQL 8.0 官方 RESERVED 全集内置于 `MapperXmlGenerator` + `sqlName()`（表名/列引用/列清单/SET/WHERE 加引，resultMap column 与 keyColumn 元数据不加引）；测试：`DruidDdlParserTest.quotedIdentifiersStoredClean`、`pojo/reserved-word` golden（order/class_）、新增 `xml/reserved` golden（`` `order` `` 表与 `group`/`order` 列 SQL 加引契约）；architecture 命名速查同步；全量门禁绿 |
| [20260906-12-chore-design-quality-checklist](20260906-12-chore-design-quality-checklist/) | 完成 | **设计流程完善（战略/战术两级设计自评）**：SKILL 步骤 4「SOLID 自评」→「设计自评」，新增「设计自评检查单」——先**战略**（架构融洽：归属层[反例实证 07→11]/架构不变量/复用优先/波及面/与历史决策），后**战术**（SOLID / GRASP 九原则逐项判据 / 设计模式取舍：能否用、哪种、简化/复杂了什么、值得吗，不用模式也是结论；教训：不为简单开关建抽象）；初版「语义归层」独立成项经评审认定属职责不清的结果（过拟合）→ 撤销，教训并入战略「归属层」；步骤 5 独立评审同此视角；TEMPLATE 增「战略定位」必填节 + 指针式提示；glossary 增 GRASP/设计模式两行；new-change.sh 同步；纯文档、记忆自检合格 |
| [20260906-06-feat-generated-code-javadoc](20260906-06-feat-generated-code-javadoc/) | 完成 | **生成代码 javadoc（类/方法/属性三层）**：`CommentDocs` 清洗（剥注解/枚举项 token）；pojo 类=表注释/字段=列注释；enum 类=表注释(退列注释)/常量=desc/字段与方法模板；mapper/repository(QueryMethodFactory)/impl/converter 类=表注释、方法摘要（「按 X 查询」/「把 X 转成 Y」）；fixtures 重出（22 文件 +402 行）审阅；core 121 绿 |
| [20260906-03-chore-springboot-integration-sample](20260906-03-chore-springboot-integration-sample/) | 完成 | **真实 Spring Boot 消费工程（生成代码可编译/装配/调用验证）**：模块 `ddl-codegen-it-springboot`（Boot 2.7.18 + mybatis-starter 2.3.2 + MySQL[Testcontainers 1.21.x，无 Docker 自动 skip]）；DDL/config 全链路生成提交入库；`@MapperScan`+Bean 显式装配；IT 覆盖 mapper 存 code → repository+converter 还原枚举。**门禁=生成器质量验收器**（用户决策，分级开放）实证发现并修复：lombok @Builder 需 spotbugs-annotations、di=constructor 缺字段声明、单值 findBy 守卫二次调用、EI_EXPOSE_REP2 误报豁免（带理由）；04（@Generated/final）由此驱动 |
| [20260906-08-chore-annotation-style-warning-gate](20260906-08-chore-annotation-style-warning-gate/) | 完成 | **编译告警强制门禁（`-Werror`）+ 注解书写规范化**：checker `type.anno.*` 30 处源码根治（type-use 注解修饰符后紧邻类型、声明注解在前；AGENTS.md 增补写作约定）；StringSplitter 6 处 `split(x,-1)`；Druid 弃用 API 实为拼写错误、存在未弃用正确拼写替代 → 修复（非抑制）；JavaLangClash×2 有意命名类级 @SuppressWarnings+WHY；死参数/死字段 3 处删除；实证 `-Werror` 会提升 junit stub 主编译噪音（主类路径无 junit 属预期）→ checker `-AstubNoWarnIfNotFound` 抑制；RoundTripSmokeTest 白名单补 javac 注解位置规范化（`public @Nullable X` ≡ `@Nullable public X` 同一 AST，幂等断言不变）；全量门禁绿 + 编译期 0 告警 |
| [20260906-09-chore-demo-fixture-to-resources](20260906-09-chore-demo-fixture-to-resources/) | 完成 | **round-trip 夹具 Demo.java 移入 test resources**：`git mv` 至 `src/test/resources/fixtures/roundtrip/`（不再编译），删除随之不再需要的三处特殊处理——pom checker `-AskipDefs=hyc.codegen.tree.Demo`、pom spotless `<exclude>`（includes 仅 src/{main,test}/java，resources 天然不格式化）、checkstyle-suppressions.xml 整文件 suppression；JavaParserTest/RoundTripSmokeTest 字节全等断言改读资源路径（不变量：parse→print 字节全等不变）；static-rules M0.1 spotless 行注记消除；全量门禁绿（340 tests）+ 0 告警 |
| [20260906-10-chore-memory-doc-drift-fixes](20260906-10-chore-memory-doc-drift-fixes/) | 完成 | **08 自检遗留 3 处记忆文档漂移修正**：architecture 命名速查 `naming.enumClassName` → `naming.enum.style`（非 config 键笔误；保留 `TableContext.enumClassName` 方法名引用）；glossary design-first 词条 10 步流程复述精简为「只存源头 SKILL」指针（卫生③同一事实单源）；AGENTS.md 模块清单补 `ddl-codegen-it-springboot`（与根 pom reactor 5 模块对齐） |

## 本目录生命周期规则

- **变更收尾 → 先蒸馏，后处置，再追加一行**：决策与实现偏差沉淀到所在变更目录 `progress.md`、现状到 `docs/architecture.md`，索引行随目录处置同步登记。
- **批准/否决痕迹仓库化**：评审结论不留在对话里——用户评审**通过** = 变更目录 progress.md 状态 ✅ + 本索引行状态「完成」；**否决/作废** = 本索引行状态「否决/作废-已删」+ 一句教训。仓库即审计记录。
- **作废处置三态**：
  - **① 未实现即否决 / 整体作废 → 默认删除目录 + 索引行留痕**（git 保留全文；死文本零残留；删除前先完成索引化与蒸馏）
  - **② 完成但被部分取代 → 目录保留原名**（部分失效无法二元标记，改名会破坏 append-only 历史引用），索引行注明「部分被 X 取代」并指向取代变更的变更号
  - **③ 特殊需要保留陈列的死目录 → 改名加状态后缀**：`-rejected`（否决/作废未实现）或 `-superseded`（完成后被取代）
- **目录名状态后缀仅 ③ 一种适用场景**——是叠加在索引行之上的防误读信号，**不替代索引**（索引行永存，教训不丢）。

## 引用规则

- 文档内引用变更用**变更号（目录 slug）或其相对链接**——slug 自带 type + title，引用处自可读，无「裸号」配对问题。
- **已删除的作废变更不引用目录**（目录已不存在）：提及时用**叙述标题**或**指索引行**（如上表 annotation-interceptors 作废行）。
- 目录命名全仓统一 `{YYYYMMDD}-{NN}-{type}-{title}`（早期存量已统一迁移）；完成目录冻结不改名（原名即其变更号，append-only 引用兼容）。

## 记忆文档自检用例集

固定一组走查问题——任何记忆文档改动后的回归验收（轻量版 continuous evals）：

- **现状类（5 问）**：
  1. 当前注解集与各注解语义？
  2. 内置生成器清单与 SPI？
  3. 运行管线各环节类名？
  4. config 顶层键 schema？
  5. 核心契约（@Generated 成员所有权等）？
- **历史类（2 问）**：
  1. 曾否决 / 曾作废的方案与教训？
  2. 最近几次变更改了什么？
- **卫生类（3 问）**：
  1. 记忆文档是否含文档自证/历史叙事或维护者治理注记（具体词表见 `new-change.sh` 的 `doc-hygiene.patterns`）？
  2. 是否含流程步号指针或与工具链重复的风格描述（步号与风格只属于 SKILL 或工具配置）？
  3. 是否有同一事实的多份定义（应只存于源头文档）？
  出现即不合格；`new-change.sh check-docs` 为确定性回归（黑名单从每次清理实证追加，问句不复述词表以防自指）。
- **执行规则**：任何记忆文档（AGENTS.md / SKILL.md / architecture.md / static-rules-review.md / 本索引 / glossary）被改动后，由**新上下文实例只喂记忆文档作答**，答案与 architecture.md / 代码不符即**不合格**，修复后才算收尾。答错 = 要么文档没触发、要么文本已漂移。
