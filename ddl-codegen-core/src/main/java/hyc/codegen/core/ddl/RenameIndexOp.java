package hyc.codegen.core.ddl;

/**
 * 索引改名的操作（{@code alter table ... rename index a to b}）。
 */
public final class RenameIndexOp implements DdlOperation {

    private final String tableName;
    private final String from;
    private final String to;

    public RenameIndexOp(String tableName, String from, String to) {
        this.tableName = tableName;
        this.from = from;
        this.to = to;
    }

    /** 旧索引名。 */
    public String getFrom() {
        return from;
    }

    /** 新索引名。 */
    public String getTo() {
        return to;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
