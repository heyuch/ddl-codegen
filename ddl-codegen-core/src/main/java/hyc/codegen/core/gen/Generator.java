package hyc.codegen.core.gen;

import hyc.codegen.core.model.Column;

/**
 * 生成器 SPI（唯一扩展点）：产物 = 生成器的具名实例 + 配置选项。
 * <p>
 * 查询契约（跨产物引用的准确值来源）：每个生成器实现自己的 {@code className/fieldName/fieldType}；
 * 用不上的方法直接抛 {@link UnsupportedOperationException}（如 XML 生成器无 Java 类/字段）。
 * 基类 {@link AbstractJavaArtifactGenerator} 提供命名/类型映射的框架默认实现。
 */
public interface Generator {

    /** 生成器注册名（config {@code generator=<名>} 引用）。 */
    String kind();

    /** 生成/更新该表该产物的文件。 */
    void generate(TableContext ctx, GenerationContext gctx);

    /** 查询契约：类名（文件路径与跨产物引用使用）。 */
    String className(TableContext ctx);

    /** 查询契约：列名 → 字段名（本生成器视角，特殊命名逻辑在此实现）。 */
    String fieldName(Column column, TableContext ctx);

    /** 查询契约：列 → 成员类型（本生成器视角，特性/特殊类型逻辑在此实现）。 */
    String fieldType(Column column, TableContext ctx);

}
