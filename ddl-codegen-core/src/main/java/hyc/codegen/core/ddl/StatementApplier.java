package hyc.codegen.core.ddl;

import java.util.List;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;

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
        ApplyResult result = applyOps(schema, operations);
        pruneIgnored(schema);
        return result;
    }

    private ApplyResult applyOps(Schema schema, List<DdlOperation> operations) {
        ApplyResult result = new ApplyResult();
        for (DdlOperation operation : operations) {
            applyOne(schema, operation, result);
        }
        return result;
    }

    private void applyOne(Schema schema, DdlOperation operation, ApplyResult result) {
        // 多态分发：每种操作实现自己的 apply（见 DdlOperation#apply），此处无需理解具体类型
        operation.apply(schema, result);
    }

    /**
     * 模型级语义：{@code @ignore} 注解的列/索引在解析应用后从模型移除
     * （一处解决所有产物含 XML；自定义生成器也不再见到被忽略的成员）。
     */
    private static void pruneIgnored(Schema schema) {
        for (Table table : schema.tables()) {
            for (Column column : new java.util.ArrayList<>(table.getColumns())) {
                if (column.getMeta().get("ignore") != null) {
                    table.removeColumn(column.getName());
                }
            }
            for (Index index : new java.util.ArrayList<>(table.getIndexes())) {
                if (index.getMeta().get("ignore") != null) {
                    table.removeIndex(index.getName());
                }
            }
        }
    }

}
