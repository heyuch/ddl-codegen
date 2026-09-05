# 变更台账：20260906-05-chore-generator-test-ddl-fixtures

## 状态

**实现完成**（2026-09-06：用户建议 → 7 套件 DDL 迁入 fixtures，29 测试绿；提交待落）

## 评审记录

- 用户建议即需求（2026-09-06）；机械式测试组织重构、无生产/产物影响、由既有 golden 测试作回归保障，按小改动处理（AGENTS 小改动豁免）。

## 实现记录

- harness：`generateAndAssert`/`generateAndAssertSubset`（按文件名序执行 `fixtures/gen/<case>/input/*.sql`、共享 Schema，再走既有 golden 比对）；input 目录定位与 expected 同机制。
- 迁移：Mapper(3)/MapperXml(3+错误内联)/Repository(2+错误)/RepositoryImpl(2+错误×2)/Converter(2)/Pojo(4)/Enum(8+错误组) —— 共 30 个 input sql；方法体瘦身为 config + 一行断言调用；错误路径 DDL 保留内联。
- 验证：7 套件 29 测试绿；core 全量门禁待跑后记。

## 待办

- core 全量门禁 + reactor 门禁复跑确认；提交（附本 design/progress + 索引行）。
