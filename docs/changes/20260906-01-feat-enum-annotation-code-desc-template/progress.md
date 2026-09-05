# 变更台账：20260906-01-feat-enum-annotation-code-desc-template

## 状态

**完成**（2026-09-06：用户评审 ✅ → 实现 → reactor 全量门禁 BUILD SUCCESS → 蒸馏收尾完成，索引行已登记）

## 评审记录

- 2026-09-06 独立评审（plan subagent）：**需调整后通过**。事实基础与历史无冲突，要求补 4 项必要处置 + 若干低危项；已全部回写 design.md「对账与冲突清单」。
- 2026-09-06 用户评审：✅ **通过**（多轮调整意见后：① lombok 选项双形态 ② 裸 @enum 与 enum column 同一命名 ③ fromCodeNullable/fromCode 方法对 ④ @Nullable 可配置；提问澄清 → v7 增量更新语义 + 用户手写保留契约 + @Generated ownership 标记如实入示例）。
- 2026-09-06 用户：执行。

## 实现记录与偏差（design.md v7 对照代码）

- 改动面与 design「改动文件与影响面」一致：core 10 个类（3 新：EnumHandler/EnumItem/EnumCommentParser）+ tree 3 个类（JavaTreeConverter/JavaCodegen/Class）+ 命名/config 零改动（复用 `NamingService.enumClassName` 与 `annotations.nullable`）。
- **偏差/补充（均等价或必要，已在文档同步）**：
  1. 常量名缺省候选简化实现：显式 name >（codeType 为 String → code 原文，含 SQL-enum 字面量）>（数字 → `{列名}_{rawCode}`），与 design 语义一致（无 design 中曾留的 sqlliteral 冗余分支）。
  2. tree `JavaCodegen.enumConstantInitText` 增 **unicode 解码**（javac toString 把非 ASCII 输出为 `\uXXXX`，直接使用会使中文 desc round-trip 非幂等）——测试中发现，非 design 原文但属实现必要。
  3. tree 参数级注解打印经验证已支持（Mapper @Param 先例），无需补树改动；空枚举 `;` 与常量 kind 标注按 design。
  4. `AbstractJavaGenerator` ClassFanOutComplexity 21/20 超 1 → **类级 `@SuppressWarnings("ClassFanOutComplexity")` + WHY 注释**（用户重申既定规则：不改静态检查配置文件；初稿误加 checkstyle-suppressions.xml 条目已回退，记录于 docs/static-rules-review.md §3 M6）。
  5. 测试组织：tree 新增独立 `EnumTreeSupportTest`（design 写 JavaCodegenTest 增用例，实际新建文件承载空枚举/常量 kind/replaceField 断言）；core 新增 `EnumCommentParserTest`（model 包）与 `EnumAnnotationTest`（gen 包，10 个 @enum 端到端）；`EndToEndTest` 旧断言改写、`AnnotationProcessorTest` 补 @enum 用例；`ParameterizedArtifactsTest` 核对无需改动。
  6. checkstyle 行长：测试 DDL 长字面量拆分多物理行处理。
- 验证：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿（tree 28 / core 97 / cli / maven-plugin 13，jacoco/spotbugs/checkerframework/checkstyle/error-prone/spotless 全进）；`new-change.sh check` + `check-docs` 通过。
- 记忆文档自检（新上下文实例，2026-09-06）：**合格**（十问全过 + 代码锚点抽查属实）；两处轻微观察已顺手修复——architecture 关键代码锚点表补 EnumHandler/EnumItem/EnumCommentParser；`@type`×`@enum` 冲突表述限定为「@enum 标注列」（与实现 `!column.isEnum()` 分支一致）。
