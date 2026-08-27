package hyc.codegen.core.ddl;

/**
 * 规范化后的 DDL 模型操作：由 {@link DdlParser} 产出、被 {@link StatementApplier} 应用到
 * {@link hyc.codegen.core.model.Schema}。
 * <p>
 * 每种操作对应一种原子变更（建表/删表/改名/加列/改列/删列/加索引/删索引/索引改名），
 * 不保留 SQL 语法形态——应用器无需理解 Druid AST。
 */
public interface DdlOperation {

    /** 操作涉及的表名；rename 类操作为旧表名。 */
    String tableName();

}
