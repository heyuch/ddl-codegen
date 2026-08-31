package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

/**
 * 新增列的操作（{@code alter table ... add column}）。
 */
public final class AddColumnOp implements DdlOperation {

    private final String tableName;
    private final Column column;

    public AddColumnOp(String tableName, Column column) {
        this.tableName = tableName;
        this.column = column;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        Table table = schema.getTable(tableName);
        if (table != null) {
            table.addColumn(column);
            result.affect(tableName);
        }
    }

    /** 新增的列定义。 */
    public Column getColumn() {
        return column;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
