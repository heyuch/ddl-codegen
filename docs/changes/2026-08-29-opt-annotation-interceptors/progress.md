# progress：opt-annotation-interceptors

## 状态：❌ 作废

被 `opt-core-first`（第一性原理简化）取代：不再建注解处理框架/拦截器钩子，
改为"生成器读配置特性开关 + 注解全存 meta + @ignore 模型级剪枝"。
保留到本变更的决策：注解解析"全存不处理"、@as 移除、@type 独立于 enums。
