# DDL Codegen 任务列表

> 执行顺序：M0 是地基（tree lib），必须先完成并全绿；M1→M2→M3 是框架核心；M4 收尾。
> 技术方案见 `DESIGN.md`。每项完成后更新勾选状态。

## M0：ddl-codegen-tree 提取与改造（前置，正确性红线）

### M0.1 模块提取
- [ ] 0.1.1 建 `ddl-codegen-tree` 模块（parent 转多模块，插件配置迁 pluginManagement）
- [ ] 0.1.2 迁移 tree 包（`hyc.codegen.tree`）+ `utils/CodePrinter` + `utils/U`，移除对 codegen-groovy 其余代码的依赖
- [ ] 0.1.3 迁移现有测试：JavaParserTest / JavaCodegenTest / JavadocCodegenTest / Demo / RoundTripSmokeTest
- [ ] 0.1.4 迁移后 `mvn validate` + 测试全绿

### M0.2 保真层：表达式全局兜底
- [ ] 0.2.1 JavaCodegen 增加兜底：未显式处理的节点 → `print(node.toString())`
- [ ] 0.2.2 用 RoundTripSmokeTest 验证：member select（`System.lineSeparator()`）、方法调用、二元运算不再丢失

### M0.3 保真层：语句打印
- [ ] 0.3.1 visitBlock 局部变量按语句语义打印（补分号）
- [ ] 0.3.2 toString 语句缩进归一化（复用 SourceBlock 缩进对齐逻辑），块内缩进不再塌缩
- [ ] 0.3.3 验证：块内多语句、嵌套 if/for 的 round-trip

### M0.4 保真层：数组与 varargs
- [ ] 0.4.1 实现 ArrayTypeTree 打印（`String[]`、`Object...`）
- [ ] 0.4.2 验证 varargs 签名 round-trip

### M0.5 小 bug 修复
- [ ] 0.5.1 `Method.getTypeParameters()` 返回真实字段（当前 `List.of()`）
- [ ] 0.5.2 排查同类 getter/字段不一致（Class 的 typeParameters 等）
- [ ] 0.5.3 验证 throws 子句、注解参数（`@X(a=1)`）round-trip

### M0.6 生成助手：Expr / Block
- [ ] 0.6.1 `Expr` 助手：call / member / ternary / nullSafe / literal（返回字符串）
- [ ] 0.6.2 `Block` 助手：if / for / 平铺语句序列（缩进由 SourceBlock 对齐）
- [ ] 0.6.3 **import 登记**：助手引用的类型（TypeReference）登记进 import 收集器
- [ ] 0.6.4 测试：用助手构建 `toEntity` 方法体（new + set + enum 三元转换 + return）

### M0.7 质量优化（开源项目标准，用户要求）
- [ ] 0.7.1 模型类全部私有字段 + getter/setter（接口 getter 保留）
- [ ] 0.7.2 builder 统一：只留 Class/Method/Variable，API 签名一致（type/name/annotation/modifiers）
- [ ] 0.7.3 防御性拷贝统一（集合 getter 一律 `new ArrayList<>(field)`）
- [ ] 0.7.4 公共 API 全 javadoc
- [ ] 0.7.5 命名审查：`U`/`Types` 等无意义类名改名；类组织审查（模型/转换/打印分层清晰）
- [ ] 0.7.6 拆分 JavaCodegen 的 import 管理为独立类（ImportManager），职责单一
- [ ] 0.7.7 移除死代码/重复逻辑
- [ ] 0.7.8 `mvn validate`（spotless/checkstyle）全绿

### M0.8 golden 测试转正
- [ ] 0.8.1 round-trip golden 测试集：复杂构造文件（varargs/lambda/三元/内部类/泛型/注解参数/throws）
- [ ] 0.8.2 生成侧单元测试迁移转绿（JavaCodegenTest/JavadocCodegenTest）
- [ ] 0.8.3 M0 完成：`mvn clean test` 全绿

## M1：core — DDL 处理与模型

- [ ] 1.1 SchemaModel：Table / Column / Index + 开放 meta map + 注释原文保留
- [ ] 1.2 Druid 解析适配：create / alter / drop / rename 语句 → 模型变更（Parser SPI）
- [ ] 1.3 语句顺序应用（内存累积，后续语句可见前面结果）；rename 检测（改类名 + 移动文件）
- [ ] 1.4 注解解析：严格 `@name[:value]`；内置 type/as/ignore；未知/解析失败 → warning + 忽略；
      DdlAnnotationHandler SPI + config 注册
- [ ] 1.5 Config：Properties 加载器（ConfigLoader SPI）；artifacts.* 启停；module；naming；types；
      annotations.custom
- [ ] 1.6 命名：变换链（前缀剥离/分表后缀/camelCase/关键字处理/按 artifact 后缀/方法名前缀/enum 命名）；
      TableNameStrategy SPI
- [ ] 1.7 类型映射：按 artifact；POJO 固定映射；enum → Enum 类/String；@type 覆盖；jdbcType 映射表
- [ ] 1.8 文件 IO：路径解析（根 + module + package / XML path）；字节比对不写盘；执行报告结构

## M2：生成核心

- [ ] 2.1 ArtifactGenerator SPI + AbstractJavaArtifactGenerator（@Generated 成员 reconcile 循环：
      缺→增 / 多余→删 / 签名类型不符→替换 / 一致→跳过）
- [ ] 2.2 ArtifactInterceptor SPI（只动 @Generated 成员 + import；幂等）+ 内置 lombok、jsr303
- [ ] 2.3 两阶段生成（阶段 A 发布描述 → 阶段 B 生成文件）+ ArtifactRegistry 兜底 +
      PO 类型推导（pojo 未启用 → mapper 用 Entity）
- [ ] 2.4 拦截器/生成器/注解处理器的组合测试

## M3：内置生成器（每个都配 golden 测试）

- [ ] 3.1 Entity：enum 类型、@Generated 成员、@type/@as/@ignore、DDL 列序
- [ ] 3.2 Enum：常量、fromValue(String) switch、@as 命名
- [ ] 3.3 Pojo：固定基础类型
- [ ] 3.4 Mapper 接口：findBy*/@Nullable/List/@Param、索引 @ignore
- [ ] 3.5 MapperXml：resultMap / BaseColumnList / insert / delete / update / selectById / findBy*、
      jdbcType、t. 别名、id 与接口一致
- [ ] 3.6 Repository 接口 + RepositoryImpl（mapper→converter→entity、enum 参数转换、DI 按 config）
- [ ] 3.7 Converter（plain）：逐字段赋值 + enum 三元转换（用 Expr/Block 助手 + import 登记）
- [ ] 3.8 全链路 golden 测试（示例 DDL → 全部 artifact 文件对比）

## M4：CLI 与收尾

- [ ] 4.1 CLI：--config / --ddl / --dry-run / --sync（手写参数解析，~40 行）
- [ ] 4.2 报告输出（逐文件 +n/-m/~k + 警告）；--dry-run 不写盘
- [ ] 4.3 `--sync` 模式：对账磁盘生成物与输入
- [ ] 4.4 文档：config 参考 / 注解参考 / 扩展指南（SPI 三件套示例）
- [ ] 4.5 端到端验收：真实 DDL 跑 create / alter / drop / 重跑幂等 / 用户手写代码保留
- [ ] 4.6 `mvn clean test` + `mvn validate` 全绿收尾

## 备注

- M0 源代码位置：`/Users/humpy/code/java/codegen-groovy/codegen-core/src/main/java/hyc/codegen/core/tree`
  （连同 `utils/CodePrinter.java`、`utils/U.java`、`src/test/java/hyc/codegen/core/tree/` 下测试一起迁移）
- 每步提交前跑 `mvn validate`（spotless/checkstyle 绑定 validate）
