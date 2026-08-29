# parameterized-artifacts：产物与生成器解耦（artifact = 生成器具名实例 + 配置）

## 背景与问题

当前 config 的 artifact 名 == 生成器名（`artifacts.entity` 隐含用 entity 生成器），且存在多处硬编码：
`TypeMapper.isEnumArtifact`（enum 视图按 artifact 名判断）、`GenerationContext.poType/entityType`
（硬编码查 `artifacts.pojo`/`artifacts.entity`）。用户需要自定义产物（dto/vo/query）、一个生成器多实例
（多个 converter）、无 po 时 mapper 直连 entity——当前设计都要改框架代码。

目标：**artifact = 生成器的具名实例 + 实例配置**。`artifacts.<名字>.generator=<生成器名>`，
生成器通过实例配置参数化（`use` 拦截器链、`source`/`target` 产物引用）。

## 可选方案与取舍

| 方案 | 结论 |
|---|---|
| A) 保持 kind==generator，需要新产物就加硬编码 | 否决：违背框架可扩展初衷 |
| B) **artifact 名与生成器名分离，配置引用驱动**（本次） | **推荐**：自定义产物零成本、多实例、顺带消掉 isEnumArtifact 硬编码 |
| enum 类生成位置：A) 独立 enum 产物；B) 并入 field 生成器选项 | **推荐 A**（enum 类只生成一次，全项目共享；字段类型改写由 enums 拦截器做） |
| 跨产物引用：A) 显式 source/target + 缺省规则；B) 全靠显式 | **推荐 A**：`target`/`source` 显式配置；缺省 = 该生成器唯一实例（多实例/无实例时须显式） |

## 方案

### Config schema（Properties，格式不变）

```properties
# 产物名自由定义；generator 引用注册的生成器
artifacts.entity.generator=pojo
artifacts.entity.module=
artifacts.entity.package=com.demo.entity
artifacts.entity.suffix=
artifacts.entity.use=lombok,jsr303,jsr305,enums   # 拦截器链（lombok 类级，jsr303/jsr305/enums 字段级）

artifacts.po.generator=pojo
artifacts.po.package=com.demo.pojo
artifacts.po.suffix=Po
artifacts.po.use=lombok

artifacts.dto.generator=pojo
artifacts.dto.package=com.demo.dto
artifacts.dto.suffix=Dto

artifacts.enum.generator=enum
artifacts.enum.package=com.demo.enums

artifacts.mapper.generator=mybatisMapper
artifacts.mapper.package=com.demo.mapper
artifacts.mapper.suffix=Mapper
artifacts.mapper.target=po                    # 返回类型产物（无 po 时配 target=entity）

artifacts.xml.generator=mybatisXml
artifacts.xml.path=src/main/resources/mapper
artifacts.xml.target=po

artifacts.repository.generator=repository
artifacts.repository.package=com.demo.repository
artifacts.repository.suffix=Repository
artifacts.repository.target=entity

artifacts.repositoryImpl.generator=repositoryImpl
artifacts.repositoryImpl.package=com.demo.repository.impl
artifacts.repositoryImpl.suffix=RepositoryImpl
artifacts.repositoryImpl.target=entity
artifacts.repositoryImpl.mapper=mapper          # 引用：缺省 = 该生成器唯一实例
artifacts.repositoryImpl.converter=entityConverter

artifacts.entityConverter.generator=converter
artifacts.entityConverter.package=com.demo.converter
artifacts.entityConverter.suffix=Converter
artifacts.entityConverter.source=po
artifacts.entityConverter.target=entity

artifacts.dtoConverter.generator=converter
artifacts.dtoConverter.package=com.demo.converter
artifacts.dtoConverter.suffix=DtoConverter
artifacts.dtoConverter.source=entity
artifacts.dtoConverter.target=dto
```

### 引用解析规则

- `source`/`target`/`mapper`/`converter` 等引用键 = 产物名；解析为 FQN（该产物 pkg + naming.artifactClassName(table, 产物名)）
- **缺省规则**：`target` 缺省 = `pojo` 生成器的唯一实例；`mapper`/`converter` 引用缺省 = 对应生成器唯一实例；
  无唯一实例/无实例 → 必须显式配置，否则报错（信息含产物名）
- `enums` 拦截器对 enum 产物的引用同样走缺省规则（唯一 `enum` 实例）

### 生成器注册名与参数

| 生成器 | 实例参数 | 说明 |
|---|---|---|
| `pojo` | use（拦截器） | 字段类产物；enum 列默认 String，`enums` 拦截器改写为枚举类 |
| `enum` | — | 枚举类产物（一次生成，全项目共享） |
| `mybatisMapper` | `target` | 接口；参数用 POJO 视图（TypeMapper 已统一 enum→String，无需视图逻辑） |
| `mybatisXml` | `target` + mapper 引用 | XML 模板 |
| `repository` | `target` | 接口；enum 参数用枚举类（由 enums 语义决定——repository 的 use 里配 enums） |
| `repositoryImpl` | `target`/`mapper`/`converter` | 桥接 |
| `converter` | `source`/`target` | 逐字段映射 |

注意：repository/repositoryImpl 的 enum 参数视图由**该产物自身的 `use: enums`** 决定（不再按产物名魔法）。

### 新增拦截器

| 拦截器 | 级别 | 行为 |
|---|---|---|
| `enums`（新） | 字段级 | `@Generated` 字段的 enum 列：类型 String → 枚举类 FQN（enum 产物 pkg + naming.enumClassName），幂等 |
| `jsr305`（新） | 字段级 | nullable 列 → `@javax.annotation.Nullable`（与 jsr303 的 @NotNull 互补；语义假设，review 确认） |
| `lombok`/`jsr303` | 类级/字段级 | 现有，不变 |

### 移除的硬编码

- `TypeMapper.isEnumArtifact` 与 `TableContext.typeOf` 的 entity-view 分支 → 删除（enum 视图交给 enums 拦截器）
- `GenerationContext.poType/entityType` → 由引用解析替代
- CodeGenerator 的 kind→生成器查找 → artifact 名→config.generator 解析

## 改动文件与影响面

- config：ArtifactConfig 增 `generator`/`source`/`target`；PropertiesConfigLoader 解析
- 框架：CodeGenerator（按 generator 名解析）、TableContext（typeOf 去 hack、引用解析 helper）、GenerationContext（引用解析）、Codegen 门面（默认配置显式化）
- 生成器：全部改为从实例配置读参数（target/source/mapper/converter）；FieldArtifactGenerator 更名注册 `pojo`
- 新增：`enums`/`jsr305` 两个拦截器
- 测试：EndToEndTest/CLI 冒烟/插件 IT 的 config 全部迁移到新 schema；新增引用解析/拦截器测试
- 影响：**breaking config 变更**（项目未发布，单轨迁移）；CLI/插件 facade 默认生成器按新注册名构建

## 验证

- 全量 `mvn clean test` + `mvn validate` 绿；插件集成测试（it-simple/range/inline）在新 schema 下通过
- 新增场景验证：无 po（mapper.target=entity）、多 converter（entityConverter/dtoConverter）、自定义产物（dto）
- ArchUnit 规则不变（包结构不破坏）；PIT 关注引用解析与 enums 拦截器
