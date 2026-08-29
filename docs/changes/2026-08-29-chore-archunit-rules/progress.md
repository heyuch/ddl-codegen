# progress：archunit-rules

## 状态：✅ 核心规则已落地（门面防绕过规则随 maven-plugin 变更追加）

- [x] 打破 gen↔interceptor 循环：`ArtifactInterceptor` 接口归位 `gen`，jdeps 复验 `gen -> interceptor` 边为 0
- [x] ArchitectureTest 4 条规则（叶子无依赖 / 单向分层 / interceptor 约束 / 无循环）——68/68 测试绿
- [ ] 门面防绕过：cli/maven-plugin 不得直连 `gen`/`tree`（依赖 Codegen 门面，随 maven-plugin 变更追加）
