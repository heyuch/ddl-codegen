# 变更台账：20260906-07-fix-generator-leftover-fixes

## 状态

**实现完成**（2026-09-06：core 121 绿；提交待落）

## 评审记录

- 2026-09-06 用户要求：02/04 遗留 fix 一并解决（反引号保留字、pk≠id 链路、impl 单值空安全）；缺陷客观、范围由 02/04 记录定义，随执行授权推进，design.md 记录取舍与验收。

## 实现记录

- A 反引号：`NamingService` Java 侧剥反引号（columnFieldName/toCamelCase/toPascalCase 入口 local strip，规避 ParameterAssignment）；`Column.getName()` 保留原文（XML/SQL 侧合法）。恢复 Pojo 保留字 golden（`order_`、无反引号、可编译）；NamingServiceTest 新增反引号用例。
- B pk≠id：`Mapper.deleteById` 参数注解 `"id"` → `ctx.fieldName(id)`；`MapperXmlGenerator` 删除无条件 `findById` select 与循环 `findById` 去重（PRIMARY spec 天然产 `findBy<IdPascal>`）。pk=id 产物不变（xml fixtures 无 diff 实证）。
- C impl 空安全：`bridgeMethod` 单值 convert 桥接改 `call == null ? null : converter.toX(call)`；fixtures（field/constructor-di）与 EndToEnd 断言同步更新（2 文件 4 行 diff 审阅）。
- 验证：core 全量门禁 121 绿（含新 Naming/保留字用例、37 gen 套件回归）。

## 边界（不做，随记录）

- 类级修饰符/成员 javadoc 不 reconcile（升级需删文件重生成，04 已录）；Repository enum 参数化查询未实现（与实现一致的行为）。
