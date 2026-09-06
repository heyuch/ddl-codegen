# progress.md — 20260906-13-feat-repository-spring-cache-crud

## 用户评审

- 多轮 co-design + 独立评审（reviewer subagent，冲突清单回写 design.md 对账表）后，用户逐步确认全部决策点；R1（update 含新值 evict）/R2（缓存 NULL 防穿透）/R3（恒去 final）/R4（key `{method}:{args}`、`,` 分隔）均定案。状态 ✅
- fixture 先行审查：用户两轮调整（key 去表名 + `,` 分隔；@Slf4j lombok 形态；默认无 lombok 例命名 `spring-cache`、特例 `spring-cache-with-lombok`；po import；evictCaches 日志常量置顶）

## 实现偏差（步骤 9，对照 design.md）

| # | 偏差 | 原因/处置 |
|---|---|---|
| D1 | `@CacheEvict` 多组注解 → 单个 `@Caching(evict={...})` | **Spring 5.3（Boot 2.7）`@CacheEvict` 无 `@Repeatable`**（独立评审断言「4.3 起可重复」有误，IT 真实 javac 暴露）；`@Caching` 容器注解为全版本通用形态。design.md 示例保持原样属评审稿，实际产物以 fixture/golden 为准 |
| D2 | 非 cache insert/update 透传初版漏 converter（直传 entity） | 实现期 golden 重出暴露（产物将不可编译）；修复为 convert 时 `toUserPo` 透传，constructor-di/field-di fixture 重出 |
| D3 | pkFind 用 `@Nullable String pkFindName` 而非 `@Nullable QueryMethods.Spec` | checker 对嵌套类型 type-use 报错；且仅需方法名，收敛更简 |
| D4 | sample 模块 converter/mapper 提交代码存在**预存漂移**（方法序/`java.util` FQN 等与生成器不一致），随本变更重出收敛 | 非本特性引入；顺带归一（git diff 可审） |
| D5 | `MybatisRepositoryImplGenerator` 新增类级 `@SuppressWarnings("ClassFanOutComplexity")`（原无） | buildClass 组合编排 fanout 高，小方法切分后仍高；带 WHY 注释豁免（与 AbstractJavaGenerator 同类先例） |
| D6 | ReconcileLifecycleTest 增注解 toggle 用例（验收 6）用自定义 `deprecated` 选项 + `TestGenerator` 条件注解 | 达成验收 6：方法注解增删触发替换、幂等、注解删复原 |

## 已核对为真的设计预判（实现确认）

- converter 反向 `to<Source>`（toUserPo）已存在，Route A 写方法无需 converter 生成器改动
- `Variable.init` + STATIC 修饰符可表达 Logger 常量（tree 无需扩展）
- `@Lazy` 自引用 + 字段注入对 @Bean 手工 new 生效（容器后处理器照跑）——sample 装配验证（无 Docker 环境编译级通过）
- 注解比对用 `JavaCodegen.generateCode` 渲染 = 与打印同源，存量一致零 churn（ReconcileLifecycle 全量重跑实证）
- po import 冲突回退 FQN 守卫生效（简单名双 import 防编译崩）

## 关键决策（10a 摘要）

- 特性开关：`repository.springCache=true`（repository 产物；impl 经既有引用同读）；表无索引 → 缓存产物整体失效
- 日志双形态：`repositoryImpl.lombok=true` → 类级 `@Slf4j`；缺省 → `private static final Logger log = LoggerFactory.getLogger(...)` 常量置类顶（首个成员）
- key：`{methodName}:{arg0},{arg1}`；cacheNames 缺省基类名、可 `repository.cacheName` 覆盖；`#f`/`#user.f` 两侧同 spec 循环产出
- 写方法：insert 回填行 evict（rows>0）；update/deleteById fetch-first 老行 evict（update 追加新值 evict = R1）；单列主键才 fetch-first，无主键/复合主键退化 best-effort
- IT：内存缓存（simple）+ `@EnableCaching` + javac `-parameters` + MyBatis 计数拦截器断言命中/失效/防穿透

## 验证记录

- `mvn clean test`（openjdk@11）全 reactor 绿：core 126 / cli 8 / plugin 13 / IT 2（无 Docker 自动 skip，分级内）
- 新 golden：`repository-impl/spring-cache`（默认无 lombok）、`repository-impl/spring-cache-with-lombok`；既有 repository/repository-impl golden 重出并 diff 审阅（CRUD + po import + 去 final）
- ReconcileLifecycleTest 注解 toggle 用例绿

## 残留与后续

- ~~Docker 环境跑 `SampleIntegrationTest.repositorySpringCacheHitsAndEvicts`~~ → **已跑通**（本机 colima：`DOCKER_HOST=unix:///Users/humpy/.colima/default/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`，ryuk 在 colima 下启动失败故禁用以外的工作区；2/2 绿，含缓存命中/失效/防穿透断言与 evictCaches 日志实证）
- README.md 用户手册与 architecture.md 配置参考需在蒸馏同步后（见 architecture 更新条目）

## 收尾增补（20260906-13 后记）

- **evictCaches `@Caching` 多行格式定稿**：用户要求每个 @CacheEvict 独立一行且有缩进；生成器经注解原始文本内嵌换行 + 打印器续行 +4 规则产出 entries 12 格 / 收尾 `})` 4 格（与用户手改 fixture 一致）；两 golden 与 sample 已同步。注：打印器对注解参数原始文本内的换行不加嵌套，缩进由生成器内嵌控制（8 → 输出 12）
