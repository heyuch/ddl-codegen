package hyc.codegen.core.interceptor;

import java.util.Collections;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import hyc.codegen.core.gen.ArtifactInterceptor;
import hyc.codegen.core.gen.GeneratedSupport;
import hyc.codegen.core.gen.TableContext;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Variable;

/**
 * jsr305 拦截器：nullable 列的 {@code @Generated} 字段加 {@code @Nullable}
 * （与 jsr303 的 {@code @NotNull} 互补），幂等重算。
 */
public final class Jsr305Interceptor implements ArtifactInterceptor {

    /** 拦截器名。 */
    public static final String NAME = "jsr305";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(Class cls, TableContext ctx) {
        for (Variable field : cls.getFields()) {
            if (!GeneratedSupport.isGenerated(field)) {
                continue;
            }
            Column column = findColumn(ctx, field.getName().toString());
            if (column == null) {
                continue;
            }
            List<String> managed = InterceptorSupport.managed("Nullable");
            if (column.isNullable()) {
                List<AnnotationTree> targets = Collections.singletonList(
                        Annotation.of(ctx.getNullableAnnotation()));
                InterceptorSupport.replaceAnnotations(field, managed, targets);
            } else {
                InterceptorSupport.replaceAnnotations(field, managed, Collections.emptyList());
            }
        }
    }

    private Column findColumn(TableContext ctx, String fieldName) {
        for (Column column : ctx.columns()) {
            if (ctx.fieldName(column).equals(fieldName)) {
                return column;
            }
        }
        return null;
    }

}
