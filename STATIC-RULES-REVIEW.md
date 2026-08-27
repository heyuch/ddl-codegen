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
- 编译期处理器：error-prone（-Xplugin，经 pom 注入）、checkerframework NullnessChecker（`-Awarns`，warning 级）、lombok
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
