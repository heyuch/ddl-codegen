package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

/**
 * 删除索引的操作（{@code alter table ... drop index}；主键用 {@link hyc.codegen.core.model.Index#PRIMARY}）。
 */
public final class DropIndexOp implements DdlOperation {

    private final String tableName;
    private final String indexName;

    public DropIndexOp(String tableName, String indexName) {
        this.tableName = tableName;
        this.indexName = indexName;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        Table table = schema.getTable(tableName);
        if (table != null) {
            table.removeIndex(indexName);
            result.affect(tableName);
        }
    }

    /** 被删除的索引名。 */
    public String getIndexName() {
        return indexName;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
