# opt-spotbugs：引入 SpotBugs 字节码级静态检查

## 背景与问题

现有静态检查全部作用于**源码/编译期**：spotless（格式）、checkstyle（风格，error 级）、
error-prone（编译期 bug pattern）、checkerframework NullnessChecker（空值，-Awarns）、
ArchUnit（架构）、PIT（变异测试）。缺字节码级缺陷分析：
资源泄漏、equals/hashCode 契约、可变对象暴露（EI_EXPOSE_REP）、默认编码（DM_DEFAULT_ENCODING）、
忽略返回值（RV_RETURN_VALUE_IGNORED）等——这类模式在编译期插件里覆盖不全或完全不覆盖。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：只扩展 error-prone/checkerframework | 零新增依赖 | 仍是编译期分析，字节码层模式（序列化、互操作、EI_EXPOSE_REP 等）仍漏 | 否决 |
| **B：spotbugs-maven-plugin + 严格门槛**（`check` 绑 `process-classes`，effort=Max，threshold=Low，failOnViolation=true） | 进 `mvn clean test` 无需改验证命令；与项目严格基调一致；首轮 triage 顺带修掉真 bug | 首轮有 triage 工作量（真 bug 修代码 / 误报进 exclude 带理由） | **推荐** |
| C：只出报告不设门槛（绑 verify 或手动跑） | 零维护成本 | 不强制 = 没人跑，形同虚设 | 否决 |

**版本硬约束**：项目构建运行于 Java 11（`JAVA_HOME=/opt/homebrew/opt/openjdk@11`），
SpotBugs 4.9+ 运行需 Java 17 → 锁 **spotbugs-maven-plugin 4.8.6.8**（自带 SpotBugs 4.8.6，
Java 8+ 可运行，与 Java 11 完全兼容）。这是整个方案唯一的版本绑定理由。

**Lombok 影响**：工具自身主代码几乎不用 Lombok 注解（仅 PojoGenerator 注释提及），
不存在生成代码误报问题 → 不需要 lombok.config / spotbugs-annotations；
若后续出现生成代码误报，再按需在 exclude filter 处理。

## 方案

1. **根 pom.xml** `pluginManagement` 加 `spotbugs-maven-plugin` 4.8.6.8：
   `effort=Max`、`threshold=Low`（全量检出）、`failOnViolation=true`、
   `excludeFilterFile=${maven.multiModuleProjectDirectory}/spotbugs-exclude.xml`
2. **根 pom.xml** `<plugins>` 绑定 `check` 到 `process-classes` 阶段：
   `check` 自带分析（无需先跑 spotbugs 目标），`process-classes` 在 `mvn clean test`
   生命周期内（compile 之后）→ 沿用现有验证命令即自动执行
3. **首轮 triage**（跑 `mvn clean test` 拿失败清单 + `mvn spotbugs:spotbugs` 出 HTML 报告）：
   - 真 bug → 修源码（顺带收益）
   - 误报/有意接受 → `spotbugs-exclude.xml` 加 Match，**每条必带 Justification**（项目既定抑制准则）
   - 若实证显示 Low 级噪音占比过高且无价值 → 降 threshold=Medium（评审时凭数据决定，不预判）
4. 不引入 spotbugs-annotations 依赖、不扫描 test 类（测试已有 PIT/ArchUnit 覆盖），保持最小面

## 改动文件与影响面

- 改动：根 `pom.xml`（property + pluginManagement + plugins 绑定）
- 新增：`spotbugs-exclude.xml`（根目录，与 checkstyle.xml 同位；首轮 triage 后按需填内容）
- 可能：个别源文件小改（首轮 triage 发现的真 bug）
- 文档：`docs/static-rules-review.md` §1 规则集概览补一行；`docs/progress.md` 关键决策记录追加
- 影响面：4 个模块构建各多一次字节码分析（秒级）；无运行时影响、无公共 API 变化、
  与现有插件（spotless/checkstyle/error-prone/PIT/dependency-analyze）无冲突
  （spotbugs core 是插件依赖，不影响 dependency-plugin 的项目依赖分析）

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿（spotbugs check 在 process-classes 执行）
- `mvn spotbugs:spotbugs` 出 HTML/XML 报告，人工核对 triage 结果与 exclude 理由一致
- 若首轮修了源码：既有测试全绿（PIT 与 ArchUnit 不受影响）
