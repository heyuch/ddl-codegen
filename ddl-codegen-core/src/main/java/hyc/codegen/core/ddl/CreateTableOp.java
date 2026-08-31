package hyc.codegen.core.ddl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

/**
 * 创建（或整体替换）一张表的操作。
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "DDL 操作携带表模型，应用语义要求同一引用")
public final class CreateTableOp implements DdlOperation {

    private final Table table;

    public CreateTableOp(Table table) {
        this.table = table;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        schema.addTable(table);
        result.affect(tableName());
    }

    /** 完整的表模型（含列、索引、注解元数据）。 */
    public Table getTable() {
        return table;
    }

    @Override
    public String tableName() {
        return table.getName();
    }

}
