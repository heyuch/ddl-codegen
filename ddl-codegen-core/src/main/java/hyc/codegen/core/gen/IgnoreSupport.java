package hyc.codegen.core.gen;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;

/**
 * DDL 注解 {@code @ignore} 的元数据读取（位于 gen：生成器在构建成员时跳过被忽略的列/索引；
 * 拦截器不处理忽略成员——被忽略的列不生成字段、被忽略的索引不生成方法，天然不会到达拦截器）。
 */
public final class IgnoreSupport {

    /** {@code @ignore} 元数据键。 */
    public static final String IGNORE = "ignore";

    private IgnoreSupport() {
        throw new AssertionError("no instances");
    }

    /** 列是否被 {@code @ignore}（所有产物跳过该字段）。 */
    public static boolean isIgnored(Column column) {
        return column.getMeta().get(IGNORE) != null;
    }

    /** 索引是否被 {@code @ignore}（不生成查询方法）。 */
    public static boolean isIgnored(Index index) {
        return index.getMeta().get(IGNORE) != null;
    }

}
