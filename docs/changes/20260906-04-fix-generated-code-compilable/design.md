# generated-code-compilable

> 变更号：20260906-04　变更类型：fix（生成物字节变化 → breaking 级产物更新，同步 golden fixtures）　目录：`docs/changes/20260906-04-fix-generated-code-compilable/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

20260906-03 真实 Spring Boot 模块首次对生成物做真实 javac 编译，立即暴露两个**生成代码不可编译/不可继承设计缺陷**（此前 e2e 只读文本、从不编译）：
1. **`@Generated` 缺必填 value**：`javax.annotation.processing.Generated` 的 `value()` 元素无默认值；生成器 `GeneratedSupport.mark` 用 `Annotation.of(GENERATED)` 裸注解 → 所有成员编译报「缺少默认值」。
2. **DesignForExtension**：`MybatisRepositoryImplGenerator`/`ConverterGenerator` 生成 public 非 final 具实现类（leaf 实现、不应被继承）→ 门禁违规（本应是 final）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：@Generated 改用其它零参数注解 | 无 value | 换注解偏离 JDK 惯例/生态 | 否决 |
| B：`@Generated("ddl-codegen")` 显式 value + impl/converter 生成 final 类 | 生成物可编译、符合设计规则；03 模块门禁豁免随之移除 | 产物字节变化 → 全量 golden fixtures 更新（有意识契约变更，随 04 审阅） | **选定** |

## 方案

- `gen/GeneratedSupport.mark`：`Annotation.of(GENERATED, "\"ddl-codegen\"")`（value = 工具名）。
- `gen/AbstractJavaGenerator`：新增 `protected boolean finalClass()`（缺省 false），`buildFresh` 在 finalClass 时为类加 `Modifier.FINAL`；`MybatisRepositoryImplGenerator`/`ConverterGenerator` 覆写返回 true。
- 同步：全量 golden fixtures 经 `-Dgolden.update` 重出并审阅 diff（02 套件）；03 模块生成物再生成；03 模块 DesignForExtension 豁免移除。
- 不修（记 progress/另立）：保留字反引号剥除、mapper 单值 findBy @Nullable→converter 非空传参 NPE（03 已知，04 聚焦可编译/设计规则两项）。

## 改动文件与影响面

- core main：`gen/GeneratedSupport.java`、`gen/AbstractJavaGenerator.java`、`gen/MybatisRepositoryImplGenerator.java`、`gen/ConverterGenerator.java`
- 测试/fixtures：02 全量 golden fixtures 重出（有意识产物契约更新）；core 全量测试含 checker/spotbugs/checkstyle 回归
- 03 模块：生成物再生成 + DesignForExtension 豁免移除
- 影响：**生成物字节变化**（@Generated 带 value、impl/converter class final）——breaking 级产物更新；EndToEnd 文本断言与 golden fixtures 同步
- 文档：changes/README 索引行（04，注明产物变化）；无记忆文档事实变更（architecture 生成器行提一句 final/标记值）→ 收尾同步

## 验收标准

1. `mvn clean test` 全 reactor 绿（含 02 golden、03 模块编译+门禁[豁免移除后]）。
2. 任一生成文件成员注解为 `@Generated("ddl-codegen")`；UserRepositoryImpl/UserConverter 为 `public final class`。
3. golden fixtures 与实现一致（重出 diff 审阅后零漂移）；`-Dgolden.update` 关闭状态下测试绿。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`；`new-change.sh check` + `check-docs`（如改 architecture/索引）
