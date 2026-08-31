package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Schema;

/**
 * 删除一张表的操作。
 */
public final class DropTableOp implements DdlOperation {

    private final String tableName;

    public DropTableOp(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        schema.removeTable(tableName());
        result.affect(tableName());
        result.dropped(tableName());
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
