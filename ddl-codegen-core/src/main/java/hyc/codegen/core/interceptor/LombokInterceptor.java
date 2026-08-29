package hyc.codegen.core.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import hyc.codegen.core.gen.ArtifactInterceptor;
import hyc.codegen.core.gen.TableContext;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;

/**
 * lombok 拦截器：给生成的类加 lombok 类级注解（默认 {@code @Data}）。
 * <p>
 * 可通过 artifact 配置的 {@code lombok} 选项指定注解列表（逗号分隔），如
 * {@code artifacts.entity.lombok=Data,Builder,NoArgsConstructor}；默认 {@code Data}。
 */
public final class LombokInterceptor implements ArtifactInterceptor {

    /** 拦截器名。 */
    public static final String NAME = "lombok";

    private static final String DEFAULT_ANNOTATIONS = "Data";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(Class cls, TableContext ctx) {
        String configured = ctx.getArtifactConfig().getOption("lombok");
        List<String> names = parse(configured == null ? DEFAULT_ANNOTATIONS : configured);
        if (names.isEmpty()) {
            return;
        }
        // 类可能没有 modifiers 容器（如纯 builder 构建）——先创建并放第一个注解
        if (cls.getModifiers() == null) {
            cls.addAnnotation(Annotation.of("lombok." + names.get(0)));
        }
        List<String> managed = new ArrayList<>();
        List<AnnotationTree> targets = new ArrayList<>();
        for (String name : names) {
            String fqn = "lombok." + name;
            managed.add(name);
            targets.add(Annotation.of(fqn));
        }
        InterceptorSupport.replaceAnnotations(cls.getModifiers(), managed, targets);
    }

    private static List<String> parse(String value) {
        List<String> result = new ArrayList<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

}
