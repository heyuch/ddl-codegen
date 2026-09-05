# progress：feat-maven-plugin

## 状态：✅ 完成

- [x] core `Codegen` 门面（CLI/插件共用单一管线）；CLI Main 重构为薄壳
- [x] CLI --config 缺省 = cwd/ddl-codegen.properties（对齐插件默认）
- [x] `ddl-codegen-maven-plugin` 模块：GenerateMojo（projectRoot/configFile/ddl/ddlFile:范围/dryRun/skip，不绑生命周期）
- [x] 单测 12 例（范围解析边界 5 + Mojo 参数/互斥/skip/执行 7）
- [x] 集成测试 3 个（maven-invoker：it-simple / it-range / it-inline，真实 mvn 执行 + verify.groovy 断言）
- [x] 门面防绕过 ArchUnit 规则（cli + plugin 不得直连 gen/tree）——补齐 archunit-rules 变更的最后一环
- [x] README：config 默认名 ddl-codegen.properties + Maven 插件用法

## 决策补充（实现时拍板）

- ddlFile 相对路径按 projectRoot 解析（Maven 惯例：相对路径 → basedir）
- 行范围越界钳制到文件末尾 + warning（与设计一致）；"file:66"（无 - 段）按整文件处理，文件不存在错误会带完整路径
