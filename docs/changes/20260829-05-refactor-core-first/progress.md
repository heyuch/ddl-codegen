# progress：opt-core-first

## 状态：✅ 完成

- [x] 删除 GeneratorInterceptor + use 链 + 4 个拦截器实现（lombok/jsr303/jsr305/enums 逻辑迁入 PojoGenerator 特性开关）
- [x] 特性开关：产物配置选项（entity.lombok/jsr303/jsr305/enums/type/serializable），生成器内部应用
- [x] 查询契约：ArtifactGenerator 抽象 className/fieldName/fieldType（无 default 逻辑），基类提供命名/TypeMapper 默认，PojoGenerator 覆盖 fieldType（type/enums 视图），MapperXml 抛 UnsupportedOperationException
- [x] @ignore 模型级剪枝（StatementApplier 应用后）
- [x] converter 转换方向改用查询 source/target 生成器 fieldType 判定
- [x] config：use 键移除；测试/IT/README/DESIGN/AGENTS 迁移
- [x] 全量 69 core + 插件 IT 3 绿

## 实现时决策

- 接口查询方法全抽象（无 default 逻辑，避免泄露生成器内部）；视图逻辑在 PojoGenerator.fieldType
- repository/mapper 参数类型走基类 fieldType（TypeMapper，enum 列 String）；impl 移除 enum 参数 .value() 转换（两侧同型直传）
- 转换方向按方法自身 from/to 判定（非配置 source/target）
