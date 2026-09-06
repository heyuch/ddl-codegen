# 变更台账：20260906-09-chore-demo-fixture-to-resources

## 状态

**实现完成**（2026-09-06：全量门禁绿 + 编译期 0 告警；提交待落）

## 评审记录

- 2026-09-06 用户提问「Demo.java 是否可移入 test resources 去掉特殊处理」→ 完成影响面核查（全仓 grep：Demo 仅被 JavaParserTest/RoundTripSmokeTest 作文本夹具 parse→print，无编译期类引用）→ 方案呈现 → 用户确认执行。小改动（改动直白、无行为变化），design.md 精简记录取舍与验收。

## 实现记录

- `git mv`：`src/test/java/hyc/codegen/tree/Demo.java` → `src/test/resources/fixtures/roundtrip/Demo.java`（tree 模块首个 resources 目录；路径与 core `fixtures/gen/<case>` 惯例同构，`roundtrip` 表用途）。
- 消费方路径更新：`JavaParserTest.parse`、`RoundTripSmokeTest.demoRoundTripIsByteExact` → `src/test/resources/fixtures/roundtrip/Demo.java`（沿用 File/CWD 风格，与 core harness 同款）。
- 三处特殊处理删除：根 pom checker `-AskipDefs=hyc\.codegen\.tree\.Demo`（含注释）、pom spotless `<exclude>`（含注释，includes 仅 `src/{main,test}/java`，resources 天然不格式化）、checkstyle-suppressions.xml 首条整文件 suppression（保留测试级 suppression）。
- static-rules-review §3 M0.1 spotless 行追加消除注记（指向本变更）。
- 验证：全量门禁绿（340 tests）+ 编译期 0 告警；`grep Demo|AskipDefs pom.xml/checkstyle-suppressions.xml` 为空；JavaParserTest 1/1、RoundTripSmokeTest 3/3（Demo 字节全等）绿。

## 边界（不做，随记录）

- Demo 语法/语义合法性不再经 test-compile 校验——由 parse→print 字节全等断言兜底（parse 失败即测试失败）；字节断言针对文件内容、与位置无关。
- Demo 后续新增语法要素（javadoc/枚举/注解）不再受 checker/EP/checkstyle/spotless 约束，改完跑字节断言即可。

## 收尾与自检

- 记忆文档自检（新上下文实例 10 问走查）：五份记忆文档与代码/构建配置全部自洽，无漂移；Demo 相关处理全仓 `*.xml` grep 零残留。
- `new-change.sh check` / `check-docs` 通过。
