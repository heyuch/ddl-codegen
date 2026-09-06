# 变更台账：20260906-08-chore-annotation-style-warning-gate

## 状态

**实现完成**（2026-09-06：全量门禁绿 + 编译期 0 告警；提交待落）

## 评审记录

- 2026-09-06 用户逐条决定（已入 design.md 方案，此处只列执行要点）：checker `type.anno.*` 30 条告警**源码根治**（type-use 注解放修饰符后紧邻类型；声明注解 @Override/@SuppressWarnings 在前）；AGENTS.md 增补注解书写与告警处理约定；告警**升级为 error 强制门禁**；确认不处理处就地带 WHY 的 @SuppressWarnings（项目统一抑制方式），**不改工具/静态检查配置**。

## 实现记录

- **源码规范化 30 处 type.anno**：tree model（Class/Method/Variable/CompileUnit/JavaCodegen/JavadocTreeConverter/Literal）+ core（DdlConfig/Column/Index/GenerationContext）字段 `@MonotonicNonNull` 独立行 → `private @MonotonicNonNull X x;`；方法注解序统一「@Override → @SuppressWarnings（声明注解）→ `public @Nullable X`（type-use 紧邻类型）」，行内注释保持原位。
- **StringSplitter 6 处** → `split(x, -1)`：ImportManager/NamingService×2/PropertiesConfigLoader/CommentDocs/EnumCommentParser（各守卫已忽略尾空，语义等价实证）。
- **JavaLangClash×2**（tree.Package/Class 与 java.lang 同名属有意领域命名）：类级 `@SuppressWarnings("JavaLangClash")` + WHY 注释。
- **Druid deprecation×1**：核查 druid-1.2.23 字节码（javap）——弃用 API 是拼写错误的 `containsNotNullConstaint()`，存在正确拼写**未弃用**替代 `containsNotNullConstraint()` → 代码修复（design 预案「无替代则方法级抑制」未触发，两方法并存于 1.2.23）。
- **UnusedVariable×3**：`MapperXmlGenerator.baseColumnList` 死参数 ctx（含调用点）删除；`EndToEndTest.add` 死参数 use（签名 + 7 调用点）删除；`TypeMapperTest` 未用字段 config + 相应 import 删除。
- **-Werror 生效实证（真实告警而非注入）**：加 `-Werror` 后主编译（无 junit 类路径）的 junit stub "Package not found" 噪音即被提升为 error——javac 报「错误: 发现警告, 但指定了 -Werror」，构建失败，实证门禁有效。
- **junit stub 主编译噪音处置**：maven-compiler-plugin 3.15 无 test-only 编译器参数（plugin.xml 实证：`testCompilerArgs` 参数不存在，`testCompilerArguments` 已废弃指向 compilerArgs）→ 无法把 `-Astubs` 限定到测试编译；改用 checker 选项 **`-AstubNoWarnIfNotFound`**（与 `-AstubWarnIfNotFound` 互斥）抑制「主类路径无 junit」的预期缺失噪音——非源码告警，测试编译时 junit 在类路径上契约照常生效。
- **RoundTripSmokeTest 白名单扩展（javac 注解位置规范化）**：javac 实证（Probe）`public @Nullable Boolean m()` 与 `@Nullable public Boolean m()` 解析为**同一 AST**——解析器把声明头注解一律收进 modifiers 注解列表、与修饰符的相对位置不保留，打印固定「注解先于修饰符」。checker 要求的写法（修饰符后紧邻类型）经 parse→print 文本必不等，但语义零丢失、幂等成立（parse(print) 再 print 不变）。`semantic()` 增 `canonicalizeAnnotationPosition`：去空白前把「修饰符 注解」规整为「注解 修饰符」（字符串/字符字面量占位保护、括号参数按深度整体移动），与既有 @Foo()→@Foo、单参 lambda 括号白名单同类。**未放宽任何断言**：幂等字节断言、Demo 字节全等、其余语义丢失检测均原样保留。

## 边界（不做，随记录）

- 不改工具/静态检查配置文件（用户重申）；`-Werror` 提升本身是唯一工具链改动，作为 §5「不改配置」豁免项记入 `docs/static-rules-review.md` §4/§5。
- it-springboot 模块 `combine.self="override"` 自带 compilerArgs（不含 -Werror，亦不继承 pluginManagement 配置）；其生成代码质量由 spotless/checkstyle/spotbugs 守护（20260906-03 分级设计不变）。
- round-trip 语义比较只覆盖 tree 模块自身 3 个源文件（CodePrinter/Demo/JavaCodegen）；其余 model 类未入 round-trip 套件，注解位置白名单仅当未来扩展覆盖范围时继续生效。

## 收尾与自检

- 记忆文档自检（新上下文实例只喂记忆文档 10 问走查）结论：决策内容无矛盾；修正 4 处与本变更直接相关的措辞/结构——§5 旧句「不加 suppression」未限定（与代码内抑制准则字面矛盾）→ 限定为「配置级 suppression」并指代准则；§4 标题「调整项待定」与已生效内容错位 → 「用户决策与豁免区（已生效）」；§4 豁免枚举补第三项 `-AstubNoWarnIfNotFound`；changes/README 自检执行规则的记忆文档集合补 static-rules-review.md。
- 走查另报 3 处**既有漂移**（非本变更引入，未改，留待用户决定）：architecture.md 命名速查 `naming.enumClassName`（应为 `naming.enum.style`）；glossary design-first 词条复述 SKILL 步骤；AGENTS.md 模块清单缺 it-springboot。
- `new-change.sh check` / `check-docs` 通过。
