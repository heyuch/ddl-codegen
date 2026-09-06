# 项目现状架构

> 事实以代码对账为准：变更后按文末「代码锚点表」重新核对；禁止散文式追加。
> 历史基线 `docs/changes/20260801-01-feat-project-foundation/design.md` 已部分被后续变更取代，不作为事实来源。

## 模块与依赖方向

```
ddl-codegen-cli（命令行入口） ──────────────┐
                                          ▼
ddl-codegen-maven-plugin（Maven Mojo）──► ddl-codegen-core（框架）──► ddl-codegen-tree（自研 Java AST）
```

| 模块 | 职责 | 依赖 | 入口 |
|---|---|---|---|
| `ddl-codegen-tree` | 自研可修改 Java AST：解析（JDK `javax.tools`/`com.sun.source`）/ 构建 / 打印 | 零第三方运行时依赖（lombok/spotbugs/checker-qual 均 provided） | — |
| `ddl-codegen-core` | 框架本体：配置/解析/命名/类型/生成器/写盘 | `tree`；运行时外部依赖仅 druid（DDL 解析） | `Codegen`（门面） |
| `ddl-codegen-cli` | 命令行：`--config/--ddl/--dry-run`，shade fat jar | `core`（不直接依赖 tree） | `hyc.codegen.cli.Main` |
| `ddl-codegen-maven-plugin` | `mvn ddl-codegen:generate`（不绑生命周期，显式调用），支持 ddl 内联 / ddlFile（含行范围）/ dryRun / skip | `core` + maven-plugin-api | `GenerateMojo` |
| `ddl-codegen-it-springboot` | 真实消费工程（20260906-03）：Boot 2.7 + MyBatis + MySQL[Testcontainers]；DDL/config 生成全链路代码提交入库，验证可编译/装配/调用（IT 无 Docker 自动 skip） | spring-boot/mybatis-starter/mysql/lombok/checker-qual/spotbugs-annotations（lombok @Builder 生成引用）+ testcontainers | `SampleApplication` |

模块/包依赖方向由 Maven 边界 + 各模块 `ArchitectureTest`（ArchUnit）强制：cli/plugin 不得依赖 tree；core 包分层——叶子（`model/config/io`）无内部依赖，低层（`annotation/naming/types/ddl`）只向下，`gen` 是顶层，core 包间无循环。

## 运行管线（现版，无拦截器）

```
DDL 文本（多条语句，分号分隔）
 → PropertiesConfigLoader.load(configFile)         # 项目根 = config 所在目录；产物顺序 = 配置文件行序
 → 反射实例化 annotations.custom 注解处理器（无参构造）
 → DruidDdlParser(AnnotationProcessor(builtin + custom))   # Druid 解析 MySQL 方言
 → DdlOperation 序列（规范化原子操作，按语句顺序）
 → StatementApplier.apply(Schema, ops)            # 空 Schema 起步，就地应用
     └ apply 后统一 pruneIgnored：meta["ignore"] 的列/索引从模型移除（模型级剪枝）
 → ApplyResult（受影响表按首次变更序去重 / drop / rename 记录）
 → CodeGenerator.generate(config, schema, result)
     ├ DROP          → 删除该表全部启用产物文件
     ├ RENAME        → 保留旧文件（用户手写代码），新表名走正常生成；类名变化 → warning 提示手动迁移
     └ 受影响表 × 逐产物（config 出现顺序）→ 生成器
         ├ Java 类产物：AbstractJavaGenerator（定位 → 解析现有源 → 只 reconcile @Generated 成员 → 打印）
         └ XML 产物：整文件字符串模板重生成
 → FileWriter.writeIfChanged（字节比对，内容不变不写盘；dry-run 只算状态不落盘）
 → 逐文件变更条目（ChangeStatus）+ warning → ChangeReport
```

- **增量语义**：`Schema` 每次从空起步，本批次 DDL 描述目标状态；生成只覆盖 `ApplyResult.affectedTables`（本批次变更过的表），未涉及的表与文件完全不触碰。
- **drop 语义**（代码实证 `CodeGenerator.handleDrops`）：drop table → 删除该表所有启用产物文件（Java 类 = 根/module/package/类名；XML = 根/module/path/mapper 类名.xml）。列/索引 drop 只改模型，下次生成时由 reconcile 删对应 @Generated 成员/方法。
- **rename 语义**（代码实证 `CodeGenerator.handleRenames`）：表改名 → **保留旧表名产物文件**（含用户手写代码），新表名正常生成；任一产物类名变化时记 warning 提示「迁移手写内容后手动删除旧文件」。列/索引改名 → 表内原地替换（`Schema.renameTable` / `Table.renameColumn`/`renameIndex`），文件层由 reconcile 按新成员名对齐。
- 无拦截器 / 无 `use` 链：产物特性为生成器内部选项（见「生成器体系」「config schema」）。
- 变更状态：`CREATED`（新建）/ `UPDATED`（覆盖）/ `UNCHANGED`（一致未写盘）/ `DELETED`；报告摘要形如 `+N ~M -D =U`。

## 生成器体系

**Generator SPI**（`hyc.codegen.core.gen.Generator`，唯一扩展点，Java 类继承 `AbstractJavaGenerator`）：

| 方法 | 契约 |
|---|---|
| `className(TableContext)` / `fieldName(Column, TableContext)` / `fieldType(Column, TableContext)` | **查询契约**：跨产物引用的准确值来源——每个生成器实现自己的视图；用不上的方法直接抛 `UnsupportedOperationException`（XML 生成器三方法全抛） |
| `generate(TableContext, GenerationContext)` | 生成/更新该表该产物文件 |
| `kind()` | 注册名，即 config `generator=<名>` 的引用值 |

**内置生成器**（`Codegen.defaultGenerators()` 注册 7 个，按 kind 入 Map；类文件见锚点表）：

| 类 | kind | 典型产物（config 名，自由定义） | 说明 |
|---|---|---|---|
| `PojoGenerator` | `pojo` | entity / po / dto | 纯字段类，一表一文件；类名 = 基类名 + suffix；特性选项 lombok/serializable/jsr303/jsr305/enums/type |
| `EnumGenerator` | `enum` | enum | 表内每个**枚举列**生成一个枚举类（一表多文件）；枚举列 = SQL `enum(...)` 类型列或 `@enum` 标注列（`Column.isEnumColumn()`）。模板：常量 = 枚举项 + code/desc 双字段 + `fromCodeNullable`/`fromCode` 反查方法对（`@Nullable` 用 `annotations.nullable`）；产物选项 `lombok=true` → 类级 `@Getter`+`@RequiredArgsConstructor`，缺省 → 手写私有构造器与 `getCode()/getDesc()`。枚举项数据在常量 init、参与 reconcile 签名（ALTER comment 后增/删/同名 code/desc 值改均增量同步，见契约）；无枚举列时 `shouldGenerate=false` → 删除旧文件 |
| `MapperGenerator` | `mybatisMapper` | mapper | Mapper 接口：insert/update + 有主键时 deleteById + 索引派生 findBy*；返回类型 = `target` 引用 |
| `MapperXmlGenerator` | `mybatisXml` | mapperXml / xml | 整文件字符串模板重生成（无 reconcile）；文件名为其 `mapper` 引用产物类名 + `.xml`；必配 `path`；结构 BaseResultMap/BaseColumnList/insert/deleteById/update/findById/findBy* |
| `RepositoryGenerator` | `repository` | repository | Repository 接口：索引派生 findBy*（`target` 视图） |
| `MybatisRepositoryImplGenerator` | `mybatisRepositoryImpl` | repositoryImpl | 桥接 Mapper →（Converter）→ target；`di=field`（默认 @Resource 字段注入）或 `di=constructor`；mapper.target == 自己 target → 直连，否则经 converter（校验 converter.source==mapper.target 且 converter.target==自己 target） |
| `ConverterGenerator` | `converter` | converter | source ↔ target 双向 `toX` / `toXList`；枚举列按两端产物视图差异自动插 `fromCode`/`getCode()` 空安全转换（视图判定 = 枚举类 FQN 精确比对，非 String 启发式；标量→enum 用 nullSafe + `fromCode`，保持未知 code 抛错语义） |

产物（artifact）≠ 生成器：产物名是 config 顶层键、自由定义；「配置了即启用」。**同一 kind 可服务多个产物实例**（如 `entity` 与 `po` 均 `generator=pojo`，各自配 package/suffix/特性）；`CodeGenerator` 每 kind 持一个实例，由 `TableContext`（表 × 产物配置）驱动，实例不存表态。

**生成物 javadoc（20260906-06）**：Java 产物类/字段/方法均带 javadoc——类取表注释（enum 空则退列注释）、字段取列注释、方法取模板/列注释摘要；注释先经 `gen.CommentDocs` 清洗（剥注解与枚举项 token）。成员标记 `@Generated("ddl-codegen")`；impl/converter 为 `final`（20260906-04）。

**注册与多实例/缺省规则**（`CodeGenerator` / `GenerationContext.resolveReference`）：
- 产物未配 `generator` → warning 跳过；配的 kind 未注册 → warning 跳过（门面只组合内置 7 个；`CodeGenerator` 构造器接受任意 Generator 列表，自定义 kind 需自行组装，不经 `Codegen.run`）。
- 跨产物引用解析：显式引用（`source`/`target` 属性，或任意选项键如 `mapper`/`converter`/`repository`）→ 指定产物；**缺省 → 默认生成器（多为 pojo）的唯一实例**；0 个或 >1 个实例时未显式配置 → 明确报错（如 po 与 entity 并存时 mapper 必须写 `target=...`）。
- 被引用产物未配置 → 报错（`requireArtifact`）。
- **enum 实例唯一性约束**：`enumPackage()` 要求 generator=enum 的产物 ≤1；某产物开 `enums=true` 时要求恰好 1 个 enum 产物（`enumPackageFor`）。
- 索引 → 查询方法按最左前缀逐级拆分（`QueryMethods`）：唯一键且参数 = 全部列 → 单值返回（标注 @Nullable），其余 → List。

## 注解体系

**语法**（`AnnotationParser`，严格）：DDL 注释文本内 `@` 后跟标识符为注解名，`@name` 或 `@name:value`；value 不含空白与 `@`、非空；一个注释可含多个注解；**无隐式简写**（`@boolean` ≠ `@type:boolean`）。注释中的邮箱（`foo@bar.com`）会被当未知注解提取，warning 忽略。**解析时机 = DruidDdlParser 转换语句时**（表/列/索引注释 → 各节点 `Meta`，`AnnotationProcessor` 分发）。未知注解名 / 位置不符 → warning 忽略，不中断生成。

**内置 handler**（`AnnotationRegistry.builtin()`，注册顺序 type/as/ignore/enum）：

| 注解 | Handler | 允许位置 | 写入 meta | 语义（消费方） |
|---|---|---|---|---|
| `@type[:<FQN或简单名>]` | `TypeHandler` | COLUMN | 列 `meta["type"]` | 列类型覆盖：**由 pojo 类产物在 `type=true` 时优先采用**（`PojoGenerator.fieldType`），不生成、不校验类型存在 |
| `@as[:<名>]` | `AsHandler` | TABLE + COLUMN | 各自 `meta["as"]` | 表级：覆盖该表所有产物的**基类名**（类名 = as 值 + artifact suffix，`TableContext.className`）；列级：覆盖该列**枚举类名**（`TableContext.enumClassName`） |
| `@enum[:<枚举类名>]` | `EnumHandler` | COLUMN | 列 `meta["enum"]`（有值存字符串，裸注解存 `Boolean.TRUE` 占位） | 声明该列**按枚举处理**（`Column.isEnumColumn()`）：非 SQL-enum 列生成 code/desc 枚举类；类名 = `@as` > `@enum` 值 > 命名策略（裸 `@enum` 与 SQL-enum 列同走 `naming.enum.style`）；枚举项 `{code}={desc}({name})` 由 EnumGenerator 经 `model.EnumCommentParser` 从 comment 解析；`@enum` 标注列再带列级 `@type` → 报错 |
| `@ignore` | `IgnoreHandler` | COLUMN + INDEX | `meta["ignore"]` = true | 模型级剪枝：StatementApplier 应用完 `pruneIgnored` 把列/索引**从模型移除** → 所有产物（含 XML）与自定义生成器都不再见（一处解决）；索引忽略 = 不生成查询方法 |

**自定义注解**：config `annotations.custom` = 逗号分隔的 `DdlAnnotationHandler` 实现类名；Codegen 反射实例化（要求无参构造）后 `register` 进注册表，同时注入 `TypeMapper`。Handler SPI = `name()` / `parse(Meta, @Nullable String)` / `targets()` + 默认钩子 `resolveType(Column, String)`（自定义注解可在 SQL 映射后改写列默认类型）。

## config schema

配置文件 = 项目根 properties（UTF-8；`key=value` 或 `key: value`；`#`/`!` 注释）。顶层键按第一段分流：

- **保留命名空间**：`naming.*`、`annotations.*`（第一段非产物，`PropertiesConfigLoader.RESERVED`）。
- 其余顶层键**第一段 = 产物名**（自由定义）；产物顺序 = 配置文件行序（逐行扫描收集，Properties 无序）。

**产物键** `artifacts.<产物名>.<属性>`（代码：顶层直接 `<产物名>.<属性>`）：

| 属性 | 语义 |
|---|---|
| `generator` | 注册生成器名（见「生成器体系」清单）；配置了即启用 |
| `module` | 项目根下的一级子目录；空/缺省 = 根 |
| `package` | Java 包名（Java 类产物）；XML 产物省略、改用 `path` |
| `path` | 资源相对路径（XML 用） |
| `suffix` | 类名后缀（`user` → `User` + `Mapper` = `UserMapper`）；缺省空 |
| `source` / `target` | 跨产物引用（属性）；converter 用 `source`，mapper/repository/repositoryImpl/xml 用 `target`（缺省 = 唯一 pojo 实例） |
| 其余任意键 | 进 options（特性布尔 / 引用 / di） |

**特性与引用选项键**（生成器实际读取；布尔用 `Boolean.parseBoolean`，非字面 `true` 即 false）：

| 键 | 消费方 | 语义 |
|---|---|---|
| `lombok=true` | pojo | 类级 `@Data/@Builder/@NoArgsConstructor/@AllArgsConstructor` |
| `serializable=true` | pojo | `implements Serializable` + `serialVersionUID` |
| `jsr303=true` | pojo | NOT NULL→`@NotNull`；varchar/char→`@Size(max=n)`；decimal→`@Digits(integer=p-s, fraction=s)`（javax.validation.constraints） |
| `jsr305=true` | pojo | nullable 列 → `@Nullable`（FQN 见 `annotations.nullable`） |
| `enums=true` | pojo | 枚举列（SQL-enum 或 `@enum` 标注，`Column.isEnumColumn()`）字段类型 → 枚举类（要求 enum 产物唯一） |
| `lombok=true` | enum | 枚举类级 `@Getter`+`@RequiredArgsConstructor`（构造器/getter 由 lombok 生成）；缺省 false → 手写私有构造器 + `getCode()/getDesc()` 成员；选项翻转需删文件重生成（类级注解不 reconcile） |
| `type=true` | pojo | 列 `@type` 注解值覆盖类型 |
| `di=field\|constructor` | repositoryImpl | 注入方式；缺省 field（@Resource） |
| `mapper=<产物>` / `converter=<产物>` / `repository=<产物>` 等 | mybatisXml / repositoryImpl | 跨产物引用选项键（经 `resolveReference` 的任意 refKey 路径） |

**校验**：每个产物必须配置 `package` 或 `path` 之一，否则加载报错。

**naming.\***（默认值见 `DdlConfig` / `ConfigTest.defaultsApplied`）：

| 键 | 默认 | 语义 |
|---|---|---|
| `naming.table.stripPrefixes` | 空 | 逗号分隔表名前缀，剥除（大小写不敏感，依次匹配） |
| `naming.table.stripShardSuffix` | `false` | 是否剥分表后缀（`user_0` → `user`） |
| `naming.table.shardPattern` | `_\d+$` | 分表后缀正则（首个匹配处截断） |
| `naming.column.camelCase` | `true` | 列名 → camelCase 字段名 |
| `naming.column.keywordSuffix` | `_` | 命中保留字时的字段名后缀 |
| `naming.method.prefix` | `find` | 查询方法名前缀 |
| `naming.enum.style` | `column` | 枚举类命名风格：`column`（列名 Pascal）\| `tableColumn`（基类名 + 列名 Pascal） |

**annotations.\***：`annotations.custom`（逗号分隔 handler 类名）；`annotations.nullable`（默认 `org.checkerframework.checker.nullness.qual.Nullable`）。

## 关键契约

| 契约 | 内容（代码实证） |
|---|---|
| config 存在即启用 | 产物 = config 顶层键；文件位置 = config 推导（根 + module + package/资源 path + 类名），**无 manifest** |
| @Generated 成员所有权 | 工具只增删改带 `@Generated`（`javax.annotation.processing.Generated`，JDK 自带）的字段/方法成员；用户手写成员永不触碰；现有文件无期望类名 → 视为新建（用户迁移代码未改 config 后果自负）；包名/类名以 config 为准 |
| reconcile 即 diff | 字段按名匹配、方法按名匹配；替换判据：普通字段 = 类型，**enum 常量 = 类型 + init 规范化文本**（code/desc 值改触发原位替换、保持声明顺序）；方法 = 返回类型 + 参数类型序列 + 方法体（空白归一化）。模型有而文件无 → 增；有而模型无 → 删；签名变 → 替换；一致 → 跳过。期望成员名与既有**非 @Generated 手写成员**重复 → 跳过新增 + warning（保留用户版） |
| 解析失败不覆盖 | 现有源解析失败 → 抛 `IllegalStateException` 中止本次运行、该文件未修改；**当前无 `--force` 逃生口**（CLI/plugin 均无此选项） |
| 删除无条件 | drop table → 无条件删除该表全部启用产物文件；产物不再适用（enum 无 enum 列）→ 删除旧文件 |
| rename 保留用户代码 | 表改名保留旧表名产物文件（含手写代码），新表名正常生成，类名变化 → warning 提示手动迁移删除；列/索引改名 → reconcile 层替换成员 |
| 字节比对幂等 | FileWriter 写前比对，内容一致不写盘（UNCHANGED）；dry-run 只计算状态不落盘 |
| 只处理本批次 | 生成只覆盖受影响表（ApplyResult）；未在批次内出现的表文件完全不触碰 |
| 查询契约 | 跨产物值（类名/FQN/字段类型）一律由被引用产物自身的生成器提供（`refFqn`/`typeOf` 路由到该产物 `generator.*`），自定义生成器自实现视图 |
| 警告不中断 | 未知注解/位置不符/未配或未注册 generator/不识别的语句/不支持的 alter 子句 → warning 记录并继续 |
| 顺序保持 | Schema 内表/列/索引按 DDL 定义顺序（生成依赖列序）；同名替换保持位置，新增追加末尾 |

## 已知限制（现状缺口）

- `--sync` 模式未实现（需文件归属标记才能对账磁盘）
- 列失去枚举类型（SQL `enum(...)` 改型或 `@enum` 标注移除/列删除）后旧枚举文件不自动清理（shouldGenerate=false 只删当前类名文件；单列移除需手工删文件）
- SQL-enum 旧模板产物（`value()`/`fromValue` 时代）升级：模板整体换代 + 类级注解不 reconcile → 删除旧枚举文件重生成
- enum 产物 `lombok` 选项翻转（true↔false）对已存在文件失效（类级注解不 reconcile）→ 删文件重生成
- comment 枚举项**顺序重排**不追（reconcile 按名不按序）→ 需删文件重生成以对齐 `values()` 序
- 跨表同 enum 包枚举类名撞名不检测（生成器无状态；显式 `@enum:名`/`@as` 可避撞）
- 生成枚举类恒引用 `annotations.nullable` 配置的注解（缺省 checkerframework qual）→ 消费项目需具备所配注解的依赖
- merge 时删除成员不清理其 import（保守策略：不删可能被用户代码引用的 import）
- ALTER COLUMN SET/DROP DEFAULT、FK/CHECK、分区、FULLTEXT/SPATIAL 索引 → warning 跳过（不生成对应变更）
- 源码残留过时 javadoc 文案（"拦截器"残留、`CodeGenerator` RENAME 描述与实现矛盾等），待代码清理变更处理

## 命名与类型映射速查

**命名**（`NamingService`；`TableNameStrategy` 为逃生口，可整体替换表名 → 基类名逻辑）：
- 表名 → 基类名：剥前缀 →（可选）剥分表后缀 → snake→Pascal（`t_user_0` → `User`）。
- artifact 类名 = 基类名 + suffix；表级 `@as` 覆盖基类名。
- 列名 → 字段名：camelCase（`user_id` → `userId`；可关）；命中保留字（Java 关键字 + 常用 SQL 保留字全集见 `NamingService.RESERVED_WORDS`，如 `order`）→ 追加 keywordSuffix。
- 索引 → 查询方法名：`前缀 + By + 列 camelCase 以 And 连接`（`name, gender` → `findByNameAndGender`）。
- 枚举类名：列级 `@as` > `@enum` 显式值 > `naming.enum.style`（SQL-enum 列与裸 `@enum` 列同走该函数：column 风格 = 列 Pascal，tableColumn = 基类名 + 列 Pascal）。

**类型映射**（`TypeMapper`，返回全限定名；SQL→Java 内置表不进 config）：
- 整数：`smallint/mediumint/int/integer/year` → `Integer`（**unsigned → `Long`**，防溢出）；`bigint` → `Long`；`tinyint(1)` → `Boolean`、其余 `tinyint` → `Integer`。
- 小数：`decimal/numeric` → `BigDecimal`；`float/double` → `Float/Double`；`boolean/bool` → `Boolean`。
- 字符串：`char/varchar` + `tinytext/text/mediumtext/longtext/json` → `String`。
- 二进制：`binary/varbinary` + blob 族 → `byte[]`。
- 时间：`date` → `LocalDate`；`datetime/timestamp` → `LocalDateTime`；`time` → `LocalTime`。
- 未知 SQL 类型 → `String`（保守）；**原生 SQL-enum 列标量视图固定 `String`**（`TypeMapper.resolveType` L134）；`@enum` 标注列保留自然映射（tinyint→Integer 等，po 等无 enums 产物即得数字/字符串类型）；枚举类视图走产物 `enums` 特性。
- MyBatis jdbcType：`int→INTEGER`、`varchar→VARCHAR`、`text→LONGVARCHAR`、`datetime/timestamp→TIMESTAMP` 等（完整表见 `TypeMapper.JDBC_TYPES`；未知 → `VARCHAR`）。

## 关键代码锚点表

路径均相对仓库根；核心包 `ddl-codegen-core/src/main/java/hyc/codegen/core/`，下表省略该前缀（`…/core/` 后为类路径）。

| 概念 | 类 | 路径 |
|---|---|---|
| 门面（CLI/插件共用入口） | `Codegen` | `…/core/Codegen.java` |
| CLI 入口 / Maven 入口 | `Main` / `GenerateMojo` | `ddl-codegen-cli/src/main/java/hyc/codegen/cli/Main.java`；`ddl-codegen-maven-plugin/src/main/java/hyc/codegen/mavenplugin/GenerateMojo.java` |
| 配置模型 / 加载 | `DdlConfig` / `ArtifactConfig` / `ConfigLoader` / `PropertiesConfigLoader` | `…/core/config/` |
| 类型映射 / 命名 | `TypeMapper` / `NamingService` / `TableNameStrategy` | `…/core/types/TypeMapper.java`；`…/core/naming/` |
| 模型 | `Schema` / `Table` / `Column` / `Index` / `Meta` / `EnumItem` / `EnumCommentParser` | `…/core/model/` |
| DDL 解析 / 操作 / 应用 | `DdlParser` / `DruidDdlParser` / `DruidAst` / `DdlOperation`（10 个 Op）/ `StatementApplier` / `ApplyResult` | `…/core/ddl/` |
| 注解体系 | `AnnotationParser` / `AnnotationProcessor` / `AnnotationRegistry` / `DdlAnnotationHandler` / `TypeHandler` / `AsHandler` / `IgnoreHandler` / `EnumHandler` / `MetaTarget` | `…/core/annotation/` |
| 生成编排 | `CodeGenerator` / `GenerationContext` / `TableContext` / `GeneratorRegistry` | `…/core/gen/` |
| Generator SPI / 内置生成器 | `Generator` / `AbstractJavaGenerator` / `GeneratedSupport` / `JavaTypes` / `QueryMethods` / `QueryMethodFactory` / `PojoGenerator` / `EnumGenerator` / `MapperGenerator` / `MapperXmlGenerator` / `RepositoryGenerator` / `MybatisRepositoryImplGenerator` / `ConverterGenerator` | `…/core/gen/` |
| 写盘 / 报告 / 路径 | `FileWriter` / `ChangeReport` / `ChangeStatus` / `PathResolver` | `…/core/io/` |
| Java AST（tree 模块） | `JavaParser` / `JavaCodegen` / `Class` / `CompileUnit` / `Method` / `Variable` 等 | `ddl-codegen-tree/src/main/java/hyc/codegen/tree/` |
| 依赖方向强制 | `ArchitectureTest` | 各模块 `src/test/java/…/ArchitectureTest.java` |

