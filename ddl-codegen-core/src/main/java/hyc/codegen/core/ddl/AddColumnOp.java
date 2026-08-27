package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Column;

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

    /** 新增的列定义。 */
    public Column getColumn() {
        return column;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
