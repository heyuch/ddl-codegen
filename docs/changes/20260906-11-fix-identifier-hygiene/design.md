# identifier-hygiene

> 变更号：20260906-11　变更类型：fix　目录：`docs/changes/20260906-11-fix-identifier-hygiene/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

20260906-07 对反引号/保留字的处置职责错位（用户评审否决其 A 部分处置方式）：反引号在 `NamingService` 三个命名入口各剥一次（模型仍携带 `` ` ``，靠渲染层透传才合法）；`NamingService.RESERVED_WORDS` 混入 MySQL 词表（Java 侧对 SQL 词改名）；XML 侧无任何 MySQL 保留字引号逻辑（仅 07 的 pojo reserved-word 一个 fixture 覆盖，XML 保留字行为零契约）。

**用户定案原则（2026-09-06）**：
1. 反引号在 **DDL parse 阶段单点清理**，不放任流转到 codegen。
2. `NamingService` 只对 **Java 关键字**改名（加后缀）；其余按 column 原名做格式变更——尽量用原名，除非影响生成代码正确性。
3. `NamingService` 不关心 MySQL 保留字——只有 mybatis mapper 与 MySQL 打交道，由 **MapperXmlGenerator 内部自理** MySQL 保留字引号。

## 方案

- **Parse 单点清理**：`DruidDdlParser` 建 Column 处 `.name(definition.getColumnName())` → `DruidAst.nameOf(definition.getName())`（与表名/rename 同 helper：剥反引号 + 剥库前缀）；`DruidAst.columnNames` 的 SQLIdentifierExpr 分支同样剥引号（索引列与模型列名匹配的前提）。模型 Column/Table/Index 名称恒为干净标识符。
- **NamingService**：`RESERVED_WORDS` 删整段 MySQL 词表、只留 Java 关键字；删除 3 处 `.replace("`","")`（输入契约 = 干净名）；`columnFieldName` 仅当结果为 Java 关键字时加 `getColumnKeywordSuffix()`。
- **MapperXmlGenerator**：内置 MySQL 8.0 官方 RESERVED 词表 + `sqlName(name)`（命中才加 `` ` ``，大小写不敏感）；应用到真正进 SQL 的标识符发射点——表名（DELETE/INSERT/UPDATE/SELECT）、BaseColumnList `t.` 列、INSERT 列清单、UPDATE SET/WHERE、DELETE WHERE、SELECT WHERE。`resultMap column=` 与 `keyColumn=` 是 MyBatis 元数据串（ResultSet label 比对/key 回填）**不加引**；非保留字恒不加引。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 07：命名层剥引号 + 模型透传引号 + XML 无引号逻辑 | 改动小 | 职责错位：MySQL 引号语义泄漏进模型/命名；保留字改名过度（Java 侧 SQL 词） | 否决（本次取代） |
| 本次：parse 单点清理 + Java 关键字限定的命名 + XML 保留字引号 | 每层职责单一：模型存真名、Java 命名只对 Java 正确性负责、SQL 方言细节只在 SQL 生产者内 | 新增 MySQL 词表（与 Java 词表独立、会漂移——但两侧本就不同职责） | 采用 |

## 改动文件与影响面

- core main：`ddl/DruidDdlParser.java`、`ddl/DruidAst.java`（columnNames 剥引号）、`naming/NamingService.java`（词表收窄 + 去 strip）、`gen/MapperXmlGenerator.java`（词表 + sqlName + 发射点）
- 测试/fixtures：`NamingServiceTest`（反引号入参用例删除；`order`→`order`、`class`→`class_`）；`pojo/reserved-word`（input 补 Java 关键字列 `` `class` ``，expected 重出：`order` 原名、`class_` 后缀、无反引号）；新增 XML 保留字 golden 用例（`order` 列等，断言 SQL 文本加引）；parser 相关断言（Column/索引列名无反引号）；`PojoGeneratorTest` 断言同步。
- 不做（边界）：MySQL 词表仅 SQL 引号判定、不并入 Java 命名；枚举项/索引名不做保留字判定；07 的 B/C 部分不动。

## 验收标准

1. `` `order` `` 列：模型 `Column.getName()` == `order`（无反引号）；pojo 字段 `order`（原名直出）；`` `class` `` 列字段 `class_`（Java 关键字后缀）。
2. XML golden：`order` 作列/表名时 SQL 文本自动 `` `order` `` 加引；非保留字不加引；resultMap `column=`/`keyColumn=` 不加引。
3. `NamingService.columnFieldName("order") == "order"`、`("class") == "class_"`；反引号入参不再有测试契约。
4. 全量门禁绿 + 0 告警；golden diff 审阅（预期：pojo reserved-word 重出 + 新增 xml 用例，其余 0 diff）。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`（`-Dgolden.update=true` 重出后审阅 diff 再复原）
- grep：模型/命名层无 `` ` `` 残留处理；`new-change.sh check`
