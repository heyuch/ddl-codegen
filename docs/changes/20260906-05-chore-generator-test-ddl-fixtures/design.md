# generator-test-ddl-fixtures

> 变更号：20260906-05　变更类型：chore（仅测试组织重构：DDL 输入迁入 fixtures；无行为/产物变化）　目录：`docs/changes/20260906-05-chore-generator-test-ddl-fixtures/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

20260906-02 的 7 生成器 golden 测试把 DDL 内联在测试方法里，期望产物在 fixtures——同一 case 的输入/期望分离两处；DDL 改动 diff 不直观、方法体冗长（用户 2026-09-06 建议：generator 单测的 DDL 也放 fixtures）。

## 方案

- fixtures 每 case 增加 `input/*.sql`（DDL 事实源，按文件名序、共享 Schema 依次执行；多步场景 = create + 各 alter 文件）。
- harness `GeneratorTestSupport` 新增 `generateAndAssert`/`generateAndAssertSubset`（先跑 input 再比对）；原 `assertGolden*` 保留（无 input case 兼容）。
- 7 套件成功场景的 DDL 全部迁入 fixtures（30 个 input 文件）；config/options 仍在测试代码（程序化、非 DDL 文本）；错误路径断言 DDL 保留内联（贴近断言、便于阅读）。
- 不做：config 也入 fixtures（artifact/options 是程序化配置，迁文件收益低且失去类型安全）。

## 改动文件与影响面

- 测试 only：`GeneratorTestSupport`（+2 方法 + input 目录定位）；7 个 golden 测试类方法瘦身；`src/test/resources/fixtures/gen/**/input/*.sql`（30 个）新增。
- 无生产代码/产物/文档变化；既有断言语义不变（extra 断言保留）。

## 验收标准

1. 7 套件全绿（成功场景读 fixtures input 生成并比对 expected；错误场景不变）。
2. `mvn clean test` 全 reactor 绿；fixture 文件集无多余（input 未重复执行、顺序稳定）。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`；`new-change.sh check`
