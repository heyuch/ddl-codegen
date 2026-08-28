# DDL Codegen

MySQL DDL（create/alter/drop）驱动的 Java 代码生成框架：手写 schema 即唯一事实来源，
`create table` 生成代码、`alter table` 增量更新（只动生成代码、保留手写改动）、`drop table` 删除代码。

技术方案见 `DESIGN.md`，任务进度见 `TASKS.md` 与 `PROGRESS.md`，静态规则考察见 `STATIC-RULES-REVIEW.md`。

## 快速开始

```bash
# 构建（Java 11+）
JAVA_HOME=/path/to/jdk-11 mvn clean package -pl ddl-codegen-cli -am -DskipTests

# 项目根放 ddlgen.properties（项目根 = 该文件所在目录），DDL 放 schema.sql
java -jar ddl-codegen-cli/target/ddl-codegen-cli-1.0-SNAPSHOT.jar \
    --config /path/to/ddlgen.properties --ddl /path/to/schema.sql

# 只报告不写盘
java -jar ... --config ... --ddl ... --dry-run
```

一份 DDL 生成完整 MyBatis 链路：

```
Mapper (接口+XML) → POJO（基础类型）→ RepositoryImpl（桥接）→ Converter → Entity（enum 类型）+ 枚举类
```

config 里配置了哪些 artifact 就生成哪些（`artifacts.*` 段存在即启用）；只配 entity+mapper 就没有 pojo/converter/repository 一整套。

## 运行时依赖

- **druid**：DDL 解析（唯一运行时依赖）
- Java 解析/生成：自研（`ddl-codegen-tree`，基于 jdk.compiler 的可修改 AST，零依赖）

## 边界契约（重要）

1. **以 config + DDL 为准**：文件位置 = config 推导（项目根 + module + package + 类名）。迁移代码不改 config → 视为新建。
2. **不猜数据库真实状态**：DDL 即事实。
3. **删除无条件**：drop 删文件、删成员直接删，不检查引用（编译错误用户处理）。
4. **`@Generated` 成员 = 工具拥有**：reconcile 只动它们；其余（含用户手写成员）内容上永不触碰。
5. 解析失败 → 不动文件并报错（不覆盖用户坏文件）。

## Config（ddlgen.properties）

```properties
# module = 项目根下的一级子目录；留空 = 项目根
artifacts.entity.module=core
artifacts.entity.package=com.myapp.core.entity
artifacts.entity.suffix=
artifacts.entity.use=lombok,jsr303        # 拦截器链

artifacts.enum.module=core
artifacts.enum.package=com.myapp.core.enums

artifacts.pojo.module=core
artifacts.pojo.package=com.myapp.core.pojo
artifacts.pojo.suffix=Po                  # user → UserPo

artifacts.mybatisMapper.module=service
artifacts.mybatisMapper.package=com.myapp.service.mapper
artifacts.mybatisMapper.suffix=Mapper

artifacts.mybatisXml.module=service
artifacts.mybatisXml.path=src/main/resources/mapper   # XML 走资源路径，不用 package

artifacts.repository.module=service
artifacts.repository.package=com.myapp.service.repository
artifacts.repository.suffix=Repository

artifacts.repositoryImpl.module=service
artifacts.repositoryImpl.package=com.myapp.service.repository.impl
artifacts.repositoryImpl.suffix=RepositoryImpl
artifacts.repositoryImpl.di=field         # field=@Resource 字段注入 / constructor

artifacts.converter.module=service
artifacts.converter.package=com.myapp.service.converter
artifacts.converter.suffix=Converter

# 命名
naming.table.stripPrefixes=t_,tmp_        # t_user → User
naming.table.stripShardSuffix=true        # user_0 → User
naming.table.shardPattern=_\d+$
naming.column.camelCase=true              # user_id → userId
naming.column.keywordSuffix=_             # SQL/Java 保留字：order → order_
naming.method.prefix=find                 # 索引 → findByNameAndGender
naming.enum.style=column                  # gender → Gender；或 tableColumn → UserGender

# 自定义注解处理器（实现 DdlAnnotationHandler，须有 public 无参构造）
annotations.custom=com.myapp.MyHandler
annotations.nullable=javax.annotation.Nullable
```

## DDL 注解（列/表/索引注释中，严格 `@name[:value]`）

| 注解 | 位置 | 语义 |
|---|---|---|
| `@type:X` | column | 字段直接使用已有类型 X（entity 视图；不生成、不校验存在） |
| `@as:X` | column | 生成该列对应类（enum）时类名 = X |
| `@as:X` | table | 基类名 = X（所有 artifact 派生） |
| `@ignore` | column | 所有 artifact 跳过该字段（含 XML） |
| `@ignore` | index | 不生成查询方法 |
| 其他 | 任意 | warning 日志并忽略，不中断 |

## 索引 → 查询方法

- 方法名：`findBy` + 各列 camelCase 以 `And` 连接（前缀可配）
- 唯一键全列 → `@Nullable PO`；普通索引/唯一键前缀 → `List<PO>`；多列索引按最左前缀逐级拆分
- `@ignore` 索引不生成

## 增量同步

无 manifest：按 config 定位文件 → 解析现有源码 → 只 reconcile `@Generated` 成员（缺→增、多余→删、类型变→替换、一致→跳过）→ 写盘前字节比对（无变化不写）。同一 DDL 重跑 = 全量 no-op。

## 扩展（三层 SPI）

| SPI | 位置 | 用途 |
|---|---|---|
| `ArtifactGenerator` | `hyc.codegen.core.gen` | 自定义 artifact（继承 `AbstractJavaArtifactGenerator` 只写成员构建） |
| `ArtifactInterceptor` | `hyc.codegen.core.interceptor` | AST 装饰（内置 lombok/jsr303；`artifacts.X.use` 引用） |
| `DdlAnnotationHandler` | `hyc.codegen.core.annotation` | DDL 注解解析 + 类型钩子（`annotations.custom` 注册） |

## 模块

```
ddl-codegen-tree   # 通用 Java 源码解析/生成工具（可修改 AST，基于 jdk.compiler，零依赖）
ddl-codegen-core   # DDL 代码生成框架（模型/解析/命名/类型/生成器/拦截器/编排）
ddl-codegen-cli    # 命令行入口（fat jar）
```
