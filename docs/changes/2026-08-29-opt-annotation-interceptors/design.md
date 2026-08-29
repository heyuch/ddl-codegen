# opt-annotation-interceptors：DDL 注解处理重构（开闭原则）

## 背景与问题

现有注解的作用域与处理位置分散，违反开闭原则：

| 注解 | 作用域 | 当前处理位置 | 问题 |
|---|---|---|---|
| `@ignore` | 所有产物的字段/索引 | 生成器 8 处检查 `IgnoreSupport.isIgnored` | 分散、易漏（漏一处 = 忽略字段泄漏进产物）；应是结构性剪枝而非生成期判断 |
| `@type` | 实体视图（use:enums 产物） | 耦合在 EnumsInterceptor 里 | 与 enums 语义耦合；作用域靠"实体视图"魔法 |
| `@as` | 列：枚举类名；表：基类名 | TableContext 命名层读 meta["as"] | 命名层读注解，职责错位；枚举撞名已有 `naming.enum.style=tableColumn` 覆盖 |

目标：**解析器只做通用注解解析（@name:value → meta，无语义），语义全部由拦截器/结构剪枝承担**——新注解 = 新拦截器，零核心改动。

## 可选方案与取舍

| 方案 | 结论 |
|---|---|
| `@ignore`：A) 生成器逐个检查（现状）；B) **解析后从模型剪枝** | **推荐 B**：单一执行点，模型里不存在 = 所有生成器自动跳过，删除 IgnoreSupport 与 8 处使用 |
| `@type`：A) 耦合在 EnumsInterceptor；B) **独立 TypeInterceptor（use: type）** | **推荐 B**：作用域 = config 显式（po 不配 type 即不生效），"entity/po 级别"从魔法变配置；Enums 只管 enum→类 |
| `@as`：A) 保留；B) **移除** | **推荐 B**：列级（枚举名）撞名场景由 naming.enum.style 覆盖，逐列逃生口 YAGNI；表级（基类名）无实际价值 |

## 非目标（延后，另行设计）

- **converter 的 po/entity 类型差异转换逻辑不在本次范围**：@type 列经 converter 的映射保持现状
  （直接赋值，可能产生类型不匹配——与重构前行为一致，无回归）；po/entity 类型不同时的转换策略
  （如 String↔UserExtInfo 如何映射）由用户单独设计，本次只保证注解处理侧的结构正确。

## 方案

### 1. @ignore → 解析后剪枝（结构性）

`StatementApplier.apply` 应用完所有操作后，遍历 schema 各表，移除 `meta["ignore"]` 的列与索引
（`Table.removeColumn/removeIndex` 已存在）。模型即净化：XML/字段/方法/转换器全部自动排除。

### 2. @type → TypeInterceptor（use: type，独立拦截器）

```java
public final class TypeInterceptor implements ArtifactInterceptor {
    // onField/onParam：列 meta 有 "type" → 类型改写为 JavaTypes.typeTree(value)
    // 值 = Java 类型字符串（FQN/简单名/primitive/byte[] 均可）
}
```

- 产物 use 含 `type` 才生效（entity.use=lombok,jsr303,enums,type；po 不配即用 JDBC 映射类型）
- EnumsInterceptor 去掉 @type 分支：只做 enum→类，**含 @type 的列跳过**（避免覆盖显式类型）

### 3. @as → 移除

- 删除 AsHandler；TableContext 两处 meta["as"] 读取（枚举类名、基类名）删除
- 枚举类名完全由 naming（`naming.enum.style` + 命名策略）决定

### 解析层不变

`AnnotationParser`/`AnnotationProcessor` 已是通用解析（@name:value → meta，未知注解 warning 忽略）——保持；
内置 handler 集从 {type, as, ignore} 变 {type, ignore}。

## 改动文件与影响面

- `StatementApplier`：应用后剪枝 @ignore（新增私有方法）
- 新增 `interceptor/TypeInterceptor.java`；`interceptor/EnumsInterceptor.java` 简化（去 @type 分支 + 跳过 @type 列）
- 删除 `annotation/AsHandler.java`、`gen/IgnoreSupport.java`；`TableContext` 去 meta["as"] 读取
- Codegen 门面 + 测试注册表：注册 TypeInterceptor
- 测试更新：EndToEndTest（@as 移除 → Gender）、DruidDdlParserTest（@as 用例删除/改写）、
  ParameterizedArtifactsTest（typeAnnotationScopedByEnumView 改用 use: type）、AnnotationProcessorTest
- 影响：@as 注解不再可用（行为移除）；@type 作用域从"use:enums 产物"变为"use:type 产物"（config 变更）

## 验证

- 全量 `mvn clean test` + 插件 IT 绿
- 新增断言：@ignore 剪枝后模型无该列/索引（StatementApplier 测试）；TypeInterceptor 在 use:type 产物生效、未配置产物不生效
- 运行 PIT 关注 TypeInterceptor 与剪枝逻辑
