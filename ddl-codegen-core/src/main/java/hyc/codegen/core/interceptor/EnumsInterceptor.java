package hyc.codegen.core.interceptor;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import hyc.codegen.core.gen.GeneratorInterceptor;
import hyc.codegen.core.gen.TableContext;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Variable;

/**
 * enums 拦截器：实体视图的类型改写（字段与方法参数同机制，见 SPI 默认 apply）。
 * <p>
 * use 含 {@code enums} 的产物上：{@code @type} 注解覆盖优先；enum 列的类型 String → 枚举类 FQN
 * （enum 产物 pkg + naming.enumClassName，含 {@code @as} 覆盖）。非 enum/无 @type 的列不触碰。
 */
public final class EnumsInterceptor implements GeneratorInterceptor {

    /** 拦截器名。 */
    public static final String NAME = "enums";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void onField(Variable field, Column column, TableContext ctx) {
        Tree type = typeTree(column, ctx);
        if (type != null) {
            field.setType(type);
        }
    }

    @Override
    public void onParam(VariableTree param, Column column, Method method, TableContext ctx) {
        Tree type = typeTree(column, ctx);
        if (type != null && param instanceof Variable) {
            ((Variable)param).setType(type);
        }
    }

    /** 目标类型：@type 覆盖 > enum 列 → 枚举类 FQN；否则 null（不改写）。 */
    private Tree typeTree(Column column, TableContext ctx) {
        Object type = column.getMeta().get("type");
        if (type != null) {
            return new TypeReference(type.toString());
        }
        if (!column.getEnumValues().isEmpty()) {
            String enumFqn = ctx.getEnumPackage() + "." + ctx.enumClassName(column);
            return new TypeReference(enumFqn);
        }
        return null;
    }

}
