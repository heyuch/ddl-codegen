package hyc.codegen.core.ddl;

/**
 * 删除一张表的操作。
 */
public final class DropTableOp implements DdlOperation {

    private final String tableName;

    public DropTableOp(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
