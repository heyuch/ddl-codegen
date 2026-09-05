# archunit-rules：架构强制规则

## 背景与问题

项目膨胀（第 4 个模块 maven-plugin 即将加入）后，架构依赖需要自动强制而非靠 review 记忆。
模块方向已被 Maven 自身强制（pom 依赖不可能成环），真正需要工具化的是 **包级分层、循环、门面防绕过**。
实测发现一个真实的包循环：`gen ↔ interceptor`（`GeneratorInterceptor` 接口依赖 `gen.TableContext`，而 `GenerationContext` 依赖该接口）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 仅 jdeps 脚本检查（CI grep） | 零依赖 | 文本断言脆弱、无循环检测、不进 mvn test | 否决 |
| **ArchUnit 测试**（依赖已在父 pom 声明，未使用） | 可读失败信息、mvn test 内跑、原生 beFreeOfCycles | 规则需维护（与分层漂移同步） | **推荐** |
| 循环修复方式：A) SPI 接口归位 gen；B) 引入中间 Context 接口 | A 纯挪文件、公共 API 不变；B 改 SPI 签名 | B 是破坏性 API 变更 | **推荐 A** |

## 方案

### 循环修复（gen ↔ interceptor）

把 `GeneratorInterceptor` 接口从 `hyc.codegen.core.interceptor` 移到 `hyc.codegen.core.gen`
（与 `ArtifactGenerator` 同居，SPI 归位框架核心）；interceptor 包只留实现（Lombok/Jsr303/InterceptorSupport）。
依赖变单向：`interceptor → gen`；`gen` 不再引用 interceptor 包。

### 规则（ArchUnit，core 模块 ArchitectureTest，jdeps 实证矩阵校准）

| 规则 | 内容 |
|---|---|
| 叶子无依赖 | `model`/`config`/`io` 不得依赖任何其他 core 包 |
| 单向分层 | `ddl`/`annotation`/`naming`/`types` 不得依赖 `gen`/`interceptor` |
| 实现层约束 | `interceptor` 只允许依赖 `gen`/`model`/`config` |
| 无循环 | `slices().matching("hyc.codegen.core.(*)..").should().beFreeOfCycles()` |

### 门面防绕过（随 maven-plugin 变更落地后追加）

`cli`/`maven-plugin` 模块测试：不得直接依赖 `hyc.codegen.core.gen..` 与 `hyc.codegen.tree..`
（必须走 `Codegen` 门面）——依赖 maven-plugin 变更中的门面提取，完成后在对应模块补规则。

## 改动文件与影响面

- 移动：`interceptor/GeneratorInterceptor.java` → `gen/GeneratorInterceptor.java`（package 变更）
- 更新 import：GenerationContext / CodeGenerator / CLI Main / LombokInterceptor / Jsr303Interceptor / 相关测试
- 新增：`core/.../ArchitectureTest.java`；后续 cli/maven-plugin 各加一条防绕过规则
- 影响：公共类型包路径变更（`interceptor.GeneratorInterceptor` → `gen.GeneratorInterceptor`）——破坏性但项目未发布、引用点少

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿（含新 ArchitectureTest）
- jdeps 复跑确认 gen↔interceptor 循环消失
