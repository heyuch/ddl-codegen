package hyc.codegen.core.annotation;

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;

import hyc.codegen.core.model.Meta;

/**
 * 内置注解 {@code @type}：列类型覆盖。
 * <p>
 * 仅允许出现在列注释；用在表/索引注释时记 warning 忽略。
 * 解析结果写入列 meta 的 {@code "type"} 键（类型字符串，FQN 或简单名），
 * 由类型映射阶段优先采用（复用已有类型，不生成、不校验存在）。
 */
public final class TypeHandler implements DdlAnnotationHandler {

    /** 注解名。 */
    public static final String NAME = "type";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Set<MetaTarget> targets() {
        return EnumSet.of(MetaTarget.COLUMN);
    }

    @Override
    public void parse(Meta meta, @Nullable String value) {
        meta.put(NAME, value);
    }

}
