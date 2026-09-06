package io.github.heyuch.codegen.core.ddl;

import io.github.heyuch.codegen.core.model.Index;
import io.github.heyuch.codegen.core.model.Schema;
import io.github.heyuch.codegen.core.model.Table;

/**
 * 删除索引的操作（{@code alter table ... drop index}；主键用 {@link Index#PRIMARY}）。
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
