# progress：feat-parameterized-artifacts

## 状态：✅ 完成

- [x] config：顶层键 = 产物名（无 artifacts. 前缀），generator/source/target 属性；naming./annotations. 保留命名空间
- [x] 引用解析：source/target/mapper/converter 显式或"生成器唯一实例"缺省，多实例/无实例明确报错
- [x] enums 视图：实现为 typeOf 按 use 决定（非独立拦截器，避免双机制；设计文档已注明）
- [x] 移除硬编码：TypeMapper.isEnumArtifact、TableContext.typeOf entity-view、poType/entityType
- [x] 生成器注册名：pojo/enum/mybatisMapper/mybatisXml/repository/mybatisRepositoryImpl/converter（repositoryImpl 更名）
- [x] mybatisRepositoryImpl 转换规则：mapper.target == 自己 target → 直连；否则 converter.toX + 一致性校验（converter 仅在转换时解析）
- [x] jsr305 拦截器（nullable 字段 @Nullable）
- [x] 测试：ConfigTest/EndToEndTest/ReconcileLifecycleTest 迁移 + ParameterizedArtifactsTest 5 场景（无 po/自定义 dto/多 converter/一致性校验/直连）+ 插件 IT 迁移
- [x] README/DESIGN.md config 章节同步

## 实现时决策

- converter 方法名 = to + target 类名（toUser/toUserPo）；insert/update 参数名 = target 类名小写
- mybatisRepositoryImpl 的 converter 字段/构造器参数仅在需要转换时生成
- use 含 enums 但未配置 enum 产物 → 明确报错（消息含修复提示）
