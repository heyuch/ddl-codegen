package hyc.codegen.core.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import hyc.codegen.core.gen.ArtifactInterceptor;
import hyc.codegen.core.gen.TableContext;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Variable;

/**
 * jsr303 拦截器：按列的 DDL 约束为 {@code @Generated} 字段添加校验注解（只映射真实存在的约束，不猜语义）：
 * <ul>
 * <li>{@code NOT NULL} → {@code @NotNull}</li>
 * <li>{@code varchar(n)/char(n)} → {@code @Size(max=n)}</li>
 * <li>{@code decimal(p,s)} → {@code @Digits(integer=p-s, fraction=s)}</li>
 * </ul>
 * 管理 {@code javax.validation.constraints.*} 的这三个注解，幂等重算。字段级拦截器（见 SPI 默认 apply）。
 */
public final class Jsr303Interceptor implements ArtifactInterceptor {

    /** 拦截器名。 */
    public static final String NAME = "jsr303";

    private static final String PACKAGE = "javax.validation.constraints";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void onField(Variable field, Column column, TableContext ctx) {
        List<String> managed = InterceptorSupport.managed("NotNull", "Size", "Digits");
        List<AnnotationTree> targets = buildTargets(column);
        InterceptorSupport.replaceAnnotations(field, managed, targets);
    }

    private List<AnnotationTree> buildTargets(Column column) {
        List<AnnotationTree> targets = new ArrayList<>();
        if (!column.isNullable()) {
            targets.add(Annotation.of(PACKAGE + ".NotNull"));
        }
        String sqlType = column.getSqlType();
        if (("varchar".equals(sqlType) || "char".equals(sqlType)) && column.getLength() > 0) {
            targets.add(Annotation.of(PACKAGE + ".Size", "max = " + column.getLength()));
        }
        if ("decimal".equals(sqlType) && column.getPrecision() > 0) {
            int integer = column.getPrecision() - column.getScale();
            targets.add(Annotation.of(PACKAGE + ".Digits",
                    "integer = " + integer + ", fraction = " + column.getScale()));
        }
        return targets;
    }

}
