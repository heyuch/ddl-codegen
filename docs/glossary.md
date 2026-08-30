# 词汇表（Glossary）

统一项目术语：**代码标识符（英文）与文档中文表达一一对应**，消除中英混用与歧义。
文档写作与讨论默认使用「中文」列；代码/配置键使用「英文标识符」列。
条目出处为术语的权威定义位置；冲突时以本表为准并同步修正出处文档。

## 核心术语

| 英文标识符 | 中文 | 定义 | 出处 |
|---|---|---|---|
| artifact | 产物 | config `artifacts.*` 段定义的生成单元（顶层键第一段 = 产物名，自由定义）；**配置即启用** | README §config、docs/design.md §6 |
| generator / kind | 生成器 / 注册名 | `Generator` 接口实例；`kind()` 为 config `generator=<名>` 引用的注册名；一个产物对应一个生成器 | `gen/Generator`、docs/design.md §10 |
| 产物文件 / 生成物 | 产物文件 | 生成器产出的 `.java`/`.xml` 文件；路径 = 根 + module + package/资源路径 + 类名（config 推导） | docs/design.md §4 |
| reconcile | 增量同步 | 模型 vs 现有文件的 `@Generated` 成员级 diff：模型有文件无 → 增；有而模型无 → 删；签名变 → 替换；一致 → 跳过 | AGENTS.md「关键机制」 |
| `@Generated` | 成员所有权标记 | 工具只增删改带此注解的成员；**用户手写代码永不触碰**；解析失败不覆盖 | AGENTS.md「关键机制」 |
| DDL 注解 | DDL 注解 | 注释中 `@name[:value]`；内置 `@type`（复用类型）/ `@as`（类名覆盖）/ `@ignore`（模型剪枝）；未知注解 warning 忽略 | docs/design.md §9 |
| 剪枝 | 剪枝 | `@ignore` 注解的列/索引在解析应用后从模型移除（一处解决所有产物） | `StatementApplier.pruneIgnored` |
| Schema | 内存模型 | DDL 应用后的表模型（Table/Column/Index/Meta）；后续语句可见前面结果 | docs/design.md §5 |
| 模型 vs 树 | 模型 / 可修改 AST | 模型 = core `model` 包的领域对象（DDL 语义层）；树 = `ddl-codegen-tree` 的自研可修改 Java AST（源码表示层） | docs/design.md §3 |
| 门面 | 门面 | `Codegen.run(...)`：core 的统一入口（cli/maven-plugin 只调门面，不直接碰生成器/树） | AGENTS.md 架构 |
| 查询契约 | 查询契约 | 生成器查询方法 `className/fieldName/fieldType`：跨产物引用的准确值来源，95% 由命名推导覆盖 | `gen/Generator`、docs/design.md §12 |
| TableContext / GenerationContext | 表上下文 / 全局上下文 | 单表生成上下文（表模型 + artifact 配置 + 命名/类型服务）/ 一次生成执行的全局上下文（配置、共享服务、变更报告） | `gen/TableContext`、`gen/GenerationContext` |
| DdlOperation | DDL 操作（原子变更） | 规范化后的原子变更（建表/删表/改名/加列/改列/删列/加索引/删索引/索引改名）；每种操作自实现 `apply`（多态分发） | `ddl/DdlOperation`、docs/design.md §4 |
| StatementApplier | 应用器 | 把 DDL 操作按语句顺序应用到 Schema，产出 ApplyResult（受影响表/改名/删除记录） | AGENTS.md 数据流 |
| ChangeReport / ChangeStatus | 变更报告 / 变更状态 | 逐文件 CREATED/UPDATED/UNCHANGED/DELETED 摘要 + 警告；无变化不写盘 | `io/ChangeReport` |
| SPI 扩展点 | 扩展点（SPI） | `Generator` 接口 = 唯一生成器扩展点；`DdlParser`/`ConfigLoader`/`TableNameStrategy` 为可替换 SPI | AGENTS.md「扩展点」 |

## 构建与流程

| 英文标识符 | 中文 | 定义 | 出处 |
|---|---|---|---|
| 门禁 | 门禁 | 静态检查硬门槛：spotless/checkstyle（error）/error-prone/checkerframework（error）/spotbugs（字节码）/jacoco（line ≥75%）全部进 `mvn clean test` | AGENTS.md「Quality Bar」 |
| quick build | 快速构建 | `-Pquick` profile：跳过编译期分析器/spotbugs/jacoco/checkstyle，保留格式化与测试（37s → 11s）；提交/验收前必须跑全量 | AGENTS.md「Build」 |
| 全量构建 | 全量构建 | 无 profile 的 `mvn clean test`：全部门禁 + 106+ 测试 | AGENTS.md「Build」 |
| design-first | 设计优先 | 项目工作流：分析 → 设计文档（docs/changes/）→ 用户评审 → 实现 → 验证 → 文档同步 | AGENTS.md「开发工作流」 |
| PIT | 变异测试 | 变异覆盖率（击杀突变体比例）验证测试质量；显式运行（core 模块） | docs/design.md §15 |

## 约定（写作与讨论时统一）

- 中文文档一律用「中文」列词（产物、生成器、增量同步、门禁…），不直接夹杂英文标识符（代码/配置键除外）
- 代码/提交信息使用「英文标识符」列词（artifact、generator、reconcile…）
- 歧义场景：提到「模型」默认指 core model 包；需要源码表示时用「树」；提到「产物」默认指 config artifact
