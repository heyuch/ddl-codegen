# chore-applyone-polymorphism：StatementApplier 多态分发重构（OCP）

## 背景与问题

`StatementApplier#applyOne` 是 10 分支的 `instanceof` if-else 链（~90 行）：
新增一种 DdlOperation 类型必须修改 applyOne（违反开闭原则）。操作的应用逻辑
（怎么改 Schema、怎么记 ApplyResult）散落在应用器里，与操作类型本身分离。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：保持现状（instanceof 分发） | 集中、调用链直观 | 违反 OCP；新增操作类型必须改应用器；逻辑不内聚 | 否决 |
| **B：多态分发（DdlOperation.apply）** | OCP ✓（新类型 = 新类实现 apply，应用器零修改）；操作与应用逻辑同居；applyOne 10 分支 → 1 行 | 判空逻辑分散到各操作（7 处小判空重复，可接受）；接口加一个方法 | **推荐** |
| C：druid SQLASTVisitor 重构 | 消除 instanceof | 深度遍历破坏顶层转换上下文（visit 方法无上下文）、状态字段、代码更散（探讨已否决） | 否决 |

## 方案

`DdlOperation` 接口加 `void apply(Schema schema, ApplyResult result)`；
10 个操作类各自实现 apply（迁移 applyOne 对应分支逻辑）；`applyOne` 变成
`operation.apply(schema, result)` 一行。

判空策略：7 个列/索引操作自含 `Table t = schema.getTable(...); if (t != null)` 判空
（显式重复可接受，不为此抽基类——YAGNI）；3 个表级操作（create/drop/rename）本就不判空。

## 改动文件与影响面

- `DdlOperation`：加 `apply(Schema, ApplyResult)` 方法
- 10 个操作类（CreateTableOp/DropTableOp/RenameTableOp/AddColumnOp/DropColumnOp/
  ChangeColumnOp/RenameColumnOp/AddIndexOp/DropIndexOp/RenameIndexOp）：实现 apply
- `StatementApplier`：applyOne 简化为分发；保留 apply 协调 + pruneIgnored
- 测试：StatementApplierTest 行为不变（纯结构重构，语义等价）
- 文档：AGENTS.md 数据流（StatementApplier 描述）、docs/design.md 数据流节
- 影响面：ddl 包内部重构；依赖方向 ddl → model 不变（无循环）；无公共 API 破坏
  （DdlOperation 是内部 SPI，项目未发布）

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿（StatementApplierTest
  覆盖 10 种操作的行为断言不变）
- spotbugs/checkerframework error 级/checkstyle 全过（新 apply 方法带 @Nullable 边界）
