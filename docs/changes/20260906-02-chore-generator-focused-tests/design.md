# generator-focused-tests

> 变更号：20260906-02　变更类型：chore（仅测试面，不改生产代码/产物/契约）　目录：`docs/changes/20260906-02-chore-generator-focused-tests/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

7 个内置生成器此前没有各自的隔离契约测试，行为靠 `EndToEndTest`（固定 demo 表）+ `ParameterizedArtifactsTest` 黑盒承载。20260906-01 期间临时建立的 `EnumAnnotationTest` 实际承担了 EnumGenerator 的输出契约断言——职责越界：它本应只测 DDL `@enum` 注解特性（用户 2026-09-06 明确指出），EnumGenerator 的逻辑应由与其他生成器同级的 golden 契约测试承担。现有断言多为 **contains 子串**，PIT 基线（progress.md 冻结）显示 gen 包存活突变集中 MapperXmlGenerator 25 / PojoGenerator 9 / ConverterGenerator 5 / EnumGenerator 5 / MybatisRepositoryImplGenerator 4——子串断言杀不死字面量/顺序/格式类突变，固定 schema 带不出变体/边界/错误分支。

用户决策（2026-09-06）：不测生成器内部方法；**契约式 golden 比对**——控制不同 DDL/config 输入，期望产物文件固化在测试目录，与 generator 实际生成文件整文件比对；**7 个生成器全部纳入 golden**（含 EnumGenerator）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：e2e + contains + PIT 抓漏 | 改动少 | 子串断言弱（PIT 存活实证）、固定 schema 带不出分支、产物改版断言成片碎 | 否决 |
| B：测生成器内部方法 | 细粒度 | 与实现耦合，重构即碎（用户否决） | 否决 |
| C：**golden 契约测试**（期望产物文件 + 整文件比对，7 生成器全纳入） | 产物即契约：与实现解耦；字节级比对击杀字面量/顺序/格式突变；分支经输入矩阵穷举 | fixture 与产物逐字节绑定，模板变更需同步 fixture（=有意识改契约）；初始固化成本 | **选定**（用户拍板） |
| D：纯 mock 断 AST | 最细 | 失真、绕真实打印/reconcile | 否决 |

补充：整文件比对前提 = 产物打印确定性（20260906-01 已验证同输入两次输出一致），fixture 可比。

## 方案

- **测试形态**：每场景 = 「DDL + config + expected 文件集（`src/test/resources/fixtures/<generator>/<case>/expected/**`）」→ CodeGenerator 生成到 @TempDir，逐个与实际文件**整文件文本比对**（不等输出差异便于审阅）；**错误路径**断言 `IllegalStateException` 消息（含表/列/产物名），不进 golden。
- **fixture 更新流程**：产物模板/格式变更 = 契约变更 → 有意同步更新受影响 fixture 并随变更评审；不提供 auto-update 开关。
- **harness（测试 only，`gen` 包）**：`GeneratorTestSupport`——mini DdlConfig + 指定 kinds 注册 + 复用 Schema 的 apply/generate + read + `assertGolden`。注册子集：单 kind 为主；需真实视图时补注册（如 Converter 依赖 pojo fieldType 视图）。
- **新测试类与场景矩阵**（每生成器一个 golden 类，共 7）：
  - `MapperGeneratorTest`：CRUD/findById/唯一键 `@Nullable` 单值；多列最左前缀拆分；唯一键前缀→List；无主键（无 deleteById/findById）；无唯一键；`@ignore`；target=entity。
  - `MapperXmlGeneratorTest`：主路径整文件（resultMap/BaseColumnList/insert[自增排除+useGeneratedKeys]/update/deleteById/findById/findBy*）；无主键；无唯一键；多列 where 序；**非自增主键 insert**；update SET 排除主键；jdbcType 快照；`@ignore`；缺 `path` 报错（先配好 mapper+target）；空方法集冒烟。
  - `RepositoryGeneratorTest`：findBy 单值 `@Nullable` vs List；多列拆分；零方法冒烟；target 缺失报错；参数 = repository 自身 ctx 标量视图（SQL-enum `String`、`@enum` 自然类型——与代码一致；enum 参数化查询未实现记 progress 已知）。
  - `MybatisRepositoryImplGeneratorTest`：`di=field` 与 `di=constructor`；直连 vs converter 桥接（含 enum fromCode/getCode）；converter 未配置且 mapper.target≠own target 报错；converter source/target 半边不一致负例。
  - `ConverterGeneratorTest`：toX/toXList 整文件；枚举一端/两端同/无 enum 产物；nullable 空安全；source/target 缺 artifact 报错。
  - `PojoGeneratorTest`：全关；lombok；serializable；jsr303（@NotNull/@Size/@Digits）；jsr305（`@Nullable` 定制值）；enums 开/关；type 覆盖优先 enums（恰 1 enum 产物、不注册 EnumGenerator）；保留字列名（`` `order` ``→`order_`，先冒烟 Druid 反引号）；组合。
  - **`EnumGeneratorTest`（golden）**：SQL-enum 列模板（comment 无列表 → desc `""`；列表按 code 匹配补 desc/name；不匹配项不影响产物）；`@enum` tinyint 数字列（显式 name / 缺名 `STATUS_n` / Integer code）；`@enum` varchar 字符列（code 即名）；裸 `@enum` 类名（column / tableColumn）；空列表骨架（`;` + 字段 + 反查对）；`lombok=false`（手写构造/getter）与 `lombok=true`（`@Getter`+`@RequiredArgsConstructor`）两套 fixture；`fromCodeNullable`/`fromCode` body；`annotations.nullable` 定制值；bigint `L` 后缀；校验错误（同表类名撞名、`@enum`×`@type`、decimal/不支持类型、code/常量名重复、非法类名）→ 异常消息断言。
- **`EnumAnnotationTest` 职责收敛（本变更唯一允许改造的既有测试）**：删除其中与 EnumGenerator 输出契约重复的文本断言（常量文本/字段行/方法 body/lombok 形态等——由 `EnumGeneratorTest` golden 覆盖）；错误断言移交 `EnumGeneratorTest`；**保留**属于「`@enum` 注解特性端到端」的：enums=true 字段视图、converter 桥接表达式、ALTER 增量同步 + 用户手写保留、裸注解命名特性、`@as`×`@enum` 优先级联动。避免与 golden 双套件漂移。其余既有测试（EndToEnd/Parameterized/Reconcile 等）零改动。

## 边界声明（评审 ②，显式定界、不在本变更修复）

- findById/deleteById 族仅对主键列名为 `id` 的表成立（`@Param("id")`/`findById` 名硬编码，pk≠id 分叉）——疑似缺陷，记 progress 已知，不加错误探针。
- Repository enum 参数化查询未实现；pojo `@type` 单侧转换不可编译直拷；converter 同产物退化配置——记 progress 已知，不写进测试。

## 类职责与交互

- 新增：`GeneratorTestSupport` + 7 个测试类 + `src/test/resources/fixtures/**`
- 改造：`EnumAnnotationTest`（职责收敛，见上）
- 生产代码零改动；其余既有测试零改动。

## 对账与冲突清单（独立评审 + 用户决策 → 处置）

- v1 评审：「7 无隔离」「Repository enums 参数」与代码不符、pk≠id 边界、内部矛盾、PIT 基线未落地、口径窄 → v2/v3 已修。
- 用户决策 ①（golden 契约、不测内部方法）→ v3；用户决策 ②（**EnumGenerator 纳入 golden；EnumAnnotationTest 只测 `@enum` 注解特性、收回越界职责**）→ v4：新增 EnumGeneratorTest golden + EnumAnnotationTest 收敛（「既有测试零改动」仅此一文件例外，其存在即 20260906-01 临时形态）。

## 改动文件与影响面

- 改动：core `src/test` 新增（harness + 7 测试类 + fixtures）；改造 EnumAnnotationTest；**无生产代码、无 config/产物/文档影响**
- 影响：回归网升级为「输入矩阵 + 整文件 golden」，7 生成器全覆盖；EnumAnnotationTest 语义聚焦

## 验收标准（完成 = 下列行为全部成立）

1. 7 个 golden 测试类全绿：成功场景 = 输入与 expected 全部文件整文件一致；错误场景断言异常消息。
2. EnumAnnotationTest 收敛后仍绿且仅含 `@enum` 特性断言（不再重复 EnumGenerator 模板文本）；其余既有断言零改动、全量 `mvn clean test` 全模块绿。
3. **PIT 对比**：与基线同口径重跑——gen 相关类 survived 合计下降（重点 MapperXmlGenerator 25 / PojoGenerator 9 / ConverterGenerator 5 / EnumGenerator 5；逐类 diff 记 progress）。
4. 风格合规：AAA、harness 内聚、无样板注释；fixture 变更走契约 review（无 auto-update）。

## 验证

- 测试：新 golden 类 + 收敛后 EnumAnnotationTest + 全量既有；PIT：`JAVA_HOME=... mvn org.pitest:pitest-maven:mutationCoverage -pl ddl-codegen-core -Dmaven.compiler-plugin.debug=true`（约 2.5 分钟），解析 `target/pit-reports/mutations.xml` 与基线同口径对比（两次运行目录记 progress）
- 验证命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿；`new-change.sh check`
