# Repository 写方法 + Spring Cache（cache-aside）生成

> 变更号：20260906-13　变更类型：feat　目录：`docs/changes/20260906-13-feat-repository-spring-cache-crud/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

现状 repository 层只有索引派生读方法（`RepositoryGenerator`/`MybatisRepositoryImplGenerator`），写路径在 Mapper（`insert/update/deleteById`），repositoryImpl 无法承担 cache-aside 职责：缓存点（读）与写点（mapper 直写）不相交，缓存永不失效。需求（多轮用户决策）：

1. repository 补齐数据更新类方法（insert/update/deleteById 透传），读写在 repository 层收敛；
2. 新增开关控制 repository 层生成 Spring Cache 注解（cache-aside）：查询方法 @Cacheable、数据变更后清理缓存；
3. 清理策略：keyed 精确失效，统一收口到 `evictCaches(Entity)` 一个方法（接口 + 实现各一份，用户可手动调用）；deleteById/update 先按 id 查老数据、再写、再用**老数据**清缓存；insert 不特意清 id key（新行无 id 缓存）；
4. 缓存 key 格式——**后经用户多轮调整定稿**：既有 `cacheName` 已按表/实体隔离缓存命名空间，key 不再拼表名；最终格式 `{methodName}:{arg0},{arg1},…`——方法名后 `:`、参数间 `,`（如 `findById:5`、`findByNameAndKind:tom,MALE`）；简单字符串拼接、无复杂 SpEL（用户否决 asList）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 缓存注解放 repository 接口方法（JDK 代理） | 无需去 final | Spring 官方不推荐：接口注解在 CGLIB（Boot 默认 proxy 风格之一）下被静默忽略（SPR-14343/15271）；注解进接口 = 缓存策略进服务契约 | 否决 |
| 缓存注解放 impl 方法 | JDK/CGLIB 双代理都认（注解源按最具体方法解析到实现） | impl 是代理目标，需类可代理 | 推荐 |
| `evictCaches` 内写方法 `this.evictCaches(e)` 直调 | 无 | 自调用不过代理 → 注解永不触发（静默失效） | 否决 |
| 自引用字段 + `spring.main.allow-circular-references=true` | 无 | Boot 2.6+ 默认禁循环依赖；全局放开是官方「last resort」 | 否决 |
| **`@Lazy` 自引用字段**（`@Autowired @Lazy` 接口类型字段） | 官方推荐局部解法：注入惰性代理，绕开创建期环，无需全局配置；JDK/CGLIB 双代理可调；**字段注入对 @Bean 手工 new 的对象同样生效**（后处理器照跑） | 特性开启时生成代码出现 spring 注入注解 | 推荐 |
| 写方法直接各挂一组 @CacheEvict（无 evictCaches） | 无自引用 | deleteById(id) 只有 id 参数，注解无法表达「按行其它列 key 失效」→ 必须运行时拿老数据 → 还是需要过代理的注解方法；且重复注解组 | 否决（用户既定 evictCaches 收口） |
| evict 用 `allEntries=true`（每表清 namespace） | 3 行注解、无需查老数据、无自引用 | 粒度粗（一次写清空整表缓存）；与用户 keyed + fetch-first 设计相悖 | 否决（记录为将来逃生口：cacheNames=表粒度时一行可退） |
| repository 写方法参数用 po（mapper 视图） | 无转换 | 服务层拿到持久化视图，repository 的 entity 视图价值丢失 | 否决 |
| 开关放 repositoryImpl 产物 + repository 侧反向引用 | — | 接口生成器读不到 impl 产物配置，需新增反向引用键 | 否决 |
| **开关放 repository 产物**（`springCache`） | impl 已引用 repository 产物（implements），读同一开关零新增引用；单开关两生成器一致 | 特性配置落在接口产物名下（语义上可接受：缓存是 repository 层能力） | 推荐（用户已确认） |

机制事实（已核对源码/官方行为）：@Cacheable/@CacheEvict 是代理 AOP——只拦外部调用、public 方法、可代理类（final 类/方法 CGLIB 拦不了）；Boot 2.6+ 循环依赖默认禁止、`@Lazy` 自引用是官方推荐解；接口方法注解在 CGLIB 下可能被忽略，实现类注解双代理通用。

## 战略定位

- **归属层**：改动作用于**生成器层产物模板**（`RepositoryGenerator`/`MybatisRepositoryImplGenerator`/`ConverterGenerator`[仅复用，不生成改动]）+ core reconcile 一处小扩展（方法签名比对纳入注解）+ config 键 + IT 样例；方言/运行期语义（Spring 代理、缓存）全部留在**生成代码与消费工程**，core 不引入任何 spring 依赖（只输出注解 FQN 文本与 import）。
- **架构不变量**：触碰 **reconcile 语义**（方法签名比对扩展纳入注解，属显式声明的小幅扩展，方向 = 更精确的 diff）；`@Generated` 成员所有权不破（新增 evictCaches/写方法/self 字段均 @Generated，用户手写同名成员保护逻辑继续生效）；产物=artifact 配置、查询契约、Generator 唯一扩展点不破。
- **波及面**：repository/repositoryImpl 产物输出全部变化（写方法无条件新增）→ `fixtures/gen/repository*`、`fixtures/gen/repository-impl/*` golden 重出、IT 样例生成代码重新提交；`AbstractJavaGenerator` 签名比对变化影响全生成器（方法注解一致的存量文件零churn，需全量门禁重跑）；记忆文档 architecture.md/README.md 需蒸馏更新；类级修饰符不 reconcile（已知限制）→ repositoryImpl 去 final 对存量文件需删除重生成（IT 样例内删除重出、文档提示消费者）。

## 方案

### 0. config 新键（repository 产物名下）

| 键 | 语义 |
|---|---|
| `springCache=true` | 开启 repository 层缓存生成（repository 接口出 evictCaches 声明；impl 出 @Cacheable/@CacheEvict/self 字段/fetch-first 写方法）。缺省 false |
| `lombok=true`（repositoryImpl 产物） | springCache=true 时日志形态：true → 类级 `@Slf4j`；缺省/false → 手写 Logger 字段。cache 关闭时无日志、该键无效 |
| `cacheName`（可选） | 缓存名；缺省 = 该表基类名（`NamingService` 表命名输出，如 `t_user` → `User`）。@Cacheable/@CacheEvict 两侧同值；**缓存名即每表命名空间，key 无需再拼表名** |

### 1. Repository 接口（RepositoryGenerator）——写方法无条件，evictCaches 条件

在既有 findBy* 之外追加（javadoc 沿 mapper 语义「插入记录/更新记录/按主键删除记录/清理该实体涉及的缓存键」）：

```java
int insert(User user);
int update(User user);
int deleteById(Long id);        // 仅表有单列主键时（与 MapperGenerator.primaryKey 判据一致）
void evictCaches(User user);    // 仅 springCache=true
```

- 参数类型/返回 = repository 的 target 视图（`refFqn` 同既有 findBy）；`deleteById` 参数 = 主键列该视图类型，方法名固定 `deleteById`（与 mapper 对齐，不随 naming.method.prefix 走）。
- `evictCaches` 无注解（注解全在 impl，见机制取舍）。

### 2. RepositoryImpl（MybatisRepositoryImplGenerator）

**类修饰符**：`finalClass()` 改为恒 false（用户决策 2「RepositoryImpl 不加 final」；CGLIB 可代理的前提；存量文件需删后重生成——类级修饰符不 reconcile）。

**self 字段（仅 springCache=true）**——字段注入、两 di 形态通用、不扰动既有构造器：

```java
@Autowired
@Lazy
private UserRepository self;   // 接口类型：JDK/CGLIB 双代理可派发 evictCaches
```

**日志（仅 springCache=true；用户意见 2026-09-06，lombok 化再调）——双形态**：
- `repositoryImpl.lombok=true` → 类级 `@Slf4j`（`lombok.extern.slf4j.Slf4j`），不生成 Logger 字段（log 由 lombok 注入）；
- 未启用 lombok → 手写字段 `private static final Logger log = LoggerFactory.getLogger(UserRepositoryImpl.class);`（初始化器经 tree `Variable.init`，打印已验证支持），**logger 是常量，按代码风格置类最顶层——首个成员，先于 mapper/converter/self 字段**（默认无 lombok 形态 = 主 golden 例 `spring-cache`；lombok 形态 = 特例 `spring-cache-with-lombok`）。

类级注解不 reconcile（与 enum lombok/类修饰符同类已知限制）——`repositoryImpl.lombok` 或 springCache 翻转对存量文件需删除重生成（cache-on 翻转本就因去 final/加 import 需重生成，覆盖此限制）。

**查询方法（仅 springCache=true）**：给全部 findBy* impl 方法（既有 bridging 循环产物）加注解；**不加在接口上**：

```java
@Cacheable(cacheNames = "User", key = "'findById:' + #id")
@Override
public @Nullable User findById(Long id) { ... }
```

单值方法**不设 `unless`**：null 结果默认入缓存（Spring 默认行为）——防缓存穿透（反复查不存在的 key 不再打库）。负条目在「该 key 变真实」时由 keyed 写路径 evict 清除：insert 对回填行 evict（覆盖自增 id 等全部新 key）、update 新值侧 evict（R1）见决策 R2；唯一残留 = 绕过 repository 直写 mapper 的脏负条目（cache-aside 纪律，公开 evictCaches 可手动清）。

**key 表达式**（`{methodName}:{arg0},{arg1},…` 的 SpEL 落法——cacheNames 已按表隔离，方法名后 `:`、参数间 `,`）：

- 单列 spec：`'findById:' + #id`
- 多列 spec：`'findByNameAndKind:' + #name + ',' + #kind`

`#a + #b` 的无分隔边界歧义由 `,` 分隔消除（`["ab","c"]` 与 `["a","bc"]` 不再同 key）；null 时 SpEL 拼成 `null` 字样，find/evict 两侧同源、行为一致。已知限制：某参数**值本身含 `,`** 时仍存在理论边界碰撞（简单拼接的固有限制，需转义/长度前缀才能消除，超需求，不引入）。参数名列与 entity 字段名列同源 = `ctx.fieldName(column)`，故 evict 侧把 `#f` 换成 `#user.f` 即同式（含分隔符逐字一致）。

**evictCaches 实现（仅 springCache=true，用户意见 2026-09-06：方法体加日志）**——每组 @CacheEvict 对应一行 log（cacheName 字面量 + 与注解 key 同构的 Java 拼接表达式，由同一 spec 循环产出，三方同源）：

```java
@CacheEvict(cacheNames = "User", key = "'findById:' + #user.id")
@CacheEvict(cacheNames = "User", key = "'findByName:' + #user.name")
@CacheEvict(cacheNames = "User", key = "'findByNameAndKind:' + #user.name + ',' + #user.kind")
@Override
public void evictCaches(User user) {
    log.info("evictCaches: {}:{}", "User", "findById:" + user.getId());
    log.info("evictCaches: {}:{}", "User", "findByName:" + user.getName());
    log.info("evictCaches: {}:{}", "User", "findByNameAndKind:" + user.getName() + "," + user.getKind());
}
```

（若只想整体一行而非每组一行，等价实现 = 一次 `log.info("evictCaches: cache={}, keys 见注解"...)`——默认按「每组 @CacheEvict 对应一行」落地，用户 review fixture 时可再调。）

**po 类型引用 import 化（用户意见 2026-09-06）**：converter 桥接模式（bridge.convert）下 impl 内 mapper 返回/入参类型（po）不再写 FQN，改为 import + 简单名（`UserPo po = ...`）；**仅当 po 简单名与 target/mapper/converter/repository 任一已引用简单名冲突时回退 FQN**（同简单名双 import 无法编译）。既有 find 桥接临时变量一并统一 → **constructor-di/field-di golden 与 IT 已提交生成代码随之变化（波及面扩大，在案）**。

@CacheEvict 组 = 该表全部索引派生 spec 各一条（与 find 方法同一 `ctx.indexes() × QueryMethods.of` 循环源，生成器单点产出，天然两侧一致）。

**写方法（无条件透传；springCache=true 换形态）**——按桥接模式分直连 / converter 桥（复用既有 `Bridge`；反向转换方法 `to<Source>` 已由 ConverterGenerator 生成，见 P4 修正）：

- `insert(User user)`：`po = conv.toUserPo(user); rows = mapper.insert(po);` cache-on 追加 `self.evictCaches(conv.toUser(po))`（XML `useGeneratedKeys` 已回填 po.id → 回填行含 id 与全列；用户「insert 不特意清 id key」自动满足：自增回填后清一次 id key 无害且更稳；无回填时 id=null → key 命中空，no-op）。
- `update(User user)` cache-off：`return mapper.update(conv.toUserPo(user));`
  cache-on：先按主键查老行 → update → 用老行清（与 deleteById 对称，用户 P1）：
  ```java
  UserPo old = userMapper.findById(user.getId());           // 查老数据（mapper 层，不经缓存）
  int rows = mapper.update(conv.toUserPo(user));
  if (rows > 0) {
      if (old != null) self.evictCaches(conv.toUser(old));  // 老值 key
      self.evictCaches(user);                               // 新值 key（决策 R1，待确认默认含）
  }
  return rows;
  ```
- `deleteById(Long id)` cache-off：`return mapper.deleteById(id);`
  cache-on：先查 → 删 → 清：
  ```java
  UserPo old = userMapper.findById(id);
  int rows = mapper.deleteById(id);
  if (rows > 0 && old != null) self.evictCaches(conv.toUser(old));
  return rows;
  ```

**写方法 fetch-first 与查旧行仅主键单列时生成**（复用 `MapperGenerator.primaryKey` 判据 + `QueryMethods.of(pkIndex)` 拿主键查方法名/列）；**无主键表**：repository 不生成 deleteById（对齐 mapper），cache-on 的 update 无老行可查 → 退化为仅 `evictCaches(user)`（best-effort，记文档限制）。

### 3. core reconcile 扩展（AbstractJavaGenerator）

`signature(Method)` 现 = 返回类型 + 参数类型 + 方法体；**追加方法注解集文本**（保序拼接）→ 开/关 springCache 时 find 方法体未变但注解变 → 触发替换（现有文件增删 @Cacheable）。影响全生成器方法比对：注解一致 → 无 churn；全量门禁验证。字段签名不动（self 字段 toggle 由「有/无」增删，已覆盖）。

### 4. 消费工程要求（文档 + IT 样例落地）

- 类路径需 spring-context（cache 注解）+ CacheManager；**IT 用内存缓存（Boot 默认 `spring.cache.type=simple` → ConcurrentMapCacheManager，用户决策）代替 Redis**——零额外依赖、不进 Docker/Testcontainers 门禁；**@EnableCaching**（IT 样例手写入口加）；代理风格不强制（JDK/CGLIB 均可，注解在 impl）；运行期不可用时自调用退化为直调 evictCaches（无副作用、不报错）。
- **编译需 `-parameters`**：cache key SpEL 按参数名 `#id`/`#user.id` 解析，依赖 javac 参数名可发现；本项目根 pom 无 `<parameters>true`（且 debug=false），IT pom 需显式加 `<parameters>true</parameters>`（Boot starter-parent 默认有，本项目自持 parent 没有）；验收 8 必须实测通过，否则 key 落法退到 `#root.args[i]` 定位式。
- **容器依赖**：springCache=true 的 impl 不可脱离容器裸 `new`（self 字段容器外为 null → 写方法 NPE）；@Bean 手工 new + 字段注入路径可用（后处理器照跑）。
- **接口契约破坏**：repository 接口新增 insert/update/deleteById 抽象方法 → 消费方手写 repository 实现类会编译失败（用户决策 1 的预期成本，release note 明示）。
- **视图 getter**：SpEL `#user.f` 属性访问依赖 repository target 视图的 getter（与 converter 既有 `source.getX()` 依赖同类；示例用 lombok 形态；非 lombok pojo 视图下 cache-on 属未支持组合，文档注明）。
- **NULL 缓存配套**（R2 缓存 NULL）：provider 必须允许 null 值（内存 ConcurrentMapCache OK；Redis 等需 `cache-null-values` 类配置）；负值短 TTL 防负条目填满缓存——注解无法表达 TTL，属 provider 级按 cacheName 配置，可选。
- **日志依赖双轨**（用户意见：evictCaches 方法体 log）：lombok 形态（repositoryImpl.lombok=true）需要消费方 lombok 注解处理器 + slf4j-api（Boot starter 自带 slf4j；lombok 需 pom 声明——IT 样例已用 lombok）；非 lombok 形态生成代码直接引用 org.slf4j.Logger/LoggerFactory——需 slf4j-api 在类路径。均为 springCache=true 才出现（与 spring 注解同类：特性开启才出现）。
- Boot 2.6+ 无需 `allow-circular-references`（@Lazy 已绕开）。
- 生成代码出现 spring 注解仅在该特性开启时（特性即 spring 集成边界）。

## 类职责与交互

- `RepositoryGenerator` — 接口产物：+insert/update/deleteById（无条件）+evictCaches 声明（springCache）— 读自身产物 option `springCache`；被 impl implements。
- `MybatisRepositoryImplGenerator` — impl 产物：写方法透传/fetch-first、@Cacheable、@CacheEvict 组、self @Lazy 字段、非 final — 经既有 `repository` 引用读同一 `springCache` 开关。
- `ConverterGenerator` — **不改**；反向 `to<Source>` 已存在，impl 写路径经既有 converter 引用调用（直连场景不转换）。
- `QueryMethods`/`QueryMethodFactory`/`MapperGenerator.primaryKey` — 复用（pk 判据、spec 循环、javadoc 摘要），不新增平行逻辑。
- `AbstractJavaGenerator` — 方法签名比对纳入注解（diff 精确化）。
- 交互：服务层 → repository(代理) → impl 写方法（查旧→写）→ `self.evictCaches(老行)` → 代理拦截 @CacheEvict 组 → 清 key；读路径 → repository(代理) → @Cacheable 命中/透传 impl bridging。职责归属（GRASP 信息专家）：key 表达式与注解的**唯一事实源 = 索引 spec 循环**，find 注解与 evict 组同源产出，杜绝两侧漂移。

## 对账与冲突清单（独立评审输出，已处置）

| 冲突/发现 | 处置 |
|---|---|
| 验收 2（cache off → final 类）与 §2/R3（恒非 final）内部矛盾 | 已修：验收 2 改「无 spring 注解/self/evictCaches」、非 final 为常态；类级修饰符不 reconcile → 存量 final 文件需删后重生成（IT 已提交 impl 文件先删再重出） |
| cache key SpEL `#参数` 依赖 javac `-parameters`；根 pom debug=false、IT pom 无 parameters | 已补 §4 编译前提 + IT pom `<parameters>true`；验收 8 实测，否则退 `#root.args[i]` |
| cache-on 翻转 off 后 import 残留（reconcile 不回收 import，既有保守策略） | 验收 6 措辞限定成员/注解层面，import 残留列已知限制 |
| `MapperGenerator.primaryKey` 判据不看列数、仓库无复合主键用例 | §2 措辞改「单列主键（现状支持面）」 |
| 漏列受影响测试：`EndToEndTest`/`ParameterizedArtifactsTest` 走全链路 | 验证节列入跑批名单（断言以 contains 为主预计不红）+ 补 repository 写方法端到端断言 |
| spring-cache golden case 需含多列/普通索引的 DDL 才能覆盖 `','` key 与多条 @CacheEvict | 验证节注明新增 fixture 输入 DDL（既有两 case 只有单列唯一键） |
| cache-on impl 不可裸 new（self 容器外 null → 写路径 NPE） | §4 补容器依赖要求；@Bean 手工 new + 字段注入可用 |
| repository 接口新增抽象方法 → 手写实现类编译失败 | 用户决策 1 预期成本，§4 + release note 明示 |
| SpEL `#user.f` 依赖视图 getter（非 lombok pojo 视图无 getter） | §4 注明：cache-on + 无 getter 视图属未支持组合 |
| 机制核对（converter 双向 toX 已存在 / signature 现不含注解 / @CacheEvict 可重复 / @Lazy+字段注入对 @Bean new 生效 / JDK 代理认 impl 注解） | 除 @CacheEvict 可重复外全部为真，设计相应表述已保留或修正 |
| **实现偏差 D1**：@CacheEvict 多组 → 单 `@Caching(evict={...})` | Spring 5.3 @CacheEvict **无 @Repeatable**（评审断言有误，IT javac 暴露）；@Caching 全版本通用。产物以 fixture 为准 |
| **实现偏差 D2**：非 cache 透传初版漏 converter | golden 重出暴露 → 修复 convert 时 toUserPo 透传（见 progress.md） |
| **实现偏差 D6**：验收 6 注解 toggle 用例经 ReconcileLifecycleTest + 自定义 deprecated 选项落地 | 见 progress.md |

## 决策点（R 标记 = 用户评审待确认）

- R1 update 是否在 `evictCaches(老行)` 外**追加 `evictCaches(user)`（新值）**：用户 P1 说「与 deleteById 一致、只用老数据」；设计默认含新值清一次——独立评审补强证据：**非唯一索引 List 查询下新值侧缓存此前不含该行，老值 evict 清不到**（update 改索引列 a→b，findByB 的 List/空列表缓存在老值 evict 后仍残留）——含新值是必要而非可选，非仅负缓存理由。deleteById 无此问题（行删除后新值无缓存语义）。**推荐采纳（含新值）**；否决则残留 = 新值侧空列表缓存。
- R2 **缓存 NULL（防穿透）**：单值 @Cacheable 不加 `unless`，null 与空结果默认入缓存（Spring 默认行为；早稿 R2 曾推荐 `unless="#result == null"` 防脏负结果，用户以穿透防护为由否决，复核成立——负条目在该 key 变真实时由 insert 回填行 evict / update 新值 evict（R1）清除，不产生永久脏读）。配套：provider 需允许 null（内存 OK；Redis 等需 cache-null-values）；负值短 TTL 防负条目填满缓存，属 provider 级配置（注解无法表达 TTL），消费工程可选。**已定（用户确认）**。
- R3 类去 final **无条件**（用户决策 2 原话，已定；且避免改 `finalClass()` SPI 签名——`AbstractJavaGenerator.finalClass()` 无参，按 cache 分支需破坏 Generator 唯一扩展点）。验收 2 已同步改为「非 final 为常态、存量文件需删后重生成」。
- R4 key 格式 = `{methodName}:{arg0},{arg1},…`（方法名后 `:`、参数间 `,`，用户定稿，否决 asList 复杂 SpEL）；已知限制：参数值含 `,` 时理论碰撞。已定。

## 改动文件与影响面

- 改动：`ddl-codegen-core/.../gen/RepositoryGenerator.java`（写方法 + 条件 evictCaches）、`MybatisRepositoryImplGenerator.java`（写方法/注解/self/final）、`AbstractJavaGenerator.java`（signature 含注解）、`docs/architecture.md`（config 键、生成器行、契约）、`docs/changes/README.md`（收尾索引行）、IT 样例 `ddl-codegen.properties`（开 springCache）+ 手写 `SampleBeans/SampleApplication`（@EnableCaching）+ 生成代码重出、`docs/changes/TEMPLATE.md` 无。
- 影响：所有含 repository 产物的既有输出新增 3 写方法（goldens：`fixtures/gen/repository/{no-indexes,finders}`、`fixtures/gen/repository-impl/{constructor-di,field-di}`、IT 提交代码重出）；`RepositoryGeneratorTest`/`MybatisRepositoryImplGeneratorTest`/`ReconcileLifecycleTest` 扩展；converter 产物不变（P4 修正：反向方法已存在）；文档影响 = 记忆文档（architecture.md + changes/README.md）→ 收尾需跑「记忆文档自检用例集」与 `new-change.sh check-docs`。
- 无 spring 依赖进 core（注解仅文本/import；impl 依赖 spring 注解符号由**消费工程**提供，与既有 `annotations.nullable` checker-qual 模式一致——消费方需具备所配注解依赖，文档注明）。

## 验收标准

> 1. 给定含单列主键表的 DDL + repository 产物配置 → 生成的 UserRepository 恒含 `int insert(User)`、`int update(User)`、`int deleteById(Long)`（无主键表无 deleteById），impl 恒含对应透传实现（converter 桥接用 `to<Source>` 且 po 类型经 import 简单名引用[与已引用简单名冲突时回退 FQN]，直连直传）
> 2. 给定 `springCache=false` → 生成文件无任何 spring cache 注解/self 字段/evictCaches（import 残留除外，见验收 6 注）；repositoryImpl 为**非 final**（类级修饰符不 reconcile → 存量 final 文件需删后重生成）
> 3. 给定 `springCache=true` → 接口含 `void evictCaches(User)`；impl 非 final；含 `@Autowired @Lazy` 的接口类型 self 字段与 slf4j 静态 Logger 字段；查询方法带 `@Cacheable(cacheNames, key)`，key 文本 = `'<方法名>:'` 前缀 + 参数（单列 `#<参数>`、多列以 `','` 分隔、无表名前缀）；单值查询**不带 unless**（null 默认入缓存，防穿透，R2）；日志形态二选一：`repositoryImpl.lombok=true` → 类级 `@Slf4j` + `lombok.extern.slf4j.Slf4j` import + 无 Logger 字段；lombok 缺省 → `private static final Logger log = ...` 字段 + org.slf4j import；`evictCaches` 上 @CacheEvict 组与全部 findBy* 一一对应、key 表达式把 `#f` 换 `#user.f` 后逐字一致，方法体含与每组对应的 `log.info("evictCaches: {}:{}", ...)` 一行
> 4. 给定 `springCache=true` → deleteById 体 = 先 `userMapper.findBy<主键>(id)` → `deleteById(id)` → 老行非空且删行数>0 时 `self.evictCaches(...)`；update 体 = 先查老行 → update → 老行（+R1 采纳时含新值）evict；insert 体 = insert 后对回填 po 的 entity evict（cache 关闭时三者均为纯透传）
> 5. 给定 springCache=true → impl 含 `@Autowired @Lazy` 的接口类型 self 字段；构造器参数（di=constructor）不含 self
> 6. 同一 DDL/config 连续生成两次 → 第二次所有文件无变化（幂等）；对已生成文件翻转 springCache 再生成 → 注解/self/evictCaches 正确增删（reconcile 注解比对生效）。注：成员删除不回收 import 是既有保守策略（architecture 已知限制）——on→off 后 spring cache 相关 import 行残留属已知，验收 2 的「无 spring」限定在成员与注解层面
> 7. 目标文件含用户手写同名方法/字段 → 保留用户版、跳过新增 + warning（@Generated 所有权不破）
> 8. IT 样例（Boot 2.7 + @EnableCaching + **simple 内存缓存 ConcurrentMapCacheManager**）springCache=true：insert → findById 命中缓存（MyBatis Interceptor 计数 SQL，二次读不落库）→ update/deleteById 后缓存失效（再读落库）→ 行为正确；**NULL 防穿透实证**：findByName("不存在") 两次查询只落库一次（null 入缓存），随后 insert 同名行 → 再查走库（insert 回填行 evict 清掉负条目）；无 Docker 自动 skip 分级不变

## 验证

- 测试：golden 场景扩展（repository 两 case 追加写方法期望；repository-impl 新增两 case：`spring-cache`（默认无 lombok，手写 Logger 常量置顶）与 `spring-cache-with-lombok`（@Slf4j），断言注解/self/evictCaches/fetch-first 文本 + `cache-off` 透传文本）；`ReconcileLifecycleTest` 增注解 toggle 用例（验收 6）；`MybatisRepositoryImplGeneratorTest` 错误组（converter 缺失校验在写方法路径下仍成立）；IT 缓存行为测试（验收 8，含 NULL 缓存防穿透 + insert 清除负条目场景）。
- 验证命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`（全量门禁绿 + 静态检查 0 告警；fixtures 重出用 `-Dgolden.update=true` 并审阅 diff）
- 跑批名单：`RepositoryGeneratorTest` / `MybatisRepositoryImplGeneratorTest` / `ReconcileLifecycleTest` / `EndToEndTest` / `ParameterizedArtifactsTest` 全量；IT 模块生成代码重出前先删除已提交的 `UserRepositoryImpl.java`（类级修饰符不 reconcile，`final` 不会自动去除）
- IT 样例手写改动：`SampleApplication`（或配置类）加 `@EnableCaching`；`SampleBeans` 若继续手工 new impl（字段注入自引用可用）则更新其 javadoc——现文「生成物无 Spring 注解……可关闭 CGLIB 代理并 final 化」与本特性矛盾，需同步改写；IT pom 加 `<parameters>true</parameters>`
