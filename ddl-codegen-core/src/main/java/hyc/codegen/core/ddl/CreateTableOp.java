package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Table;

/**
 * 创建（或整体替换）一张表的操作。
 */
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
