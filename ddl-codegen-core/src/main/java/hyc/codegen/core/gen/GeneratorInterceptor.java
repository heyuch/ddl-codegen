package hyc.codegen.core.gen;

import com.sun.source.tree.VariableTree;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.Variable;

/**
 * artifact 拦截器 SPI：在生成器产出类之后做 AST 装饰（如 lombok 类级注解、jsr303 字段约束）。
 * <p>
 * 契约（DESIGN §11）：只允许动 {@code @Generated} 成员与 import；幂等（先清理自身管理的注解再按模型重算）；
 * 写盘前字节比对兜底。按 artifact 配置 {@code use=} 引用，顺序执行。
 */
public interface GeneratorInterceptor {

    /** 拦截器名（config {@code use} 里引用）。 */
    String name();

    /**
     * 装饰生成的类。
     * <p>
     * 默认实现：遍历 {@code @Generated} 字段，逐个回调 {@link #onField}（字段级拦截器只需实现
     * {@link #onField}，无需重复"找列/过滤生成成员"样板）；类级拦截器（如 lombok）覆盖本方法。
     *
     * @param cls 生成的类（可能来自 reconcile 后的现有类）
     * @param ctx 表上下文
     */
    default void apply(Class cls, TableContext ctx) {
        for (Variable field : cls.getFields()) {
            if (!GeneratedSupport.isGenerated(field)) {
                continue;
            }
            Column column = ctx.findColumn(field.getName().toString());
            if (column != null) {
                onField(field, column, ctx);
            }
        }
        for (Method method : cls.getMethods()) {
            if (!GeneratedSupport.isGenerated(method)) {
                continue;
            }
            for (VariableTree param : method.getParameters()) {
                Column column = ctx.findColumn(param.getName().toString());
                if (column != null) {
                    onParam(param, column, method, ctx);
                }
            }
        }
    }

    /**
     * 字段级钩子（默认无操作）：仅对 {@code @Generated} 字段调用。
     *
     * @param field  生成的字段
     * @param column 字段对应的列（按字段名匹配；匹配不到不调用）
     * @param ctx    表上下文
     */
    default void onField(Variable field, Column column, TableContext ctx) {}

    /**
     * 方法参数级钩子（默认无操作）：仅对 {@code @Generated} 方法的参数调用。
     *
     * @param param  方法参数（类型改写与字段同一套机制）
     * @param column 参数对应的列（按参数名匹配；匹配不到不调用）
     * @param method 所属方法
     * @param ctx    表上下文
     */
    default void onParam(VariableTree param, Column column, Method method, TableContext ctx) {}

}
