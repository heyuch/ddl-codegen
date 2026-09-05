# 变更台账：20260906-06-feat-generated-code-javadoc

## 状态

**实现完成**（2026-09-06：core 121 绿；提交 b409ebf；design/progress/索引随收尾）

## 实现记录

- `CommentDocs`（gen）：summary 清洗（剥 @token/枚举项 token）；classDoc/methodDoc/fieldDoc/findBySummary（列注释缺失回退字段名）。
- 落点：PojoGenerator（类=表注释、字段=列注释）；EnumGenerator（类=表注释退列注释、常量=desc、code/desc 字段与 getCode/getDesc/fromCodeNullable/fromCode/私有构造模板）；MapperGenerator（类 + insert/update/deleteById/findBy）；QueryMethodFactory（Repository findBy 摘要）；MybatisRepositoryImplGenerator（类、Mapper/转换器字段、@Override findBy 摘要）；ConverterGenerator（类、toX/toXList 方向）。
- fixtures 全量重出（22 文件 +402 行）抽查确认（表/列注释清洗为摘要、枚举 desc 入常量 javadoc）；core 门禁 121 绿；07 反引号保留字 golden 在重出后保持。

## 待办

- 03 模块再生成（含 javadoc 新产物）与收尾；记忆文档 architecture/README javadoc 一句。

