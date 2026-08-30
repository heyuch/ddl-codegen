# 静态检查规则考察记录

> 目的：项目静态检查规则（checkstyle.xml / spotless / error-prone / checkerframework）暂定，
> 在开发实践中考察其合理性，把不合理/误报/过度约束的规则记录于此，**由用户决定是否调整**。
> 本文档是活文档：随开发阶段持续补充实证。修改规则本身需用户拍板，任何人不得擅自改。

## 考察方法

1. **静态预判**：分析规则集，标出可能产生摩擦的规则（见 §2）
2. **实证记录**：M0a~M4 每个阶段，把实际触发的规则、触发样例、是否误报记入 §3
3. **决策**：用户基于实证决定调整；调整结果记入 §4

## 1. 规则集概览

- checkstyle 共 150 个 module，全局 `severity=error`（硬门槛，报错即构建失败）
- 作者已主动禁用的规则（注释掉）：`RegexpHeader`（无文件头要求）、`JavadocPackage`（与 spotless 冲突）、
  `HideUtilityClassConstructor`、`IllegalCatch`
- spotless：eclipse 格式器 + import order + sortPom（apply 绑 validate，自动格式化，无冲突面）
- 编译期处理器：error-prone（-Xplugin，经 pom 注入）、checkerframework NullnessChecker（**error 级**，强制修复或带理由抑制；`-AsuppressWarnings=keyfor` 已移除，KeyFor 局部抑制到 10 个报错类）、lombok
- spotbugs：字节码级分析（effort=Max、threshold=Low、check 绑 process-classes，`spotbugs-exclude.xml` 收录经实证的误报/接受项）
- checkerframework stub：`checker/junit-assertions.astub`（JUnit 断言契约，`@EnsuresNonNull`）
- maven-dependency-plugin analyze（`failOnWarning=true`：未使用/未声明依赖即失败）
- 结构类别：文件级（FileLength 2000 / LineLength 120 / FileTabCharacter）、
  类设计（DesignForExtension / FinalClass / VisibilityModifier / HiddenField / ThrowsCount / MutableException）、
  编码风格（约 40 条）、import（AvoidStarImport / CustomImportOrder / RedundantImport / UnusedImports）、
  javadoc（JavadocMethod 宽松配置 / 若干格式约束）、命名（全套驼峰/UPPER_SNAKE）、
  度量（MethodLength / MethodCount / ParameterNumber / ExecutableStatementCount / LambdaBodyLength / AnonInnerLength）

## 1.5 度量类规则阈值基线（当前配置）

| 规则 | 当前阈值 | 评价 |
|---|---|---|
| MethodLength | 200 行 | 宽 |
| MethodCount | maxTotal/maxPublic = 100 | 宽 |
| ExecutableStatementCount | 60 条 | 宽 |
| ParameterNumber | 7（METHOD_DEF，忽略 override） | 标准 |
| LambdaBodyLength | 30 行 | 标准 |
| AnonInnerLength | 20 行 | 标准 |
| NestedIf/For/TryDepth | 3 | 标准 |
| CyclomaticComplexity | 20 | 宽 |
| NPathComplexity | 200 | 宽 |
| JavaNCSS | 方法 100 / 类 1500 / 文件 2000 | 宽 |
| FileLength | 2000 行 | 宽 |
| LineLength | 120 | 宽 |
| VariableDeclarationUsageDistance | allowedDistance=3 | 用户已确认保留：声明应靠近使用，可读性规则合理 |

**迁移前实测基准**（tree 库最大文件 JavaCodegen.java，770 行）：37 个方法、未超任何度量阈值。
**预判修正（用户反馈）**：DesignForExtension 的 javadoc 负担对 AI 写作可忽略（不是摩擦点）；VDUD 保留。
剩余待实证项：VisibilityModifier 强制 public 字段私有化——是结构性重构工作量（43 个模型类），但符合既定风格目标，属"预期工作"而非"不合理规则"。

## 2. 规则预判（待实证，按风险排序）

| 规则 | 预判风险 | 依据 | 实证状态 |
|---|---|---|---|
| `DesignForExtension` | 低（已降级） | javadoc 成本对 AI 写作可忽略；仅存"加 final 还是补 javadoc"的设计取向问题，非摩擦 | 用户已确认 |
| `VisibilityModifier` | 中（可能利好） | packageAllowed + allowPublicFinalFields + allowPublicImmutableFields：**public 非 final 可变字段必违规**。tree 库现有大量 public 可变字段 → 强制 getter/setter 重构——恰好与既定风格目标一致，可能"合理但加重迁移负担" | 待 M0a |
| `VariableDeclarationUsageDistance` | 中 | allowedDistance=3、ignoreFinal=false：声明与首次使用距离 ≤3 行。生成器/工具代码"先收集后使用"的写法可能误报 | 待实证 |
| `VariableDeclarationUsageDistance` | 低（已确认） | 用户判定合理：声明应靠近使用，可读性规则，保留 | 用户已确认 |
| `MethodCount` / `MethodLength` / `ExecutableStatementCount` | 低（已降级） | 阈值宽（100/200/60），实测 JavaCodegen 770 行 37 方法未触发 | 实证基准已记录 |
| `HiddenField` | 低 | 仅 VARIABLE_DEF（参数遮蔽已豁免），setter 惯用法不受影响，配置合理 | 待实证 |
| `FileLength`(2000) / `LineLength`(120) | 低 | 当前最大源文件 770 行；120 列宽松 | 待实证 |
| `JavadocMethod` | 低 | allowMissingParamTags/ReturnTag=true、validateThrows=false，已宽松 | 合理（预估） |
| 命名类规则 | 低 | 标准驼峰，tree 库已符合 | 合理（预估） |

## 3. 实证记录（开发中持续填充）

| 阶段 | 规则 | 触发样例 | 判定 | 备注 |
|---|---|---|---|---|
| M0.1 | `DesignForExtension` | 286 处（全模型/工具类公共方法） | 合理 | final 化消除（仅 Identifier 因被继承保留）；实证：该规则推动的是"final vs javadoc"设计决策，非负担 |
| M0.1 | `NPathComplexity` | Class.getImports NPath 240（阈值 200） | 合理 | 拆出 ImportCollector 消除，单一职责同时改善 |
| M0.1 | `VisibilityModifier` | 全部模型类 public 可变字段 | 合理（预期工作） | 全字段私有化 + getter/setter，与风格目标一致 |
| M0.1 | `ClassFanOutComplexity` | JavaCodegen 41→39 / JavadocCodegen 25（阈值 20） | **已决策：针对性 @SuppressWarnings**（用户拍板） | 阈值保留 20 继续抓逻辑混杂；分发器类抑制并带实证注释（§6 判别方法 + 本节） |
| M0.1 | 其余度量类（MethodLength/MethodCount/Cyclomatic/NPath 等） | 未触发 | 合理 | 阈值宽松，实测确认 |
| M0.1 | `VariableDeclarationUsageDistance` | 未触发 | 合理（用户已确认） | — |
| M0.1 | spotless 配置 | Demo.java 夹具被排除格式化 | 合理（构建配置） | 夹具字节稳定性由 round-trip 断言依赖，排除属合理工程决策，用户可否决 |
| M2 | `ClassMemberImpliedModifier` vs `RedundantModifier` | 嵌套 enum：加 static 被后者报"多余"，不加被前者报"应显式"——**两条规则在嵌套 enum 上互相矛盾** | 规则矛盾 | 规避：顶层 enum（ChangeStatus），同时被 FileWriter/ChangeReport 共用；记录待用户决策（候选：ClassMemberImpliedModifier 加 exclude，或接受矛盾改代码风格） |
| M5 | `CyclomaticComplexity`（DruidDdlParser） | convertAlter 判空改造后 25（阈值 20）：分支数 ≈ ALTER 子句类型数（11）+ 畸形 DDL 判空跳过（spotbugs 严格空指针修复引入） | 已决策：类级 @SuppressWarnings（与 ClassFanOutComplexity 同依据，§6） | 分发器类别，元素驱动；记录于变更 2026-08-30-opt-spotbugs |
| M5 | spotbugs 引入实证 | 首轮 67 项：空指针类 ~40（checkerframework -Awarns 存量警告 + 注解真相不一致）、死代码/未读字段/暴露/equals 类 ~27（checkerframework 不覆盖） | 合理（工具互补实证） | spotbugs-exclude.xml 收录 17 项误报/接受项（均带 Justification）；见变更 2026-08-30-opt-spotbugs |
| M5 | KeyFor 局部化→清零 | `-AsuppressWarnings=keyfor` 全局抑制移除；实验证实 `@KeyFor` 不适用（非 key 数据），`@UnknownKeyFor` 显式元素标注 + `Collection<? extends @UnknownKeyFor T>` 签名 + keySet 拷贝是正解 | 已决策：注解化（用户拍板） | 10 个类级 keyfor 抑制清零；@KeyFor vs @UnknownKeyFor 的语义判据记录于变更 2026-08-30-opt-spotbugs |
| M5 | lombok 生成代码的 spotbugs 误报 | RCN ×8（equals/hashCode 判空 vs checkerframework 字节码 @NonNull）；lombok.config addSuppressFBWarnings=true + spotbugs-annotations → 生成成员自动带 @SuppressFBWarnings（字节码实证） | 已决策：lombok 机制解决（用户拍板） | RCN 排除删除；checkerframework 手册 Lombok 章节（addLombokGeneratedAnnotation 已配置）；spotbugs 4.8.6 不认 @lombok.Generated（4.9.4+ 才认，需 Java 17 不在约束内） |
| M5 | 注解迁移消除工具对比缺陷 | javax.annotation.Nullable → org.checkerframework @Nullable（纯 type-use）：TIGHTENS ×3（spotbugs 双注解 override 对比缺陷）随迁移消失；jsr305 依赖移除 | 已决策：统一 checkerframework 注解（用户拍板） | exclude 再删 1 条；AGENTS.md/README/design.md 注解约定同步 |
| M5 | EQ 与 EI 收尾 | TypeReference callSuper=true + 字段遮蔽消除（EQ 删除，TypeReferenceTest 契约断言）；core EI 8 类就地 @SuppressFBWarnings(justification)，浅拷贝可行的 3 项代码修复 | 已决策：代码修复优先（用户拍板） | **spotbugs-exclude 最终 1 条**（tree 包可修改 AST）；集中排除 vs 就地注解的判据：写入通道/引用传递语义必须就地或排除，只读快照语义浅拷贝修复 |
| M5 | checkerframework 升级 error 级实证 | 存量空指针全部由 @Nullable/@MonotonicNonNull 标注或显式判空修复；initialization 检查触发 `initialization.field.uninitialized`（可修改 AST 字段、builder 字段、@TempDir/@Parameter 注入字段）——**用户决策：全部注解化而非抑制**（真可空 @Nullable / 构建后必有 @MonotonicNonNull + 读取端判空 throw / JUnit-Maven 注入字段 @Nullable + getter 校验） | 已决策：注解化（用户拍板） | **initialization 类抑制零残留**；KeyFor 子检查（NullnessChecker 伴生，不可关闭）对 JDK 泛型通配符误报 → 局部 @SuppressWarnings("keyfor") 10 个类 |


## 4. 用户决策区（调整项待定）

（暂空——待实证积累后由用户拍板；调整实施时在此记录：规则 / 调整方式 / 理由 / 日期）

## 5. 执行约定（已生效）

- 静态检查是硬门槛：报错按提示改代码，直至 `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn validate` + `mvn test` 全绿
- 不自行改 checkstyle.xml、不加 suppression、不加 `-Dxxx.skip`
- 唯一例外：规则本身有 bug 或与迁移代码完全冲突 → 记录规则名 + 报错原文到 §3，用合规代码结构规避（加 final、补 javadoc 等），确实无解再提交用户决策

**针对性 @SuppressWarnings 使用准则（用户 2026-08 拍板）**：
- 允许用于"元素驱动"类（见 §6 判别方法），如 TreeScanner/DocTreeScanner 分发器
- 必须是类级、针对具体规则名（`@SuppressWarnings("ClassFanOutComplexity")`）
- 必须带 WHY 注释（含实证依据，如抽取实验数据）
- 必须记录到 §3
- 禁止用全局提阈值/加 suppression 文件绕过

## 6. 判别方法：逻辑混杂 vs 元素繁多

高扇出/高复杂度有两种成因，处理方式完全不同：

1. **抽取实验（最权威）**：把能识别的独立关注点抽成类，看扇出降幅。大幅下降 → 之前是逻辑混杂（混杂的部分本身就是可抽取的关注点）；几乎不降 → 残余为元素驱动。
   本项目实证：ImportManager 抽取后 JavaCodegen 扇出 41→39，几乎不降 → 残余结构性。
2. **单方法扇出（分散度）**：统计每个方法引用的去重类型数。分发器形状 = 方法数 ≈ 节点类型数、单方法引用 ≤3 类型；逻辑混杂 = 存在单个方法引用 5-10+ 类型（一个方法干多件事）。
3. **类型分类比例**：引用类型分两类——分发对象类型（visitor 的节点/领域类型）vs 基础设施类型（IO/集合/框架）。分发对象占比高（如 JavaCodegen 39 中 30+ 为节点/模型/扫描器）→ 元素驱动；跨多个无关领域散布 → 逻辑混杂。

判定落点：阈值保留抓逻辑混杂；元素驱动类走针对性 @SuppressWarnings。
