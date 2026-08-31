package hyc.codegen.core.ddl;

import hyc.codegen.core.model.Schema;

/**
 * 规范化后的 DDL 模型操作：由 {@link DdlParser} 产出、被 {@link StatementApplier} 应用到
 * {@link hyc.codegen.core.model.Schema}。
 * <p>
 * 每种操作对应一种原子变更（建表/删表/改名/加列/改列/删列/加索引/删索引/索引改名），
 * 不保留 SQL 语法形态——应用器无需理解 Druid AST。
 */
public interface DdlOperation {

    /**
     * 应用到 Schema（就地修改）并记录 {@link ApplyResult}。
     * <p>
     * 多态分发：每种操作知道自己的应用语义（OCP——新增操作类型只需新类实现本方法，
     * 应用器无需修改）。列/索引级操作在目标表不存在时静默跳过。
     *
     * @param schema 目标模式（就地修改）
     * @param result 变更记录（受影响表、改名/删除记录）
     */
    void apply(Schema schema, ApplyResult result);

    /** 操作涉及的表名；rename 类操作为旧表名。 */
    String tableName();

}
