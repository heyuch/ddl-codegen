package hyc.codegen.core.annotation;

import java.util.EnumSet;
import java.util.Set;

import hyc.codegen.core.model.Meta;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 内置注解 {@code @enum}：列按枚举处理。
 * <p>
 * 仅允许出现在列注释：{@code @enum:Status} 值 = 枚举类名（裸 {@code @enum} 无值 → 按命名策略）。
 * 解析结果写入列 meta 的 {@code "enum"} 键：有值存字符串，无值存 {@code Boolean.TRUE} 占位
 * （{@link Meta#put} 对 null 是清除语义，裸注解必须显式占位，否则整列静默退化为普通列）。
 * 枚举项列表由消费方从列 comment 原文解析（见 {@code model.EnumCommentParser}）。
 */
public final class EnumHandler implements DdlAnnotationHandler {

    /** 注解名。 */
    public static final String NAME = "enum";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void parse(Meta meta, @Nullable String value) {
        meta.put(NAME, value != null ? value : Boolean.TRUE);
    }

    @Override
    public Set<MetaTarget> targets() {
        return EnumSet.of(MetaTarget.COLUMN);
    }

}
