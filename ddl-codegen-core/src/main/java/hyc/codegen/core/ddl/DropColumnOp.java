package hyc.codegen.core.ddl;

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

    /** 被删除的列名。 */
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
