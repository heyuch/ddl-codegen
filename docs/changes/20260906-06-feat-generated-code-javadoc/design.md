# generated-code-javadoc

> 变更号：20260906-06　变更类型：feat（产物输出变化 → breaking 级产物更新，同步 golden fixtures）　目录：`docs/changes/20260906-06-feat-generated-code-javadoc/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

生成的代码目前无 javadoc（2026-09-06 用户指出；此前 04 刚解决「可编译」）。DDL 注释（表/列）是语义来源，但只进了 meta/枚举列表，未落产物。用户决策：**类、方法、属性三层都要 javadoc**（全部 Java 产物，XML 无注释载体除外）。

## 语义来源与清洗

- 注释清洗（新 `CommentDocs` 工具，core model/或 gen 包）：剥除全部 `@name[:value]` 注解 token 与 `{code}={desc}(name)` 枚举项 token → trim；空 → 无 javadoc。列注释示例「状态 1=初始(INIT) @enum:Status」→「状态」；「用户名」→「用户名」。
- 表注释同理（「用户表」）。

## javadoc 落点规则

| 元素 | 来源 | 内容 |
|---|---|---|
| 类（pojo/enum/mapper/repository/repositoryImpl/converter） | 表注释清洗 | 表注释文本；枚举类若表注释空则退列注释清洗（前缀段） |
| 字段：pojo 实体/po 各列 | 列注释清洗 | 列注释文本 |
| 字段：repositoryImpl 注入的 mapper/converter | 模板 | 「{表注释} Mapper / 转换器」 |
| 枚举常量 | 项 desc | desc 文本（有则加） |
| 枚举字段 code/desc | 模板 | 「code - 数据库存储值」「desc - 枚举值描述」 |
| 枚举方法 fromCodeNullable/fromCode | 模板 | 宽松/严格反查语义（null 返回/抛异常） |
| pojo 方法（lombok 生成 getter/setter） | — | lombok 注解生成、无源码方法，不加 |
| Mapper/Repository/RepositoryImpl 方法 | 模板 + 列注释 | insert/update/deleteById/findById/findByX*（含 @param 列注释、@return 语义）；Impl @Override 方法继承接口 javadoc 可省略（@Override 已注解）→ 加简短说明亦可，实现统一模板 |
| Converter 方法 | 模板 | toX/toXList 方向说明 |

模板统一中文、摘要句 + 必要时 `@param/@return`（列级注释进 @param）。方法模板与现有查询语义一致（见 gen 生成器现状）。

## 方案取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：清洗后 DDL 注释直出（表→类、列→字段）+ 模板补方法/枚举 | 内容有源、无噪音；方法可读 | 模板文案需统一评审 | **选定** |
| B：原文（含注解/列表）直出 | 实现最简 | javadoc 充斥配置噪音 | 否决 |
| C：仅 pojo/enum | 面小 | 与方法/接口断层，用户已否（三层都要） | 否决 |

- 实现侧：tree 已支持成员 javadoc（Class/Method/Variable javadoc + DocComment builder，JavaCodegenTest 实证）；各生成器在 buildClass/buildEnum/方法构建处附加 DocComment。
- 同步：02/05 golden fixtures 全量重出（有意识产物契约变更）；04 的 @Generated/final 已基线化，本变更在其上追加 javadoc 行。

## 改动文件与影响面

- core main：新 `gen/` 或 `model/CommentDocs`（清洗工具）；`PojoGenerator`/`EnumGenerator`/`MapperGenerator`/`MapperXmlGenerator`?（XML 无）/`RepositoryGenerator`/`MybatisRepositoryImplGenerator`/`ConverterGenerator` 加 javadoc 附加；方法模板常量。
- 测试：`CommentDocsTest`；全量 golden fixtures 重出并审阅；core/模块门禁回归。
- 影响：**产物字节变化**（类/方法/字段 javadoc 行）；tree 打印与 reconcile 成员签名（方法签名含 body——javadoc 不在签名内，成员匹配不受影响；常量/字段同）→ 对已存在文件新增 javadoc 需删文件重生成（类级注释/成员 javadoc 不 reconcile，记录边界同 04）。
- 文档：architecture 生成器表/速查一句（产物含 DDL 注释派生 javadoc）+ README 一句 + 索引行。

## 验收标准

1. 全量 golden fixtures 重出后：pojo/enum/mapper/repository/repositoryImpl/converter 各类、字段、方法均含 javadoc；注释含 @enum/列表的列 javadoc 为清洗后前缀文本；无注释处无空 javadoc。
2. `mvn clean test` 全 reactor 绿；`new-change.sh check`。
3. 幂等：重跑两次无变化（javadoc 文本稳定）。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`；golden fixtures 重出 diff 审阅；`new-change.sh check`/`check-docs`（改 architecture/README 时）
