package hyc.codegen.core.annotation;

import java.util.EnumSet;
import java.util.Set;

import hyc.codegen.core.model.Meta;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 内置注解 {@code @as}：生成类命名覆盖。
 * <p>
 * 列注释：指定生成该列对应类（如 enum 类）时的类名；
 * 表注释：指定该类所有 artifact 的基类名。
 * 解析结果写入 meta 的 {@code "as"} 键。
 */
public final class AsHandler implements DdlAnnotationHandler {

    /** 注解名。 */
    public static final String NAME = "as";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Set<MetaTarget> targets() {
        return EnumSet.of(MetaTarget.TABLE, MetaTarget.COLUMN);
    }

    @Override
    public void parse(Meta meta, @Nullable String value) {
        meta.put(NAME, value);
    }

}
