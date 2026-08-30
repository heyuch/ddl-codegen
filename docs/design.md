# DDL Codegen 技术方案

> 本文件是 DDL→Java 代码生成框架的完整技术设计，供实现 session 直接执行。
> 相关背景：所有决策来自 2026-08 的设计讨论，已定稿，勿再推翻（除非发现硬伤）。
> 配套任务列表见 `docs/tasks.md`。

## 1. 定位与边界

工具：MySQL DDL（create/alter/drop）→ Java 代码生成框架（MyBatis 栈）。
目标生成链路：`MyBatis Mapper → POJO（基础类型）→ RepositoryImpl → Converter → Entity（enum 类型）`，
各环节均为独立 artifact，可配置启用（例如只配 entity+mapper 就不要 pojo/converter/repository）。

### 边界契约（必须遵守，违反即越界）

1. **以 config + DDL 为准**：文件位置 = config 推导（项目根 + module + package + 类名）。用户迁移代码不改 config → 视为新建，不做扫描兜底、不猜。
2. **不对数据库真实状态做假设**：DDL 即事实，用户提供什么处理什么。
3. **删除无条件**：drop table 删 config 路径下该表的生成文件；删成员直接删，**不检查引用**，编译错误用户自己处理。
4. **成员所有权**：带 `@Generated` 注解的成员 = 工具拥有，可增删改；其余 = 用户代码，内容上永不触碰（格式可能被归一化，语义不变，可接受）。
5. **解析失败 → 不动文件 + 报告错误 + `--force` 逃生口**。
6. **无 manifest、无状态文件、无文件头标记**：增量靠"解析现有文件 + 对比"。
7. **尽量少报错**：未知注解等 → 记 warning 日志 + 忽略继续，不中断。

## 2. 技术选型与依赖审计

| 用途 | 方案 | 理由 |
|---|---|---|
| DDL 解析 | `com.alibaba:druid`（1.2.x） | MySQL 方言最成熟（ALTER/DROP/注释/版本化注释）。Parser SPI 可替换 |
| Java 解析/生成 | **自研可修改 AST**（基于 jdk.compiler，模块 `ddl-codegen-tree`） | 弃 roaster：JDT shade 进包 ~10MB、维护模式、且 roaster 自身方法体也是文本哲学。自研实现 `com.sun.source.tree` 公共 API，零依赖 |
| config | JDK `Properties` | 零依赖。ConfigLoader SPI 供 YAML/JSON 扩展 |
| 日志 | `System.Logger`（JDK 9+） | 零依赖 |
| CLI | 手写参数解析（~40 行） | 就 4 个 flag |
| 框架自身代码 | 不用 lombok/slf4j/guava | 生成的项目代码可以用 lombok——那是拦截器的职责 |
| 测试 | JUnit 5（AGENTS.md 已定）+ golden-file | — |

**运行时依赖：仅 druid 一个。** lombok/checker-qual 均为 provided（构建期）。

## 3. 模块划分（多模块）

```
ddl-codegen (parent, packaging=pom)
├── ddl-codegen-tree   # 通用 Java 源码解析/生成工具（自研 AST，从 codegen-groovy 提取改造）
├── ddl-codegen-core   # DDL 代码生成框架（SchemaModel / 编排 / SPI / 内置生成器）
└── ddl-codegen-cli    # 命令行入口
```

依赖方向：`cli → core → tree`。tree 不依赖 core（纯通用工具）。
现有 pom 是单模块，需转为多模块 parent（插件配置迁到 pluginManagement）。

## 4. 总体管线

```
DDL 文件/目录 → Druid 解析语句 → 顺序应用到内存 SchemaModel（后续语句可见前面结果）
  → 每张受影响表 × config 启用的每个 artifact kind：
      路径 = 项目根 + module + package 路径 + 类名（config 推导，XML 走 path）
      文件不存在 → generator.apply(model, null) 从零构建
      文件存在   → tree lib 解析 → generator.apply(model, existing) 只 reconcile @Generated 成员
      → 拦截器链（artifacts.X.use）→ 字节比对 → 写盘 / 跳过（无变化不写）
  DROP TABLE → 删 config 路径下该表生成文件（无条件）
  RENAME     → 老文件改类名、移到新路径（唯一需要语句级处理的场景）
  --sync 模式 → 对账磁盘生成物与当前输入（输入中消失的表 → 删文件）
  报告：逐文件 +n/-m/~k 变更摘要 + 警告
```

**reconcile 即 diff**：不需要独立 diff 引擎。alter 的增删改由"模型 vs 解析出的现有文件"对比得出：
模型有而文件无 → 增；有而模型无 → 删；类型/签名变 → 替换；一致 → 跳过。
幂等：相同输入重跑 = 全量 no-op（字节比对保证）。

## 5. SchemaModel

- `Table` / `Column` / `Index`，纯模型对象
- 每个节点带 `meta`：开放 map，注解解析结果存这里，任何生成器/拦截器可读写
- 注释原文也保留（注解从注释提取，注释本身仍是注释）

## 6. Config 格式（JDK Properties，位于项目根）

**产物与生成器解耦**（见 docs/changes/2026-08-29-feat-parameterized-artifacts/design.md）：
顶层键第一段 = 产物名（自由定义），`generator` 引用注册的生成器；`naming.*` 与 `annotations.*` 为保留命名空间。

```properties
entity.generator=pojo
entity.package=com.myapp.core.entity
entity.suffix=
entity.lombok=true                  # 特性开关（生成器内部应用）
entity.jsr303=true
entity.enums=true                   # enum 列 → 枚举类（需 enum 产物）

enum.generator=enum
enum.package=com.myapp.core.enums

po.generator=pojo
po.package=com.myapp.core.pojo
po.suffix=Po

mapper.generator=mybatisMapper
mapper.package=com.myapp.service.mapper
mapper.suffix=Mapper
mapper.target=po                     # 返回类型产物；无 po 时 target=entity

xml.generator=mybatisXml
xml.path=src/main/resources/mapper
xml.target=po

repository.generator=repository
repository.package=com.myapp.service.repository
repository.suffix=Repository
repository.target=entity

repositoryImpl.generator=mybatisRepositoryImpl
repositoryImpl.package=com.myapp.service.repository.impl
repositoryImpl.suffix=RepositoryImpl
repositoryImpl.target=entity
repositoryImpl.mapper=mapper
repositoryImpl.converter=entityConverter

entityConverter.generator=converter
entityConverter.package=com.myapp.service.converter
entityConverter.source=po
entityConverter.target=entity

naming.table.stripPrefixes=t_,tmp_
naming.table.stripShardSuffix=true
naming.table.shardPattern=_\d+$
naming.column.camelCase=true
naming.column.keywordSuffix=_
naming.method.prefix=find
naming.enum.style=column

annotations.custom=com.myapp.MyHandler
```

- **产物存在即启用**：配置了哪些产物就生成哪些；引用（source/target/mapper/converter）缺省 = 该生成器唯一实例，多实例/无实例必须显式
- PO 推导规则已废弃：mapper 返回类型由 `target` 显式/缺省决定

## 7. 命名策略

表名→类名变换链（顺序执行，均可配置）：

1. 剥前缀：`naming.table.stripPrefixes`（`t_user` → `user`）
2. 剥分表后缀：`naming.table.stripShardSuffix` + `shardPattern`（`user_0` → `user`）
3. snake_case → PascalCase（`user_profile` → `UserProfile`）
4. 按 artifact 拼后缀（`User` + `Mapper` → `UserMapper`）

列名：camelCase；Java 关键字 → 加 `naming.column.keywordSuffix`。
索引→方法名：`findBy` + 各列 camelCase 以 `And` 连接（前缀可配，接口与 XML id 必须一致）。
enum 类命名：列名转类名（`Gender`）或 表+列（`UserGender`），`@as` 可覆盖。
长尾场景（`sys_` 保留、日期分表转枚举等）：`TableNameStrategy` SPI 逃生口。

## 8. 类型映射（按 artifact 解析）

解析顺序：**`@type` 注解 > enum 列 → Enum 类（entity）/ String（pojo）> SQL→Java 基础类型**。

- POJO 固定映射，不进 config：enum→String、decimal→BigDecimal、datetime→LocalDateTime、
  tinyint(1)→Boolean、json→String、text/blob→String/byte[]、unsigned bigint→Long
- `@type` 只影响 entity（用户明确："用于 Entity 类属性"）
- jdbcType 映射表：SQL 类型 → `java.sql.Types` 名（VARCHAR/BIGINT/TINYINT/DECIMAL/TIMESTAMP…）

## 9. 注解体系（DDL comment 中的元数据）

### 语法

- 严格格式：`@name[:value]`（value 可省略，如 `@ignore`）
- **无隐式简写**：`@boolean` 不代表 `@type:boolean`，须写全
- 已知集：`{type, as, ignore}`；未注册的未知名字 / 无法解析 → **记 warning + 忽略，不中断**
- 作用于 table/column/index comment；目标位置不符 → 同样 warning + 忽略

### 内置注解

| 注解 | 位置 | 语义 |
|---|---|---|
| `@type:X` | column | 字段直接使用已有类型 X（FQN 或简单名）。**不生成、不校验存在**（复用共享枚举类；找不到编译器会报错，那是用户的事） |
| `@as:X` | column | 生成该类时类名 = X（如 enum 列不想叫 `Gender`，指定 `@as:UserGender`） |
| `@as:X` | table | 基类名 = X（所有 artifact 派生） |
| `@ignore` | column | 所有 artifact 跳过该字段（含 XML resultMap/insert/update 片段） |
| `@ignore` | index | 不生成查询方法（XML 也不生成对应 select） |

### 扩展 SPI：DdlAnnotationHandler

```java
public interface DdlAnnotationHandler {
    String name();                          // "type" / "as" / "ignore" / 自定义名
    Set<MetaTarget> targets();              // TABLE | COLUMN | INDEX
    void parse(Meta meta, String value);    // 校验 + 结构化存储进 meta
    default JavaType resolveType(Column column, JavaType defaultType) {
        return defaultType;                 // 类型解析钩子，@type 挂这里
    }
}
```

- **注解处理在模型层**（所有生成器看到同一份装饰后模型，@ignore 必须影响 XML），**AST 装饰在拦截器层**（lombok/js303）。两层不合并，可组合（自定义注解 = handler + 可选 interceptor）
- meta 是开放 map：任何生成器/拦截器可读写
- 注册：config `annotations.custom`

## 10. 生成器体系

```java
public interface Generator {
    /** existing == null 表示文件不存在，从零构建；否则只动 @Generated 成员 */
    JavaSource<?> apply(TableContext table, JavaSource<?> existing, GenerationContext ctx);
}
```

- **`AbstractJavaArtifactGenerator` 基类**：封装 reconcile 循环（找 `@Generated` 成员 vs 模型期望：
  缺→增、签名/类型不符→替换、多余→删），用户生成器只写"成员构建器"
- **两阶段**：阶段 A 生成器发布 typed 描述（仅依赖模型+config），阶段 B 消费描述产出文件 →
  跨 artifact 依赖天然无环。95% 由命名推导覆盖（类名 = table + naming + config，各生成器独立算出同一结果）；
  `ArtifactRegistry`（发布/查找元数据）仅作用户扩展兜底，不做依赖图
- **生成侧 = 字符串方法体 + Expr/Block 助手函数**（不用表达式 AST，见 §14）
- 内置生成器（7 个）：entity / enum / pojo / mybatisMapper / mybatisXml / repository / repositoryImpl / converter

### 内置生成器要点

- **Entity**：enum 列 → Enum 类；成员全带 `@Generated`；支持 @type/@as/@ignore；字段按 DDL 列序
- **Enum**：列 enum 值 → 常量；生成 `fromValue(String)`（switch 实现，处理非标识符值如 `in-progress`）；@as 命名
- **Pojo**：固定基础类型映射
- **Mapper 接口**：findBy* 方法 + @Nullable/List 返回 + @Param
- **MapperXml**：见 §12
- **Repository 接口**：方法来自索引，返回 Entity（enum 参数）
- **RepositoryImpl**：桥接 `converter.toEntity(mapper.findXxx(...))`；enum 参数转换
  `gender == null ? null : gender.name()`；依赖注入按 config
- **Converter（plain 默认）**：逐字段赋值：
  ```java
  User toEntity(UserPo po) {
      User u = new User();
      u.setId(po.getId());
      u.setGender(po.getGender() == null ? null : Gender.fromValue(po.getGender()));
      return u;
  }
  ```
  方法体 = 平铺语句字符串（`String.join("\n", stmts)` 交给 SourceBlock 对齐缩进），
  类型驱动的转换逻辑收进小助手函数 `conversionExpr(Column, valueExpr)`。
  **方法体字符串里引用的类型（如 `Gender`）不会被 import 收集器扫到——必须通过助手函数登记 import**

## 11. 拦截器体系

```java
public interface GeneratorInterceptor {
    String name();
    void apply(JavaSource<?> source, GenerationContext ctx);
}
```

- 契约：只允许动 `@Generated` 成员和 import；幂等（先移除自己管理的注解再按模型重算）；
  字节比对兜底（内容没变不写盘）
- 内置：
  - `lombok`：类级 `@Data`（可配 @Builder/@NoArgsConstructor 等）+ import
  - `jsr303`：只映射 DDL 里真实存在的约束，不猜语义：`NOT NULL`→`@NotNull`、
    `varchar(n)`→`@Size(max=n)`、`decimal(p,s)`→`@Digits(integer=p-s, fraction=s)`；
    不做 `@Email`/`@Past` 这类推断
- 配置：`artifacts.X.use=lombok,jsr303`；拦截器按名字从注册表解析，顺序执行
- 复用：`ctx.applyInterceptors(source)` / `ctx.interceptor(name)`，用户生成器一行调用

## 12. 索引 → 查询方法算法

- 方法名：`findBy` + 各列 camelCase 以 `And` 连接（`naming.method.prefix` 可配）
- 返回类型：
  - PRIMARY / UNIQUE 且参数 = 索引全部列 → `@Nullable PO`
  - 其余（普通索引、唯一索引的前缀）→ `List<PO>`
- **最左前缀拆分**：每索引生成 n 个方法（1..n 列）。例：
  `UNIQUE KEY uk_name_gender(name, gender)` →
  `@Nullable UserPo findByNameAndGender(@Param("name") String name, @Param("gender") String gender)`
  + `List<UserPo> findByName(@Param("name") String name)`
  `INDEX idx_status_type(status, type)` → findByStatusAndType + findByStatus（均 List）
- 每个参数带 `@Param`；XML 参数占位 `#{name,jdbcType=VARCHAR}`
- 索引注释含 `@ignore` → 不生成任何方法
- `@Nullable` 注解包：config 可配（默认 `org.checkerframework.checker.nullness.qual.Nullable`）

## 13. MapperXml 模板规格

生成方式：**字符串/模板**（非 AST），整文件重生成（用户手改 XML 场景少，不提供成员级合并）。

- **resultMap** `id=BaseResultMap` `type=PO 全限定名`：
  `<id property="id" column="id" jdbcType="BIGINT"/>`，其余 `<result property=字段名 column=列名 jdbcType=.../>`
  （property = camelCase 字段，column = SQL 列名）
- **BaseColumnList**：`t.id, t.name, ...`（`t.` 别名，逗号分隔），select 用 `<include refid="BaseColumnList"/>`
- **insert**：不含自增主键列；`useGeneratedKeys="true" keyColumn keyProperty`；
  `INSERT INTO t_user (列...) VALUES (#{字段,jdbcType=...}, ...)`
- **delete**：`DELETE FROM t_user WHERE id = #{id,jdbcType=BIGINT}`
- **update**：不含主键列；`SET 列 = #{字段,jdbcType=...}, ...` + `WHERE id = ...`
- **selectById** + 索引派生的 select：`SELECT <include BaseColumnList/> FROM t_user t WHERE 等值条件 AND 连接`
- **XML 方法 id 与接口方法名必须一致**（MyBatis 绑定要求，否则启动报错）
- WHERE 条件用列序等值 AND；列名与别名的使用保持一致

## 14. ddl-codegen-tree：自研 Java 源码工具改造计划（M0，前置地基）

### 现状（源：`/Users/humpy/code/java/codegen-groovy/codegen-core/src/main/java/hyc/codegen/core/tree`，~3874 行）

已实现且经验证：
- jdk.compiler 解析（`JavacTask.parse` + `DocTrees`）→ 可修改模型（实现 `com.sun.source.tree` 公共接口）
- 结构级模型：CompileUnit / Class / Method / Variable / Annotation / TypeReference / ParameterizedType /
  PrimitiveType / Modifiers / Import / Package / Literal + javadoc 双向转换（JavadocTreeConverter/JavadocCodegen）
- `JavaCodegen`（TreeScanner）打印 + import 管理（去重、java.lang/同包过滤、分组排序）
- `SourceBlock`/`SourceExpr`：方法体/表达式原文（含缩进归一化逻辑）
- 生成侧已在 codegen-msxf-member 实战（1785 行生成器 + CommentAnnotationResolver）
- Demo.java round-trip 0 diff

已知缺口（冒烟测试实证）：
- **member select 表达式静默丢失**（`System.lineSeparator()` 打印成 `System`）
- **varargs 丢失**（`Object... s` → `Object s`）
- lambda 改写（`d ->` → `(d)->`）、局部变量声明丢分号、块内语句缩进塌缩
- `Method.getTypeParameters()` 返回 `List.of()`（getter 与字段不一致）

### 改造任务（对应 docs/tasks.md M0）

1. **提取**：独立模块 `ddl-codegen-tree`；包名 `hyc.codegen.tree`；连同 `utils/CodePrinter`、`utils/U` 一起迁移，移除对 codegen-groovy 其余代码的依赖；现有测试（JavaParserTest/JavaCodegenTest/JavadocCodegenTest/Demo + 冒烟 RoundTripSmokeTest）迁移
2. **保真层四修**（正确性红线，round-trip 内容无损）：
   a. **表达式全局兜底**：任何未显式处理的节点 → `print(node.toString())`（javac 的 toString 忠实于源码）——一把修掉 member select/方法调用/二元运算/lambda 等
   b. **visitBlock 语句打印**：局部变量按语句语义打印（补分号）；toString 语句做缩进归一化（复用 SourceBlock 的缩进对齐逻辑）
   c. **ArrayTypeTree + varargs**（`Object...`）
   d. 小 bug：`Method.getTypeParameters()` 等 getter/字段不一致
3. **生成助手**：`Expr`/`Block` 助手函数集（call/member/ternary/nullSafe/if/for，返回字符串）+
   **import 登记**（方法体字符串引用的类型由助手登记进 import 收集器）
4. **质量优化**（用户要求按开源项目标准，不达标的旧代码直接优化）：
   - 私有字段 + getter/setter；实现 `com.sun.source.tree` 接口的 getter 保留（契约）
   - builder 只保留 Class/Method/Variable 三个高频 builder，API 签名统一（type/name/annotation/modifiers 四件套一致），其余去散装 builder
   - 防御性拷贝统一（getter 返回集合统一 `new ArrayList<>(field)`）
   - 公共 API 全 javadoc
   - 命名审查：`U`/`Types` 等无意义类名改名；类组织审查（模型/转换/打印分层清晰）
   - 拆分 JavaCodegen 的 import 管理为独立类（ImportManager），职责单一
   - 移除死代码/重复逻辑
5. **测试**：round-trip golden 测试集（覆盖 varargs/lambda/三元/内部类/泛型/注解参数/throws 的复杂文件）；
   生成侧单元测试迁移转绿

### 关键决策

- **不做全量表达式 AST**（YAGNI）：解析侧用 toString 兜底保真（内容无损），生成侧用字符串 + 助手
  （与 roaster `setBody(String)` 哲学一致）。何时需要表达式 AST：程序化改写用户表达式 / 表达式级 diff /
  复杂控制流生成——目前都没有。visitor 分发天然增量，将来可随时补单个节点
- javac 解析要求 JDK 运行时（开发工具，OK）；"编辑一半的坏文件" → parse 报错 → 走报告 + `--force` 契约

## 15. 构建与代码风格

- 多模块 pom：现有单模块 pom 的插件配置迁到 parent 的 pluginManagement
- Java 11 目标（AGENTS.md）；本机 JDK 23 可编译（source/target 11 兼容）
- spotless（apply 绑 validate）：eclipse 格式 + import order + sortPom
- checkstyle（绑 validate）；error-prone + checkerframework NullnessChecker + lombok（pom 已配）
- `maven-settings.xml` 是 Docker/阿里云镜像配置，本地构建默认 settings 即可
- 提交前必须 `mvn validate` 绿

## 16. 风险与取舍（已接受）

| 取舍 | 说明 |
|---|---|
| 方法体/复杂表达式 round-trip 靠 toString 兜底 | 语义保真，格式归一化（与 roaster 相同）；golden 测试锁死 |
| 用户在 `@Generated` 成员上的手写改动被覆盖 | 契约明示，文档写明 |
| 删成员不检查引用 → 可能编译错误 | 用户处理，契约明示 |
| tree lib 是自有代码 | 完全可控，维护责任自担 |
| roaster/JDT 弃用 | 少 10MB 依赖 + 一个维护模式的库 |

## 17. 开放问题（实现前拍板，或按默认走）

- `@Nullable` 注解包默认值（javax/jakarta/spring）→ 默认 `org.checkerframework.checker.nullness.qual.Nullable`，config 可配
- 内置生成器默认启用集 → 默认全配（entity/enum/pojo/mapper/xml/repository/impl/converter）
- mapper 直连 entity（无 pojo）时的类型映射细节 → 按 §8 规则走，映射到 entity
