# 变更台账：20260906-02-chore-generator-focused-tests

## 状态

**实现完成，收尾中**（2026-09-06：用户评审 ✅ → 7 生成器 golden 套件 + EnumAnnotationTest 收敛 + core 门禁绿，PIT 复测对比待记）

## 评审记录

- 2026-09-06 独立评审（v1）：**需调整后通过**。修正点：① Repository「enums 视图参数用枚举类」与代码不符（findBy 参数 = repository 自身 ctx 标量视图）② 主键列名≠id 链路疑似缺陷须显式定界 ③ Enum 缺口并入既有类 vs 不改旧测试矛盾 ④ PIT 基线未落地、口径窄。
- 2026-09-06 用户决策：golden 契约测试（不测内部方法）→ v3。
- 2026-09-06 用户决策：EnumGenerator 纳入 golden；`EnumAnnotationTest` 只测 DDL `@enum` 注解特性、收回越界职责 → v4（EnumAnnotationTest 为唯一允许改造的既有测试）。
- 2026-09-06 用户评审：✅ **通过**（design v4）。

## PIT 基线（变更启动时，2026-09-06，`ddl-codegen-core/target/pit-reports/`，复用同一 mutations.xml 解析口径）

汇总：940 突变，killed 721（77%），no-coverage 106。

| gen 相关类 | killed | survived | noCov |
|---|---|---|---|
| PojoGenerator | 17 | 9 | 1 |
| EnumGenerator | 55 | 5 | 5 |
| MapperGenerator | 16 | 1 | 1 |
| MapperXmlGenerator | 26 | 25 | 0 |
| RepositoryGenerator | 1 | 0 | 0 |
| MybatisRepositoryImplGenerator | 19 | 4 | 3 |
| ConverterGenerator | 15 | 5 | 2 |
| QueryMethods / QueryMethodFactory | 9 | 0 | 0 |
| TableContext | 22 | 0 | 8 |

（复测对比数据收尾补记本表之后。）

**PIT 复测（20260906-02 完成后，2026-09-06）：killed 721→768（77%→82%），line 88%→90%**

| gen 相关类 | 基线 survived | 复测 survived | killed(now) |
|---|---|---|---|
| PojoGenerator | 9 | 2 | 25 |
| EnumGenerator | 5 | 4 | 57 |
| MapperGenerator | 1 | 1 | 16 |
| MapperXmlGenerator | 25 | 3 | 48 |
| RepositoryGenerator | 0 | 0 | 1 |
| MybatisRepositoryImplGenerator | 4 | 2 | 22 |
| ConverterGenerator | 5 | 1 | 19 |
| QueryMethods / QueryMethodFactory | 0 | 0 | — |
| TableContext | 0 | 0 | 22 |
| **合计 survived** | **49** | **13** | — |

验收 3 达成（survived 合计下降 49→13）；XML/POJO/Converter 为主要受益者（golden 整文件比对击杀字面量/顺序/格式类突变）。

## 已知 / 记录（实现期发现，不在本变更修复）

- 保留字列名经反引号（`` `order` ``）：Druid 解析保留反引号、列名不剥 → 生成 `private Integer `order``（非法 Java 标识符）——真实解析缺陷（疑在 Druid 解析/模型层剥反引号缺失），已从 PojoGeneratorTest 移除该场景（不锁坏输出），另立 fix 变更评估。
- Repository enum 参数化查询未实现（findBy 参数 = repository 自身 ctx 标量视图，与实现一致，测试按此锚定）；pojo `@type` 单侧转换不可编译直拷；converter 同产物退化配置——均不写进测试。
- findById/deleteById 族仅对主键列名为 `id` 成立（20260906-02 定界，另立 fix 评估）。

## 实现记录

- 新增：`gen/GeneratorTestSupport`（golden harness：严格/子集比对 + `-Dgolden.update=true` authoring 不静默）+ 7 golden 测试类（Mapper 3 场景 / MapperXml 4 / Repository 3 / MybatisRepositoryImpl 4 / Converter 2 / Pojo 4 / Enum 9 含错误组）+ `src/test/resources/fixtures/gen/**` 期望产物。
- 收敛：`EnumAnnotationTest` 由 10 测试减至 3（仅 @enum 特性：视图/桥接、ALTER 增量+手写保留、手写常量去重守卫），模板文本断言移交 EnumGeneratorTest golden。
- 验证：core 全量门禁 BUILD SUCCESS（119 测试，checkstyle/checker/error-prone/spotbugs/spotless/jacoco 全进）；golden 均在 authoring 后经人工审阅（mapper/XML/repo/impl/converter/pojo/enum fixture 关键契约抽查）并关掉 update 复跑通过。
