package hyc.codegen.core.interceptor;

import java.util.Collections;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import hyc.codegen.core.gen.GeneratorInterceptor;
import hyc.codegen.core.gen.TableContext;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Variable;

/**
 * jsr305 拦截器：nullable 列的 {@code @Generated} 字段加 {@code @Nullable}
 * （与 jsr303 的 {@code @NotNull} 互补），幂等重算。字段级拦截器（见 SPI 默认 apply）。
 */
public final class Jsr305Interceptor implements GeneratorInterceptor {

    /** 拦截器名。 */
    public static final String NAME = "jsr305";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void onField(Variable field, Column column, TableContext ctx) {
        List<String> managed = InterceptorSupport.managed("Nullable");
        List<AnnotationTree> targets = column.isNullable()
                ? Collections.singletonList(Annotation.of(ctx.getNullableAnnotation()))
                : Collections.emptyList();
        InterceptorSupport.replaceAnnotations(field, managed, targets);
    }

}
