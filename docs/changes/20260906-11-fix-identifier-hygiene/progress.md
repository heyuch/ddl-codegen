# 变更台账：20260906-11-fix-identifier-hygiene

## 状态

**实现完成**（2026-09-06：全量门禁绿 + 编译期 0 告警；提交待落）

## 评审记录

- 2026-09-06 用户否决 20260906-07 A 部分的处置方式并定案两原则：① 反引号在 **DDL parse 阶段**清理，不放任流转到 codegen；② `NamingService` 只对 **Java 关键字**改名，MySQL 保留字由 **MapperXmlGenerator 内部自理**（只有 mapper 与 MySQL 打交道）。方案（design.md）经用户批准。

## 实现记录

- **Parse 单点清理（DruidAst/DruidDdlParser）**：实证（druid 1.2.23 探针）`SQLIdentifierExpr.getSimpleName()` **不剥反引号**——`nameOf`/`columnNames` 的 SQLIdentifierExpr 分支补剥；新增 `DruidAst.columnName(SQLColumnDefinition)`（剥引号）；`convertColumn` 改用之。模型（Column/Table/Index 名）从此恒为干净标识符。
- **NamingService**：`RESERVED_WORDS` → `JAVA_KEYWORDS`（Java 关键字 + 字面量 true/false/null，MySQL 词表整段删除）；删除 3 处 `.replace("`","")`；`columnFieldName` 仅当结果为 Java 关键字时加 suffix。`order` 字段从此原名直出（07 为 `order_`）。
- **MapperXmlGenerator**：内置 MySQL 8.0 官方 RESERVED 全集（注释注明来源）+ `sqlName()`（命中才 `` ` `` 包裹，大小写不敏感）；应用到 SQL 语句发射点（表名 DELETE/INSERT/UPDATE/SELECT、BaseColumnList `t.`、INSERT 列清单、UPDATE SET/WHERE、DELETE WHERE、SELECT WHERE）。`resultMap column=`、`keyColumn=` 为 MyBatis 元数据串**不加引**（golden 实证）。
- **测试/golden**：`NamingServiceTest` 反引号用例改为「干净名契约」（order→order、class→class_、int→int_）；`DruidDdlParserTest` 新增 `quotedIdentifiersStoredClean`（表/列/索引列名剥引号断言）；`pojo/reserved-word` input 补 `` `class` `` 列，expected 重出（`order` 原名 + `class_` 后缀 + 无反引号）；新增 `xml/reserved` golden（表 `` `order` `` + 列 `group`/`order`，SQL 文本自动加引、非保留字/元数据不加引），MapperXmlGeneratorTest 相应断言。
- **文档**：architecture.md 命名速查更新（Java 关键字限定 + 职责分层说明）；golden diff 审阅通过；全量门禁绿。

## 实现偏差（相对 design.md）

- design 提「DruidAst.columnNames 剥引号」仅指 SQLIdentifierExpr 分支——实测 `nameOf` 同缺陷，一并补剥（表名/rename 引号列此前同样会带引号流入模型）。
- xml/reserved 输入索引用 `KEY idx_group_order (...)`（独立 `INDEX` 元素在 Druid 中为 MySqlTableIndex、项目未建模会跳过——既有解析边界，非本变更引入）。

## 边界（不做，随记录）

- MySQL 词表只服务 SQL 引号判定，与 Java 命名词表独立（两侧职责不同，独立维护属预期）；枚举项/索引名不做保留字判定。
- 07 的 B/C 部分（pk≠id、impl 空安全）不在此变更范围，保持不变。
- 非保留字若原 DDL 显式加引，产物 XML 不再透传引号（SQL 语义等价、输出更干净）；现有 fixtures 无此形态，golden 无此 diff。

## 收尾与自检

- 记忆文档自检（新上下文实例 10 问走查）：**合格**——architecture 命名速查与代码一致（JAVA_KEYWORDS/parse 剥引号/sqlName 加引 + 元数据不加引，golden 实证）；config 键表 `naming.column.keywordSuffix` 措辞限定为「Java 关键字」（观察项 ① 已修）；07 行「A 部分由 20260906-11 取代」注记与 11 行自洽；无 07 时代表述残留于记忆文档（仅归档/历史索引行）。
- `new-change.sh check` / `check-docs` 通过；全量门禁绿 + 编译期 0 告警。
