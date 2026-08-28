package hyc.codegen.core.gen;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;

/**
 * 模型元数据便捷判断（DDL 注解结果，见 DESIGN §9）。
 */
public final class MetaSupport {

    /** {@code @ignore} 元数据键。 */
    public static final String IGNORE = "ignore";

    /** {@code @type} 元数据键。 */
    public static final String TYPE = "type";

    private MetaSupport() {
        throw new AssertionError("no instances");
    }

    /** 列是否被 {@code @ignore}（所有 artifact 跳过该字段）。 */
    public static boolean isIgnored(Column column) {
        return column.getMeta().get(IGNORE) != null;
    }

    /** 索引是否被 {@code @ignore}（不生成查询方法）。 */
    public static boolean isIgnored(Index index) {
        return index.getMeta().get(IGNORE) != null;
    }

    /** 列是否有 {@code @type} 覆盖。 */
    public static boolean hasTypeOverride(Column column) {
        return column.getMeta().get(TYPE) != null;
    }

}
