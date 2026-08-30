package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

/**
 * 列改名的操作（{@code alter table ... rename column a to b}）。
 */
public final class RenameColumnOp implements DdlOperation {

    private final String tableName;
    private final String from;
    private final String to;

    public RenameColumnOp(String tableName, String from, String to) {
        this.tableName = tableName;
        this.from = from;
        this.to = to;
    }

    /** 旧列名。 */
    public String getFrom() {
        return from;
    }

    /** 新列名。 */
    public String getTo() {
        return to;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        Table table = schema.getTable(tableName);
        if (table != null) {
            table.renameColumn(from, to);
            result.affect(tableName);
            result.columnRenamed(tableName, from, to);
        }
    }

}
