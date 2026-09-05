# generator-leftover-fixes

> 变更号：20260906-07　变更类型：fix（生成物变化，同步 golden fixtures）　目录：`docs/changes/20260906-07-fix-generator-leftover-fixes/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

02/04 记录的三项遗留 fix（2026-09-06 用户要求一并解决）：
1. **保留字反引号未剥**：`` `order` `` 列 → 字段名 `\`order\`` 非法标识符（NamingService Java 侧未去反引号，保留字检测失效）。
2. **主键列名 ≠ `id` 链路**：`Mapper.deleteById` 的 `@Param("id")` 硬编码（XML delete 用 `#{fieldName}` → 绑定分叉）；XML 无条件生成 `select id="findById"`（pk 列名非 id 时成死片段）。
3. **impl 单值 findBy 空安全桥接**：mapper 单值返回 `@Nullable` po，impl 直传 converter 非空形参 → 未命中 NPE（04 已知未修）。

## 方案

- A：`NamingService` 的 Java 侧变换入口去反引号（`columnFieldName`/`indexMethodName`/`enumClassName`/`tableClassName` 内 strip）；`Column.getName()` 保留原文（XML/SQL 侧 `\`order\`` 合法）。→ 字段 `order_`、findByOrder、枚举/表名正常；XML 列引用不变。
- B：`MapperGenerator.deleteByIdMethod` 参数注解值 `"id"` → `ctx.fieldName(id)`（变量名已是 fieldName）；`MapperXmlGenerator` 删除无条件 `findById` select 硬编码与循环内 `"findById"` 去重（PRIMARY 索引 spec 天然产出 `findBy<IdPascal>`，pk 列名 id 时即 `findById`，非 id 时为 `findByUserId`，XML/Mapper/Repo/Impl 命名一致）。
- C：`MybatisRepositoryImplGenerator.bridgeMethod` 在 `bridge.convert && spec.isUniqueFull()` 时 body 改空安全：`return <call> == null ? null : <converter>.<toX>(<call>);`（与 converter nullSafe 风格一致，避免引入中间类型名）。

## 改动文件与影响面

- core main：`naming/NamingService.java`、`gen/MapperGenerator.java`、`gen/MapperXmlGenerator.java`、`gen/MybatisRepositoryImplGenerator.java`
- 测试/fixtures：`NamingServiceTest` 补反引号用例；`PojoGeneratorTest` 恢复保留字 golden（input+expected，字段 `order_`、可编译标识符）；受影响的既有 fixtures 重出（xml full/non-auto-pk、repository-impl 两形态——见验证 diff 审阅）；全量门禁回归。
- 不做（边界，另记）：类级修饰符/成员 javadoc 不 reconcile（升级需删文件重生成，04 已录）；Repository enum 参数化查询未实现（行为与实现一致）。

## 验收标准

1. `` `order` `` 列 → pojo 字段 `order_`（合法标识符），XML 列引用仍 `` `order` ``；NamingServiceTest 新用例绿。
2. pk 列名 `uid` → Mapper `deleteById(@Param("uid") …)`、XML `#{uid}`、无死 `findById` select、Mapper/XML/Repo 均有 `findByUid` 一致命名（新增 golden 验证）。
3. impl 单值 findBy 桥接含 `== null ? null :` 守卫（fixtures 更新）；全 reactor `mvn clean test` 绿。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`；golden diff 审阅；`new-change.sh check`
