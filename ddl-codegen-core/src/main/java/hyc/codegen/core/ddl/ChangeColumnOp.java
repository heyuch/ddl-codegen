package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Column;

/**
 * 列定义变更的操作（{@code modify column} 或 {@code change column}）。
 * <p>
 * {@code change column a b ...} 时 {@code oldName} 为 a、新列名为 b；
 * {@code modify column a ...} 时两者相同。应用器按新列定义整体替换，保持原列位置。
 */
public final class ChangeColumnOp implements DdlOperation {

    private final String tableName;
    private final String oldName;
    private final Column newColumn;

    public ChangeColumnOp(String tableName, String oldName, Column newColumn) {
        this.tableName = tableName;
        this.oldName = oldName;
        this.newColumn = newColumn;
    }

    /** 变更前的列名。 */
    public String getOldName() {
        return oldName;
    }

    /** 变更后的完整列定义（可能带新列名）。 */
    public Column getNewColumn() {
        return newColumn;
    }

    @Override
    public String tableName() {
        return tableName;
    }

}
