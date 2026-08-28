package hyc.codegen.core.gen;

/**
 * artifact 生成器 SPI：一个生成器负责一种 artifact（entity/pojo/mapper/...）。
 * <p>
 * 注册方式：{@link CodeGenerator} 构造时传入；启用与否由 config {@code artifacts.<kind>} 决定
 * （存在即启用，与生成器是否注册无关）。{@link AbstractJavaArtifactGenerator} 提供完整的
 * 定位/解析/成员级 reconcile/打印/写盘管线，绝大多数生成器继承它并只写成员构建逻辑。
 */
public interface ArtifactGenerator {

    /** artifact 类型名，对应 config {@code artifacts.<kind>}。 */
    String kind();

    /** 生成/更新该表该 artifact 的文件（Java 类走 {@link AbstractJavaArtifactGenerator}，XML 等走自有实现）。 */
    void generate(TableContext ctx, GenerationContext gctx);

}
