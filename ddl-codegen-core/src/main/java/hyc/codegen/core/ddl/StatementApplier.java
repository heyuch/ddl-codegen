package hyc.codegen.core.ddl;

import java.util.List;

import hyc.codegen.core.model.Schema;

/**
 * 把 {@link DdlOperation} 序列按顺序应用到 {@link Schema}，产出 {@link ApplyResult}。
 * <p>
 * 应用器只理解规范化操作，不感知 SQL 语法；变更顺序即语句顺序，
 * 因此同一批次内 create 后的 alter/drop 天然可见先前结果。
 */
public final class StatementApplier {

    /**
     * 应用一批操作。
     *
     * @param schema     目标模式（就地修改）
     * @param operations 按语句顺序的操作列表
     * 
     * @return 应用结果（受影响表、改名/删除记录）
     */
    public ApplyResult apply(Schema schema, List<DdlOperation> operations) {
        ApplyResult result = new ApplyResult();
        for (DdlOperation operation : operations) {
            applyOne(schema, operation, result);
        }
        return result;
    }

    private void applyOne(Schema schema, DdlOperation operation, ApplyResult result) {
        if (operation instanceof CreateTableOp) {
            schema.addTable(((CreateTableOp)operation).getTable());
            result.affect(operation.tableName());
        } else if (operation instanceof DropTableOp) {
            schema.removeTable(operation.tableName());
            result.affect(operation.tableName());
            result.dropped(operation.tableName());
        } else if (operation instanceof RenameTableOp) {
            RenameTableOp rename = (RenameTableOp)operation;
            schema.renameTable(rename.getFrom(), rename.getTo());
            result.affect(rename.getFrom());
            result.affect(rename.getTo());
            result.tableRenamed(rename.getFrom(), rename.getTo());
        } else if (operation instanceof AddColumnOp) {
            AddColumnOp add = (AddColumnOp)operation;
            if (schema.contains(add.tableName())) {
                schema.getTable(add.tableName()).addColumn(add.getColumn());
                result.affect(add.tableName());
            }
        } else if (operation instanceof DropColumnOp) {
            DropColumnOp drop = (DropColumnOp)operation;
            if (schema.contains(drop.tableName())) {
                schema.getTable(drop.tableName()).removeColumn(drop.getColumnName());
                result.affect(drop.tableName());
            }
        } else if (operation instanceof ChangeColumnOp) {
            ChangeColumnOp change = (ChangeColumnOp)operation;
            if (schema.contains(change.tableName())) {
                schema.getTable(change.tableName()).replaceColumn(change.getOldName(), change.getNewColumn());
                result.affect(change.tableName());
            }
        } else if (operation instanceof RenameColumnOp) {
            RenameColumnOp rename = (RenameColumnOp)operation;
            if (schema.contains(rename.tableName())) {
                schema.getTable(rename.tableName()).renameColumn(rename.getFrom(), rename.getTo());
                result.affect(rename.tableName());
                result.columnRenamed(rename.tableName(), rename.getFrom(), rename.getTo());
            }
        } else if (operation instanceof AddIndexOp) {
            AddIndexOp add = (AddIndexOp)operation;
            if (schema.contains(add.tableName())) {
                schema.getTable(add.tableName()).addIndex(add.getIndex());
                result.affect(add.tableName());
            }
        } else if (operation instanceof DropIndexOp) {
            DropIndexOp drop = (DropIndexOp)operation;
            if (schema.contains(drop.tableName())) {
                schema.getTable(drop.tableName()).removeIndex(drop.getIndexName());
                result.affect(drop.tableName());
            }
        } else if (operation instanceof RenameIndexOp) {
            RenameIndexOp rename = (RenameIndexOp)operation;
            if (schema.contains(rename.tableName())) {
                schema.getTable(rename.tableName()).renameIndex(rename.getFrom(), rename.getTo());
                result.affect(rename.tableName());
                result.indexRenamed(rename.tableName(), rename.getFrom(), rename.getTo());
            }
        } else {
            throw new IllegalArgumentException("未知 DDL 操作类型: " + operation.getClass().getName());
        }
    }

}
