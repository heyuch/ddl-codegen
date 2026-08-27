package hyc.codegen.core.annotation;

import java.util.EnumSet;
import java.util.Set;

import hyc.codegen.core.model.Meta;

/**
 * 内置注解 {@code @ignore}：跳过生成。
 * <p>
 * 列注释：所有 artifact 跳过该字段（含 XML resultMap/insert/update 片段）；
 * 索引注释：不生成该索引对应的查询方法（XML 也不生成对应 select）。
 * 解析结果写入 meta 的 {@code "ignore"} 键（布尔真）。
 */
public final class IgnoreHandler implements DdlAnnotationHandler {

    /** 注解名。 */
    public static final String NAME = "ignore";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Set<MetaTarget> targets() {
        return EnumSet.of(MetaTarget.COLUMN, MetaTarget.INDEX);
    }

    @Override
    public void parse(Meta meta, String value) {
        meta.put(NAME, Boolean.TRUE);
    }

}
