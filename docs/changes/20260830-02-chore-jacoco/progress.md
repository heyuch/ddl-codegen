# progress：2026-08-30-opt-jacoco

## 状态

- [x] 分析 + 设计（design.md）
- [x] 用户评审（方案 B + cli 补 MainTest）
- [x] 实现（pom.xml 配置：prepare-agent/report/check）
- [x] 基线实证（全量报告 + 模块覆盖率）
- [x] 阈值定稿（呈报用户拍板：全局 line ≥75%）
- [x] 验证（mvn clean test 全绿 + 门槛生效实证）
- [x] 文档同步（progress.md / 关键决策记录）

## 执行记录

### 基线（引入后首次全量报告）

| 模块 | line | branch | 测试数 |
|---|---|---|---|
| core | 87.4% | 73.3% | 69 |
| tree | 78.9% | 63.1% | 23 |
| maven-plugin | 87.0% | 73.1% | 13 |
| cli | 0.0% | 0.0% | 1（仅 ArchUnit 架构测试，无业务测试） |

### cli 0% 的处理（用户拍板方案 a）

- 根因：cli 只有 ArchitectureTest（架构规则），Main 门面从未被测试执行
- **补 MainTest**（8 测试）：正常生成写盘、dry-run 不写盘、目录 DDL 拼接、
  缺 --ddl 返回 2、未知参数返回 2、--help 返回 0、DDL 路径不存在返回 1
- cli 覆盖率 0% → **87.7%**

### 阈值定稿（全局统一，用户确认）

- `LINE`/`COVEREDRATIO` ≥ **0.75**（BUNDLE 规则，所有模块）
- 依据：core 87.4 / tree 78.9 / maven-plugin 87.0 / cli 87.7（补测后）——留缓冲
- 门槛生效实证：临时调 0.99 → tree 构建失败（「lines covered ratio is 0.78」）→ 恢复 0.75

### 绑定（实证）

- `prepare-agent` 绑 `initialize`（surefire 前插桩）✓
- `report`/`check` 绑 `test` 阶段——实证在 surefire 之后执行（exec 数据有效）✓
- 全部进 `mvn clean test`，无需新验证命令
