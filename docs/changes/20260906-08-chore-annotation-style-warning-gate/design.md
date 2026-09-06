# annotation-style-warning-gate

> 变更号：20260906-08　变更类型：chore（源码书写规范 + 工具链门禁；无行为变化）　目录：`docs/changes/20260906-08-chore-annotation-style-warning-gate/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

编译期告警 52 条经核查分两类：真问题（已随前轮修复）与风格/三方噪音。剩余 42 条中 30 条为 **checkerframework NullnessChecker 的 type-use 注解位置建议**（`type.anno.before.modifier`/`type.anno.before.decl.anno`）——formatter 无法自动消除（注解顺序不被格式化器重排）。用户决策（2026-09-06）：
1. **根治源码书写规范**（type-use 注解置于修饰符之后紧邻类型；声明注解 @Override/@SuppressWarnings/@Deprecated 置于 type-use 之前）；
2. **AGENTS.md 增补写作约定**明确该注解风格与「不处理处用带原因的 @SuppressWarnings」的统一门禁抑制方式；
3. **将这类 warning 提升为 error（强制门禁）**——工具检查出而忽略即失去意义；确认不处理处按统一抑制方式在代码内忽略并写明原因。

## 方案

- **来源确认**：根 pom `maven-compiler-plugin.compilerArgs` 以 `-processor org.checkerframework...NullnessChecker` + `-Xplugin:ErrorProne` 运行；type.anno.* 为 checker 诊断。提升机制 = 在该 compilerArgs 追加 **`-Werror`**（javac 统一把 warning 提升为 error；checker/EP 告警同被覆盖），it-springboot 模块因消费者定位已独立 override compilerArgs 不受影响（其源码由生成器产出、质量由 spotless/checkstyle/spotbugs 守护）。
- **源码规范化（30 处 type.anno + 其余）**：
  - `type.anno.before.modifier`：字段注解由「独立行在修饰符前」改为「`private @MonotonicNonNull X x;`」（修饰符后紧邻类型）；
  - `type.anno.before.decl.anno`：方法注解顺序统一为 声明注解（@Override/@SuppressWarnings/@Deprecated）在前、type-use（@Nullable）在后（紧邻返回类型）；含中间注释保持原位。
  - 其余确认不处理项按统一抑制：JavaLangClash×2（tree.Package/Class 命名有意）类级 `@SuppressWarnings("JavaLangClash")`+原因；deprecation×1（Druid 三方 API 无替代）方法级 `@SuppressWarnings("deprecation")`+原因；UnusedVariable 覆写参数/测试 helper（MapperXml override ctx、EndToEnd add use 参数可删、TypeMapperTest 字段删除/抑制按实情）。
  - StringSplitter×6：改为 `split(x, -1)`（当前各处守卫已忽略尾空，语义等价）。
- **AGENTS.md「命名与写作约定」增补**：type-use 注解书写（修饰符后紧邻类型；声明注解先于 type-use）；「静态检查告警处理三档：修复 / 源码级 `@SuppressWarnings`+WHY / 维持(须用户确认)」，并声明不改工具/静态检查配置（`-Werror` 提升为本决策豁免项，记录 static-rules-review §4/§5）。
- **static-rules-review.md**：§3 加实证行（type.anno 来源与处置）、§4/§5 记录「-Werror 门禁 + 代码内抑制统一方式」用户决策。

## 改动文件与影响面

- 源码：tree model/JavaCodegen 等 + core（约十余文件 30 处规范 + 6 处 split(-1) + 3 处抑制/删除）
- 工具链：根 pom compilerArgs `-Werror`
- 记忆文档：AGENTS.md（写作约定）、docs/static-rules-review.md（决策/实证）、docs/architecture?（如注解写法入速查，可选）、changes/README 索引行
- 影响：门禁收紧（任何遗留 warning 将 fail）；需全量回归 + 确认无隐藏 warning 浮出（如 `removal` 等 javac 警告一并暴露，逐条按三档处置）

## 验收标准

1. `mvn clean test`（默认，无 -Dxxx.skip）全模块绿；编译期 **0 warning**（type.anno 消除、其余已处置）。
2. 故意引入一条同类违规 → 构建失败（-Werror 生效实证）。
3. AGENTS/static-rules 含上述写作与抑制约定；代码内所有 @SuppressWarnings 均带 WHY 注释。
4. 生成物字节不受影响（仅 generator 源仓库自身源码，不涉产物模板）。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`（-Werror 后全绿）+ 违规注入探针临时验证后移除
- `new-change.sh check` / `check-docs`；记忆文档自检（AGENTS/static-rules 改动后必跑）
