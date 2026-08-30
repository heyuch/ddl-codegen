# opt-jacoco：引入 JaCoCo 覆盖率（报告 + check 门槛）

## 背景与问题

项目测试质量治理目前只有 PIT 变异测试（core 模块、显式运行）——它验证「测试强不强」（变异击杀率），但：
- **慢**：全量跑不现实，只能显式命令，无法进常规构建
- **无量化的行/分支覆盖基线**：不知道「哪些代码根本没测到」——PIT 的 line coverage 在报告中但不作为门槛
- 覆盖范围限于 core（targetClasses 只配了 `hyc.codegen.core.*`）

引入 JaCoCo：行/分支覆盖率**每次构建快速采集 + 门槛化**（覆盖率低于阈值即失败），与 PIT 互补
（JaCoCo 量化保底、PIT 深度验证质量）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：仅报告（不设门槛） | 零维护、无构建失败风险 | 不强制 = 覆盖率退化无人知，与项目严格基调不符 | 否决 |
| **B：报告 + check 门槛**（prepare-agent 绑 initialize，report/check 绑 test 阶段，阈值按模块实证） | 进 `mvn clean test` 无需改验证命令；覆盖率量化基线 + 退化即失败；与 checkstyle/spotbugs 同一严格化思路 | 首次定阈值需实证（过严则构建常红，过松则无约束力）；report 绑 test 的时机需实证（surefire 后拿 exec 数据） | **推荐** |
| C：不引入（PIT 已够） | 零新增 | 无行覆盖量化基线；PIT 慢无法日常跑 | 否决 |

## 方案

1. **根 pom** `pluginManagement` 加 `jacoco-maven-plugin 0.8.12`（Java 8+ 运行，支持 Java 21 类文件，Java 11 构建无压力）
2. **根 pom** `<plugins>` 绑定（同 spotbugs 模式，进 `mvn clean test` 生命周期）：
   - `prepare-agent` → `initialize`（surefire 前插桩，agent 属性传给 surefire）
   - `report` + `check` → `test` 阶段（surefire 之后；若实证 report 在 surefire 前执行拿不到 exec 数据，改为 report/check 绑 `verify` 并调整验证命令——以实证为准）
3. **阈值策略**（实现第一步实证）：
   - 先跑全量报告拿基线（core/tree 是覆盖重点：69+23 测试；cli/maven-plugin 是薄门面）
   - 按模块分设阈值（如 core/tree 高、cli/maven-plugin 低或排除），**阈值定稿呈报用户确认**，不拍脑袋
   - check 规则用 line 覆盖率（`COVEREDRATIO`），`haltOnFailure=true`（默认）
4. 不引入额外依赖、不扫描测试类自身（jacoco 默认只统计被测类）

## 改动文件与影响面

- 改动：根 `pom.xml`（pluginManagement + plugins 绑定）
- 无源码改动（除非阈值暴露的未覆盖代码需补测——视基线而定）
- 文档：`docs/progress.md` 关键决策记录追加；变更目录 design/progress；基线与阈值记录于本变更 progress.md
- 影响面：4 模块构建各多一次覆盖率采集与报告生成（秒级）；`mvn clean test` 生命周期内运行，无需新命令；与 PIT/spotbugs 无冲突（各自独立分析）

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿（prepare-agent/report/check 在生命周期内执行）
- 报告生成：`target/site/jacoco/index.html`（每模块）人工核对基线
- check 门槛生效：临时调高阈值验证构建失败，再调回定稿值
- 与现有工具链共存：spotbugs/checkstyle/error-prone/checkerframework/PIT 全部不受影响
