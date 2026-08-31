package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Schema;

/**
 * 表改名的操作（{@code alter table a rename to b}）。
 * <p>
 * 应用器必须产出改名记录（而非删旧建新），供生成阶段移动/改写文件。
 */
public final class RenameTableOp implements DdlOperation {

    private final String from;
    private final String to;

    public RenameTableOp(String from, String to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void apply(Schema schema, ApplyResult result) {
        schema.renameTable(from, to);
        result.affect(from);
        result.affect(to);
        result.tableRenamed(from, to);
    }

    /** 旧表名。 */
    public String getFrom() {
        return from;
    }

    /** 新表名。 */
    public String getTo() {
        return to;
    }

    @Override
    public String tableName() {
        return from;
    }

}
