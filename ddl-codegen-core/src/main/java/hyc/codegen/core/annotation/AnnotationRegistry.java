package hyc.codegen.core.annotation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * 注解处理器注册表：按注解名查找处理器。
 * <p>
 * {@link #builtin()} 注册内置的 type/as/ignore；用户自定义处理器通过 {@link #register} 加入。
 */
public final class AnnotationRegistry {

    private final Map<String, DdlAnnotationHandler> handlers = new LinkedHashMap<>();

    private AnnotationRegistry() {}

    /** 创建含全部内置处理器的注册表。 */
    public static AnnotationRegistry builtin() {
        AnnotationRegistry registry = new AnnotationRegistry();
        registry.register(new TypeHandler());
        registry.register(new AsHandler());
        registry.register(new IgnoreHandler());
        return registry;
    }

    /** 注册处理器；同名后注册覆盖先注册。 */
    public void register(DdlAnnotationHandler handler) {
        handlers.put(handler.name(), handler);
    }

    /** 按名查处理器；不存在时返回 {@code null}。 */
    @Nullable
    public DdlAnnotationHandler get(String name) {
        return handlers.get(name);
    }

    /** 已注册的注解名集合。 */
    public Set<String> names() {
        return handlers.keySet();
    }

}
