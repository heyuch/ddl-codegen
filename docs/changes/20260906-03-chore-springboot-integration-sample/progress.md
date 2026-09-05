# 变更台账：20260906-03-chore-springboot-integration-sample

## 状态

**实现完成**（2026-09-06：reactor BUILD SUCCESS；模块 IT 无 Docker 自动跳过）

## 评审与决策

- 独立评审（v1）必改已回写 design v2（版本/@DynamicPropertySource/DDL 硬约束/场景措辞/装配/门禁处置）。
- 用户决策：模块门禁 = 生成器质量验收器（v3 门禁分级：质量类全开、jacoco 跳过理由、spotless 提交物归一化）。

## 实现记录与 spike 发现（门禁=验收器实证）

1. **首次真实 javac 即暴露 @Generated 缺 value + impl/converter 非 final** → 04 fix（另立已提交）。
2. **lombok `@Builder/@Data` 生成代码引用 `edu.umd.cs.findbugs.annotations.SuppressFBWarnings`** → 消费方需 spotbugs-annotations 依赖（模块 pom + README 运行时依赖注明）。
3. **di=constructor 生成 impl 缺字段声明**（构造器赋不存在的字段）→ 生成器补 final 字段；fixtures 更新。
4. **单值 findBy 空安全守卫二次调用导致 spotbugs NP_NULL** → 生成器改临时变量（FQN 免 import）一次调用守卫；fixtures/EndToEnd 同步。
5. **EI_EXPOSE_REP2**（DI 无状态 Bean 误报）→ 模块级 spotbugs exclude（`${project.basedir}` 绝对路径，带理由），核心 NPE 类问题已由 4 修复。
6. javadoc（06）之后模块再生成产物含 javadoc；最终模块文件：final/value/javadoc/ctor-di 字段齐全。
7. 装配：`@MapperScan` + `SampleBeans`（converter/repositoryImpl Bean 显式注册，proxyBeanMethods=false final）；Testcontainers IT 覆盖 mapper 存 code → repository+converter 还原枚举（Status.ACTIVE/getCode/desc、Kind.TRIAL）。

## 门禁分级结论

- 质量类（checkstyle/spotbugs + 模块化 javac 无 core 分析器）**全开且绿**；jacoco 模块级跳过（覆盖率非质量信号，理由记录）；exclude 仅 EI_EXPOSE_REP2（DI 误报，带理由，非静默）。
- 模块不在 core ArchUnit 约束内（纯消费者）。

## 已知 / 边界

- 类级修饰符/成员 javadoc 不 reconcile（升级需删文件重生成，04 已录）。
- 单值 findBy 未命中经修复后返回 null（无 NPE），IT 覆盖命中路径。

## 待办

- 03 提交（本变更）+ 索引行；memory 自检/check-docs（architecture/README 已改）。
