# opt-core-first：第一性原理简化（核心优先）

## 背景与问题

从第一性原理检视项目：核心价值是 **DDL → Entity + Mapper(接口) + MapperXml**（可查询写入的最小闭环），
加上 **AST + @Generated 成员级增量同步**（alter 更新/drop 删除/保留用户手写代码——原脚本无法实现的能力）。
衍生物（po/repository/repositoryImpl/converter/enum）是可选产物，不同团队用不用、怎么用都不同。

对照核心，发现两层过度设计：
1. **GeneratorInterceptor（use 链/钩子/默认挂载）**——为"每产物加不加 lombok/js303"这种简单开关建了一整套抽象
2. **注解处理框架（processor SPI/onModel）**——与"生成器自由读 meta"重复

## 可选方案与取舍

| 方案 | 结论 |
|---|---|
| 特性开关：A) 拦截器（现状）；B) **生成器内部读配置选项**（entity.lombok=true） | **推荐 B**：和 codegen.groovy 的 getProp 哲学一致；唯一扩展点收敛为生成器 |
| @ignore：A) 各生成器检查；B) **模型级剪枝（解析后移除）** | **推荐 B**：一处解决所有生成器（含 XML），无"脱离框架"顾虑（无注解框架，模型语义即框架行为） |
| @type：A) 并入 enums 视图；B) **独立开关 entity.type=true** | **推荐 B**：与 enums 并列的 PojoGenerator 特性 |
| 开关粒度：A) 布尔（固定注解集）；B) 可配列表 | **推荐 A**（YAGNI；长尾 = 自定义生成器） |

## 方案

### 保留（核心）

- AST + @Generated 成员级 reconcile（增量同步引擎）
- artifact = 生成器实例 + 配置（动态启停；po/entity 共用 PojoGenerator 靠配置区分）
- SchemaModel + meta（DDL 注解解析后全存，不处理）

### 删除

- `GeneratorInterceptor` SPI + use 链 + 钩子（onField/onParam/onModel）
- 4 个拦截器实现（Lombok/Jsr303/Jsr305/Enums）→ 逻辑并入生成器
- 注解处理框架设计（`opt-annotation-interceptors` **作废**）

### 特性开关（config 选项 → 生成器内部应用）

```properties
# PojoGenerator 特性
entity.generator=pojo
entity.lombok=true          # @Data/@Builder/@NoArgsConstructor/...（固定注解集）
entity.jsr303=true          # @NotNull/@Size/@Digits
entity.jsr305=true          # nullable 列 @Nullable
entity.enums=true           # enum 列 → 枚举类（需 enum 产物）
entity.serializable=true    # implements Serializable
entity.type=true            # 字段类型优先用 meta["type"]（@type 注解）

po.generator=pojo
po.lombok=true              # 同生成器，不同特性

enum.generator=enum
mapper.generator=mybatisMapper
mapper.target=po
xml.generator=mybatisXml
```

生成器读取 `ctx.getArtifactConfig().getOption("lombok")` 等，在 `buildClass` 中应用。

### DDL 注解（解析后全存 meta，语义如下）

| 注解 | 处理 |
|---|---|
| `@ignore` | **模型级剪枝**：解析应用后从模型移除列/索引（StatementApplier 或模型层一处） |
| `@type` | PojoGenerator 特性（entity.type=true）：字段类型 = meta["type"] |
| 未知 | 留在 meta，自定义生成器自己读 |

### 扩展点（唯一）

`ArtifactGenerator`：自定义生成器拿全模型 + meta，爱怎么生成就怎么生成；
Java 类产物继承 `AbstractJavaArtifactGenerator` 自动获得 @Generated 增量同步。

## 改动文件与影响面

- 删除：`GeneratorInterceptor.java` + 4 个拦截器 + `use` 链解析（GenerationContext.interceptorsFor/applyInterceptors）
- PojoGenerator：读特性选项并应用（lombok/jsr303/jsr305/enums/type/serializable 逻辑从拦截器迁入）
- @ignore 剪枝：模型层一处（StatementApplier 应用后）
- config：`use=` 移除，改特性布尔；README/插件 IT/测试 config 迁移
- 影响：拦截器扩展点删除（行为收敛到生成器）；`use` 配置键废弃

## 跨生成器依赖原则（config 引用 + 查询契约）

生成器 A 需要产物 X 的类型/字段信息时：**依赖"X 的标识（config 名字）"，解析时查询 X 的生成器实例**
——生成器暴露查询方法 `className/fieldName/fieldType`（默认从 config+naming+model 推导，可覆盖），
TableContext 作为统一查询门面路由到产物生成器。这保留 codegen.groovy 构造器注入的**内聚与准确**
（特殊命名/类型逻辑在生成器内，引用不漂移），同时去掉构造器耦合（config 名字引用、无构造顺序、
可换任意产物、自定义生成器只改 config+可覆盖查询方法）。查询是 (model, config) 的纯函数，无执行依赖。
`mapper.target=po`（核心必需）与 converter 的 `source/target`（衍生物）同机制。
校验：引用一致性（如 converter.source==mapper.target）在生成前校验并给出明确报错。

## 验证

- 全量 `mvn clean test` + 插件 IT 绿（e2e/参数化测试迁移到特性开关）
- 新增断言：@ignore 剪枝后模型无该列/索引；特性开关逐个生效/关闭
- PIT 关注 PojoGenerator 特性应用与剪枝
