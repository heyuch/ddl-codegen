# 开发进度台账

> 自动执行记录（用户睡眠期间）。每阶段：worker 交付 → 复跑 validate+test → review diff → 记录。

## 阶段状态

| 阶段 | 内容 | 状态 | 验证 |
|---|---|---|---|
| M0.1 | 模块化 + tree 迁移 + 质量修正 | ✅ 收口 | validate+test 全绿（14/14） |
| M0b | 保真层四修 + round-trip 断言测试 | ✅ 收口 | validate+test 全绿（16/16）；Demo 字节全等，复杂文件语义等价+幂等 |
| M0.7 余项 | CodePrinter 重写 / U、Types 命名 / 死代码 / Expr·Block 助手 | 🔄 进行中 | |
| M1 | core：模型/Druid/语句/注解/config/命名/类型/IO | ⬜ | |
| M2 | 生成核心：SPI/基类/拦截器/两阶段 + **Expr·Block 助手集成** | ⬜ | |
| M3 | 内置生成器 ×7 + golden | ⬜ | |
| M4 | CLI/报告/--sync/文档/端到端验收 | ⬜ | |

## 关键决策记录（执行中拍板）

- ClassFanOutComplexity：阈值 20 保留；分发器类针对性 @SuppressWarnings + 实证注释（见 STATIC-RULES-REVIEW.md §5/§6）
- **Expr/Block 助手简化**（M0c 并入 M2）：助手为纯字符串组合，不做 import 魔法；方法体引用类型的 import 由生成器显式 addImport（避免状态化 import-sink API，更清晰）。M0c 拆入 M2（对着真实生成上下文构建，避免空想 API）。
- 其余开放问题按 DESIGN.md §17 默认值

## 阶段详情

（每阶段完成后补：worker 摘要 / 我的复核结论 / 异常与处理）
