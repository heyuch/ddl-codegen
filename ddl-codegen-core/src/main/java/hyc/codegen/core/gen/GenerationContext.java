package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.model.Table;
import hyc.codegen.core.naming.NamingService;
import hyc.codegen.core.types.TypeMapper;
import hyc.codegen.tree.Class;

/**
 * 一次代码生成执行的全局上下文：配置、共享服务、拦截器注册表、变更报告。
 */
public final class GenerationContext {

    private final DdlConfig config;

    private final Path projectRoot;

    private final NamingService naming;

    private final TypeMapper typeMapper;

    private final AnnotationRegistry annotationRegistry;

    private final ArtifactRegistry artifactRegistry;

    private final Map<String, ArtifactInterceptor> interceptors;

    private final ChangeReport report;

    private final List<String> warnings = new ArrayList<>();

    GenerationContext(DdlConfig config, NamingService naming, TypeMapper typeMapper,
            AnnotationRegistry annotationRegistry, ArtifactRegistry artifactRegistry,
            Map<String, ArtifactInterceptor> interceptors, ChangeReport report) {
        this.config = config;
        this.projectRoot = config.getRoot();
        this.naming = naming;
        this.typeMapper = typeMapper;
        this.annotationRegistry = annotationRegistry;
        this.artifactRegistry = artifactRegistry;
        this.interceptors = interceptors;
        this.report = report;
    }

    public DdlConfig getConfig() {
        return config;
    }

    /** 项目根（config 所在目录）。 */
    public Path getProjectRoot() {
        return projectRoot;
    }

    public NamingService getNaming() {
        return naming;
    }

    public TypeMapper getTypeMapper() {
        return typeMapper;
    }

    public AnnotationRegistry getAnnotationRegistry() {
        return annotationRegistry;
    }

    public ArtifactRegistry getArtifactRegistry() {
        return artifactRegistry;
    }

    public ChangeReport getReport() {
        return report;
    }

    /** 产物类的全限定名（包 + 类名；未启用报错）。 */
    public String artifactFqn(String tableName, String artifactName) {
        ArtifactConfig artifactConfig = config.artifact(artifactName)
                .orElseThrow(() -> new IllegalStateException("未配置产物: " + artifactName));
        return artifactConfig.getPkg() + "." + naming.artifactClassName(tableName, artifactName);
    }

    /**
     * 解析产物引用（设计见 docs/changes/2026-08-29-feat-parameterized-artifacts/design.md）：
     * 显式 {@code refKey}（source/target/任意选项）→ 该产物；缺省 → {@code defaultGenerator} 的唯一实例。
     * 无唯一实例/引用不存在 → 明确报错。
     */
    public ArtifactConfig resolveReference(String ownerName, String refKey, String defaultGenerator) {
        ArtifactConfig owner = config.artifact(ownerName)
                .orElseThrow(() -> new IllegalStateException("未配置产物: " + ownerName));
        String ref = refOf(owner, refKey);
        if (ref != null) {
            return config.artifact(ref)
                    .orElseThrow(() -> new IllegalStateException(
                            "产物 '" + ownerName + "' 的 " + refKey + " 引用了不存在的产物: " + ref));
        }
        List<ArtifactConfig> matches = new ArrayList<>();
        for (ArtifactConfig a : config.getArtifacts().values()) {
            if (defaultGenerator.equals(a.getGenerator())) {
                matches.add(a);
            }
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        throw new IllegalStateException("产物 '" + ownerName + "' 未配置 " + refKey + "，且生成器 '"
                + defaultGenerator + "' 的实例数 = " + matches.size() + "（多实例/无实例时必须显式配置 " + refKey + "）");
    }

    private String refOf(ArtifactConfig owner, String refKey) {
        if ("source".equals(refKey)) {
            return owner.getSource();
        }
        if ("target".equals(refKey)) {
            return owner.getTarget();
        }
        return owner.getOption(refKey);
    }

    /** enum 产物包（唯一 enum 实例；未配置返回 null，多实例报错）。 */
    @Nullable
    public String enumPackage() {
        List<ArtifactConfig> matches = new ArrayList<>();
        for (ArtifactConfig a : config.getArtifacts().values()) {
            if ("enum".equals(a.getGenerator())) {
                matches.add(a);
            }
        }
        if (matches.size() <= 1) {
            return matches.isEmpty() ? null : matches.get(0).getPkg();
        }
        throw new IllegalStateException("enum 生成器实例数 = " + matches.size() + "（应唯一，多个时需显式配置引用）");
    }

    /** 产物引用 → FQN。 */
    public String refFqn(String tableName, ArtifactConfig referenced) {
        return referenced.getPkg() + "." + naming.artifactClassName(tableName, referenced.getName());
    }

    /** 按产物配置创建表上下文。 */
    public TableContext tableContext(Table table, String artifactName) {
        ArtifactConfig artifactConfig = config.artifact(artifactName)
                .orElseThrow(() -> new IllegalStateException("未配置产物: " + artifactName));
        String enumPackage = uniqueEnumPackage(artifactName);
        return new TableContext(table, artifactConfig, naming, typeMapper, enumPackage, config.getNullableAnnotation());
    }

    /** use 含 enums 时解析 enum 产物包（唯一 enum 实例；缺省报错）。 */
    private String uniqueEnumPackage(String artifactName) {
        if (!usesEnums(artifactName)) {
            return null;
        }
        try {
            ArtifactConfig enumArtifact = resolveReference(artifactName, "enums", "enum");
            return enumArtifact.getPkg();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("产物 '" + artifactName + "' 的 use 含 enums，但未配置 enum 产物（generator=enum）", e);
        }
    }

    /** 产物 use 链是否含 enums（enum 列 → 枚举类视图信号）。 */
    public boolean usesEnums(String artifactName) {
        return config.artifact(artifactName)
                .map(a -> a.getUse().contains("enums"))
                .orElse(false);
    }

    /** 某 artifact 配置的拦截器链（按 use 顺序解析；未注册的名字记 warning 跳过）。 */
    public List<ArtifactInterceptor> interceptorsFor(String artifactKind) {
        List<ArtifactInterceptor> result = new ArrayList<>();
        ArtifactConfig artifactConfig = config.artifact(artifactKind).orElse(null);
        if (artifactConfig == null) {
            return result;
        }
        for (String name : artifactConfig.getUse()) {
            if ("enums".equals(name)) {
                // 视图信号（typeOf 按 use 决定），非装饰拦截器
                continue;
            }
            ArtifactInterceptor interceptor = interceptors.get(name);
            if (interceptor == null) {
                warning("artifact '" + artifactKind + "' use 引用了未注册的拦截器: " + name);
            } else {
                result.add(interceptor);
            }
        }
        return result;
    }

    /** 对生成的类执行某 artifact 的拦截器链（只动 @Generated 成员与 import）。 */
    public void applyInterceptors(Class cls, TableContext ctx) {
        for (ArtifactInterceptor interceptor : interceptorsFor(ctx.getArtifactName())) {
            interceptor.apply(cls, ctx);
        }
    }

    /** 记一条警告（不中断生成）。 */
    public void warning(String message) {
        warnings.add(message);
    }

    /** 本次执行的全部警告。 */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /** 构造器：由 {@link CodeGenerator} 使用。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 构造器。 */
    public static final class Builder {

        private final Map<String, ArtifactInterceptor> interceptors = new LinkedHashMap<>();

        private DdlConfig config;

        private NamingService naming;

        private TypeMapper typeMapper;

        private AnnotationRegistry annotationRegistry;

        private ArtifactRegistry artifactRegistry = new ArtifactRegistry();

        private ChangeReport report = new ChangeReport();

        public Builder config(DdlConfig config) {
            this.config = config;
            return this;
        }

        public Builder naming(NamingService naming) {
            this.naming = naming;
            return this;
        }

        public Builder typeMapper(TypeMapper typeMapper) {
            this.typeMapper = typeMapper;
            return this;
        }

        public Builder annotationRegistry(AnnotationRegistry annotationRegistry) {
            this.annotationRegistry = annotationRegistry;
            return this;
        }

        public Builder artifactRegistry(ArtifactRegistry artifactRegistry) {
            this.artifactRegistry = artifactRegistry;
            return this;
        }

        public Builder report(ChangeReport report) {
            this.report = report;
            return this;
        }

        public Builder interceptor(ArtifactInterceptor interceptor) {
            interceptors.put(interceptor.name(), interceptor);
            return this;
        }

        public GenerationContext build() {
            return new GenerationContext(config, naming, typeMapper,
                    annotationRegistry, artifactRegistry, interceptors, report);
        }

    }

}
