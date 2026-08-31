package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

/**
 * 删除列的操作（{@code alter table ... drop column}）。
 */
public final class DropColumnOp implements DdlOperation {

    private final String tableName;
    private final String columnName;

    public DropColumnOp(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        Table table = schema.getTable(tableName);
        if (table != null) {
            table.removeColumn(columnName);
            result.affect(tableName);
        }
    }

    /** 被删除的列名。 */
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
