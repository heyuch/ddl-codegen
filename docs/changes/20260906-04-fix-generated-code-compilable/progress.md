# 变更台账：20260906-04-fix-generated-code-compilable

## 状态

**实现完成，收尾中**（2026-09-06：代码修复 + core 门禁绿 + 02 fixtures 重出；commit 待落）

## 评审记录

- 变更经 03 模块真实编译驱动发现（用户决策链：03 门禁=生成器质量验收器）：无需额外独立评审（缺陷客观、修复最小），由用户既有执行授权覆盖；design.md 记录取舍。

## 已知 / 边界（随本变更记录）

- 类级修饰符不 reconcile（既有边界，20260906-01 已述）：@Generated value 与 final 对**已存在**生成文件不生效 → 升级需删文件重生成（03 模块初测实证）。
- 另立待办：保留字反引号剥除、mapper 单值 findBy @Nullable→converter 非空传参 NPE（03 已知）。

## 实现记录

- `GeneratedSupport.mark` → `@Generated("ddl-codegen")`（value 必填）；`AbstractJavaGenerator` 增 `finalClass()` 钩子 + buildFresh 加 FINAL；`MybatisRepositoryImplGenerator`/`ConverterGenerator` final。
- 02 golden fixtures 全量重出（124 行：@Generated value + impl/converter final）并审阅；core 全量门禁 119 绿；EndToEnd/EnumAnnotation 文本断言兼容（contains 未受影响）。
- 03 模块再生成实证：@Generated value 生效、final 需删文件重建（reconcile 不动类级修饰符）。
