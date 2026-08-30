package hyc.codegen.core.ddl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAlterColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropColumnItem;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropPrimaryKey;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableItem;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRename;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRenameColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRenameIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.sql.ast.statement.SQLUniqueConstraint;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlKey;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlPrimaryKey;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlUnique;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlAlterTableChangeColumn;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlAlterTableModifyColumn;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlRenameTableStatement;
import hyc.codegen.core.annotation.AnnotationProcessor;
import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.annotation.MetaTarget;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.model.Table;

// 扇出/抽象耦合/圈复杂度抑制依据（元素驱动而非逻辑混杂，见 docs/static-rules-review.md §6）：
// 本类是 Druid AST → 模型操作的转换器，引用类型数 ≈ 需处理的节点类型数（列/索引/各类
// ALTER 子项与操作类型），每种节点对应一个转换分支，单方法引用类型 ≤4；convertAlter 的
// 分支数 ≈ ALTER 子句类型数（11 类）+ 畸形 DDL 判空跳过（SpotBugs 严格空指针修复引入），
// 已做抽取（DruidAst）后残余指标仍结构性偏高，与 JavaTreeConverter 同属"分发器"类别。
@SuppressWarnings({"ClassFanOutComplexity", "ClassDataAbstractionCoupling", "CyclomaticComplexity"})
public final class DruidDdlParser implements DdlParser {

    private static final System.Logger LOG = System.getLogger(DruidDdlParser.class.getName());

    private final AnnotationProcessor annotationProcessor;

    /** 默认实例：内置注解处理器（type/as/ignore）。 */
    public DruidDdlParser() {
        this(new AnnotationProcessor(AnnotationRegistry.builtin()));
    }

    /**
     * 注入注解分发器（支持自定义注解处理器）。
     *
     * @param annotationProcessor 注解分发器（可注入含自定义处理器的注册表）
     */
    public DruidDdlParser(AnnotationProcessor annotationProcessor) {
        this.annotationProcessor = annotationProcessor;
    }

    @Override
    public List<DdlOperation> parse(String ddl) {
        List<DdlOperation> operations = new ArrayList<>();
        for (SQLStatement statement : SQLUtils.parseStatements(ddl, DbType.mysql)) {
            if (statement instanceof SQLCreateTableStatement) {
                convertCreate((SQLCreateTableStatement)statement, operations);
            } else if (statement instanceof MySqlRenameTableStatement) {
                convertRenameTable((MySqlRenameTableStatement)statement, operations);
            } else if (statement instanceof SQLAlterTableStatement) {
                convertAlter((SQLAlterTableStatement)statement, operations);
            } else if (statement instanceof SQLDropTableStatement) {
                convertDrop((SQLDropTableStatement)statement, operations);
            } else {
                LOG.log(System.Logger.Level.WARNING,
                        "不识别的语句类型 {0}，已跳过", statement.getClass().getSimpleName());
            }
        }
        return operations;
    }

    private void convertRenameTable(MySqlRenameTableStatement statement, List<DdlOperation> operations) {
        for (MySqlRenameTableStatement.Item item : statement.getItems()) {
            String from = DruidAst.nameOf(item.getName());
            String to = DruidAst.nameOf(item.getTo());
            if (from != null && to != null) {
                operations.add(new RenameTableOp(from, to));
            }
        }
    }

    private void convertCreate(SQLCreateTableStatement statement, List<DdlOperation> operations) {
        String name = DruidAst.nameOf(statement.getName());
        if (name == null) {
            LOG.log(System.Logger.Level.WARNING, "建表语句缺少表名，已跳过");
            return;
        }

        String comment = DruidAst.commentOf(statement.getComment());
        Table table = new Table(name, comment);
        annotationProcessor.process(comment, MetaTarget.TABLE, table.getMeta());

        List<String> primaryColumns = new ArrayList<>();
        for (SQLTableElement element : statement.getTableElementList()) {
            if (element instanceof SQLColumnDefinition) {
                Column column = convertColumn((SQLColumnDefinition)element);
                table.addColumn(column);
                if (((SQLColumnDefinition)element).isPrimaryKey()) {
                    primaryColumns.add(column.getName());
                }
            } else if (element instanceof MySqlPrimaryKey) {
                Index index = convertConstraint((SQLUniqueConstraint)element, true, Index.PRIMARY);
                if (index != null) {
                    table.addIndex(index);
                }
            } else if (element instanceof MySqlUnique) {
                Index index = convertConstraint((SQLUniqueConstraint)element, true, null);
                if (index != null) {
                    table.addIndex(index);
                }
            } else if (element instanceof MySqlKey) {
                // 普通 KEY（非 UNIQUE）在 Druid 中也实现 SQLUniqueConstraint，必须按类区分
                Index index = convertConstraint((SQLUniqueConstraint)element, false, null);
                if (index != null) {
                    table.addIndex(index);
                }
            } else if (element instanceof SQLUniqueConstraint) {
                Index index = convertConstraint((SQLUniqueConstraint)element, true, null);
                if (index != null) {
                    table.addIndex(index);
                }
            } else {
                LOG.log(System.Logger.Level.WARNING,
                        "不支持的建表元素 {0}，已跳过", element.getClass().getSimpleName());
            }
        }

        if (!primaryColumns.isEmpty() && table.getIndex(Index.PRIMARY) == null) {
            table.addIndex(Index.builder().name(Index.PRIMARY).unique(true).columns(primaryColumns).build());
        }

        operations.add(new CreateTableOp(table));
    }

    private Column convertColumn(SQLColumnDefinition definition) {
        SQLDataType dataType = definition.getDataType();
        String typeName = dataType == null ? "unknown" : dataType.getName().toLowerCase(Locale.ROOT);

        Column.Builder builder = Column.builder()
                .name(definition.getColumnName())
                .sqlType(typeName)
                .nullable(!definition.containsNotNullConstaint())
                .unsigned(DruidAst.unsigned(definition))
                .autoIncrement(definition.isAutoIncrement())
                .comment(DruidAst.commentOf(definition.getComment()));
        if (definition.getDefaultExpr() != null) {
            builder.defaultValue(DruidAst.textOf(definition.getDefaultExpr()));
        }

        if (dataType != null) {
            if ("enum".equals(typeName)) {
                List<String> values = new ArrayList<>();
                String value = DruidAst.stringArgument(dataType, values.size());
                while (value != null) {
                    values.add(value);
                    value = DruidAst.stringArgument(dataType, values.size());
                }
                builder.enumValues(values);
            } else if ("decimal".equals(typeName)) {
                builder.precision(DruidAst.intArgument(dataType, 0))
                        .scale(DruidAst.intArgument(dataType, 1));
            } else {
                builder.length(DruidAst.intArgument(dataType, 0));
            }
        }

        Column column = builder.build();
        annotationProcessor.process(column.getComment(), MetaTarget.COLUMN, column.getMeta());
        return column;
    }

    @Nullable
    private Index convertConstraint(SQLUniqueConstraint constraint, boolean unique, @Nullable String forcedName) {
        List<String> columns = DruidAst.columnNames(constraint.getColumns());
        if (columns.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "唯一约束缺少列定义，已跳过: {0}", constraint);
            return null;
        }
        String name = forcedName != null
                ? forcedName
                : indexName(DruidAst.nameOf(constraint.getName()), unique, columns);
        Index model = Index.builder()
                .name(name)
                .unique(unique)
                .columns(columns)
                .comment(DruidAst.commentOf(constraint.getComment()))
                .build();
        annotationProcessor.process(model.getComment(), MetaTarget.INDEX, model.getMeta());
        return model;
    }

    private void convertAlter(SQLAlterTableStatement statement, List<DdlOperation> operations) {
        String tableName = DruidAst.nameOf(statement.getName());
        if (tableName == null) {
            LOG.log(System.Logger.Level.WARNING, "alter 语句缺少表名，已跳过");
            return;
        }

        for (SQLAlterTableItem item : statement.getItems()) {
            if (item instanceof SQLAlterTableAddColumn) {
                for (SQLColumnDefinition definition : ((SQLAlterTableAddColumn)item).getColumns()) {
                    operations.add(new AddColumnOp(tableName, convertColumn(definition)));
                }
            } else if (item instanceof SQLAlterTableDropColumnItem) {
                for (SQLName column : ((SQLAlterTableDropColumnItem)item).getColumns()) {
                    String name = DruidAst.nameOf(column);
                    if (name != null) {
                        operations.add(new DropColumnOp(tableName, name));
                    }
                }
            } else if (item instanceof MySqlAlterTableChangeColumn) {
                MySqlAlterTableChangeColumn change = (MySqlAlterTableChangeColumn)item;
                String oldName = DruidAst.nameOf(change.getColumnName());
                if (oldName != null) {
                    operations.add(
                            new ChangeColumnOp(tableName, oldName, convertColumn(change.getNewColumnDefinition())));
                }
            } else if (item instanceof MySqlAlterTableModifyColumn) {
                MySqlAlterTableModifyColumn modify = (MySqlAlterTableModifyColumn)item;
                Column column = convertColumn(modify.getNewColumnDefinition());
                operations.add(new ChangeColumnOp(tableName, column.getName(), column));
            } else if (item instanceof SQLAlterTableAddIndex) {
                SQLAlterTableAddIndex addIndex = (SQLAlterTableAddIndex)item;
                Index index = convertAddIndex(addIndex);
                if (index != null) {
                    operations.add(new AddIndexOp(tableName, index));
                }
            } else if (item instanceof SQLAlterTableDropIndex) {
                String indexName = DruidAst.nameOf(((SQLAlterTableDropIndex)item).getIndexName());
                if (indexName != null) {
                    operations.add(new DropIndexOp(tableName, indexName));
                }
            } else if (item instanceof SQLAlterTableDropPrimaryKey) {
                operations.add(new DropIndexOp(tableName, Index.PRIMARY));
            } else if (item instanceof SQLAlterTableRename) {
                String toName = DruidAst.nameOf(((SQLAlterTableRename)item).getToName());
                if (toName != null) {
                    operations.add(new RenameTableOp(tableName, toName));
                }
            } else if (item instanceof SQLAlterTableRenameIndex) {
                SQLAlterTableRenameIndex rename = (SQLAlterTableRenameIndex)item;
                String from = DruidAst.nameOf(rename.getName());
                String to = DruidAst.nameOf(rename.getTo());
                if (from != null && to != null) {
                    operations.add(new RenameIndexOp(tableName, from, to));
                }
            } else if (item instanceof SQLAlterTableRenameColumn) {
                SQLAlterTableRenameColumn rename = (SQLAlterTableRenameColumn)item;
                String from = DruidAst.nameOf(rename.getColumn());
                String to = DruidAst.nameOf(rename.getTo());
                if (from != null && to != null) {
                    operations.add(new RenameColumnOp(tableName, from, to));
                }
            } else if (item instanceof SQLAlterTableAlterColumn) {
                LOG.log(System.Logger.Level.WARNING,
                        "不支持的 alter 子句（列默认值/可空变更）: {0}，已跳过", item.getClass().getSimpleName());
            } else {
                LOG.log(System.Logger.Level.WARNING,
                        "不支持的 alter 子句 {0}，已跳过", item.getClass().getSimpleName());
            }
        }
    }

    @Nullable
    private Index convertAddIndex(SQLAlterTableAddIndex addIndex) {
        List<String> columns = DruidAst.columnNames(addIndex.getItems());
        if (columns.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "add index 缺少列定义，已跳过: {0}", addIndex.getName());
            return null;
        }
        boolean unique = "UNIQUE".equalsIgnoreCase(addIndex.getType());
        Index model = Index.builder()
                .name(indexName(DruidAst.nameOf(addIndex.getName()), unique, columns))
                .unique(unique)
                .columns(columns)
                .comment(DruidAst.commentOf(addIndex.getComment()))
                .build();
        annotationProcessor.process(model.getComment(), MetaTarget.INDEX, model.getMeta());
        return model;
    }

    private void convertDrop(SQLDropTableStatement statement, List<DdlOperation> operations) {
        for (SQLExprTableSource source : statement.getTableSources()) {
            String name = DruidAst.nameOf(source.getName());
            if (name != null) {
                operations.add(new DropTableOp(name));
            }
        }
    }

    /** 索引名兜底：未命名索引按 MySQL 惯例合成（仅作为模型句柄，不参与查询方法命名）。 */
    private String indexName(@Nullable String explicitName, boolean unique, List<String> columns) {
        if (explicitName != null && !explicitName.isEmpty()) {
            return explicitName;
        }
        String prefix = unique ? "uk_" : "idx_";
        return prefix + String.join("_", columns);
    }

}
