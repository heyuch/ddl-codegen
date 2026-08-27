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
| VariableDeclarationUsageDistance | allowedDistance=3 | **待实证（可能是度量类中最易误报的）** |

**迁移前实测基准**（tree 库最大文件 JavaCodegen.java，770 行）：37 个方法、未超任何度量阈值。
初步结论：阈值普遍宽松，**度量规则大概率不是摩擦点**；预判摩擦点应是 DesignForExtension（javadoc 负担）与 VisibilityModifier（public 字段），均非度量类。

## 2. 规则预判（待实证，按风险排序）

| 规则 | 预判风险 | 依据 | 实证状态 |
|---|---|---|---|
| `DesignForExtension` | **高** | 无配置：任何 public 非 final 类的可重写方法须带 javadoc。tree 库 43 个模型类数百公共方法，纯机械补 javadoc 成本高；且"应可被继承"的默认假设对模型/生成器类不一定成立（加 `final` 可能是更合理的选择） | 待 M0a/M0b |
| `VisibilityModifier` | 中（可能利好） | packageAllowed + allowPublicFinalFields + allowPublicImmutableFields：**public 非 final 可变字段必违规**。tree 库现有大量 public 可变字段 → 强制 getter/setter 重构——恰好与既定风格目标一致，可能"合理但加重迁移负担" | 待 M0a |
| `VariableDeclarationUsageDistance` | 中 | allowedDistance=3、ignoreFinal=false：声明与首次使用距离 ≤3 行。生成器/工具代码"先收集后使用"的写法可能误报 | 待实证 |
| `VariableDeclarationUsageDistance` | 中 | 唯一可能过度约束的度量类规则：声明与首次使用距离 ≤3 行，"先收集后使用"的代码可能误报 | 待实证 |
| `MethodCount` / `MethodLength` / `ExecutableStatementCount` | 低（已降级） | 阈值宽（100/200/60），实测 JavaCodegen 770 行 37 方法未触发 | 实证基准已记录 |
| `HiddenField` | 低 | 仅 VARIABLE_DEF（参数遮蔽已豁免），setter 惯用法不受影响，配置合理 | 待实证 |
| `FileLength`(2000) / `LineLength`(120) | 低 | 当前最大源文件 770 行；120 列宽松 | 待实证 |
| `JavadocMethod` | 低 | allowMissingParamTags/ReturnTag=true、validateThrows=false，已宽松 | 合理（预估） |
| 命名类规则 | 低 | 标准驼峰，tree 库已符合 | 合理（预估） |

## 3. 实证记录（开发中持续填充）

| 阶段 | 规则 | 触发样例 | 判定 | 备注 |
|---|---|---|---|---|
| M0a（迁移） | —（待 worker 报告后填充） | | | |
| | | | | |

## 4. 用户决策区（调整项待定）

（暂空——待实证积累后由用户拍板；调整实施时在此记录：规则 / 调整方式 / 理由 / 日期）

## 5. 执行约定（已生效）

- 静态检查是硬门槛：报错按提示改代码，直至 `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn validate` + `mvn test` 全绿
- 不自行改 checkstyle.xml、不加 suppression、不加 `-Dxxx.skip`
- 唯一例外：规则本身有 bug 或与迁移代码完全冲突 → 记录规则名 + 报错原文到 §3，用合规代码结构规避（加 final、补 javadoc 等），确实无解再提交用户决策
