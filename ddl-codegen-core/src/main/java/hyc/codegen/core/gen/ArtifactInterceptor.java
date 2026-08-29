package hyc.codegen.core.gen;

import hyc.codegen.tree.Class;

/**
 * artifact 拦截器 SPI：在生成器产出类之后做 AST 装饰（如 lombok 类级注解、jsr303 字段约束）。
 * <p>
 * 契约（DESIGN §11）：只允许动 {@code @Generated} 成员与 import；幂等（先清理自身管理的注解再按模型重算）；
 * 写盘前字节比对兜底。按 artifact 配置 {@code use=} 引用，顺序执行。
 */
public interface ArtifactInterceptor {

    /** 拦截器名（config {@code use} 里引用）。 */
    String name();

    /**
     * 装饰生成的类。
     *
     * @param cls 生成的类（可能来自 reconcile 后的现有类）
     * @param ctx 表上下文
     */
    void apply(Class cls, TableContext ctx);

}
