package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Index;
import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

/**
 * 新增索引的操作（{@code alter table ... add index/unique key}）。
 */
public final class AddIndexOp implements DdlOperation {

    private final String tableName;
    private final Index index;

    public AddIndexOp(String tableName, Index index) {
        this.tableName = tableName;
        this.index = index;
    }

    /** 新增的索引定义。 */
    public Index getIndex() {
        return index;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        Table table = schema.getTable(tableName);
        if (table != null) {
            table.addIndex(index);
            result.affect(tableName);
        }
    }

}
