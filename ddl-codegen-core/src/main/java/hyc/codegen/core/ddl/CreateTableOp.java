package hyc.codegen.core.ddl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hyc.codegen.core.model.Table;

/**
 * 创建（或整体替换）一张表的操作。
 */
// EI 抑制：DDL 操作携带完整表模型：StatementApplier 直接 schema.addTable(op.getTable()) 存同一引用（应用语义），拷贝破坏「Schema 表 = 操作模型」一致性
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class CreateTableOp implements DdlOperation {

    private final Table table;

    public CreateTableOp(Table table) {
        this.table = table;
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
