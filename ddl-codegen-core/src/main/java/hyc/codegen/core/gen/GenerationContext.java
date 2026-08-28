package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.interceptor.ArtifactInterceptor;
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

    /** 按 artifact 配置创建表上下文。 */
    public TableContext tableContext(Table table, String artifactKind) {
        ArtifactConfig artifactConfig = config.artifact(artifactKind)
                .orElseThrow(() -> new IllegalStateException("未启用 artifact: " + artifactKind));
        return new TableContext(table, artifactKind, artifactConfig, naming, typeMapper);
    }

    /** 某 artifact 配置的拦截器链（按 use 顺序解析；未注册的名字记 warning 跳过）。 */
    public List<ArtifactInterceptor> interceptorsFor(String artifactKind) {
        List<ArtifactInterceptor> result = new ArrayList<>();
        ArtifactConfig artifactConfig = config.artifact(artifactKind).orElse(null);
        if (artifactConfig == null) {
            return result;
        }
        for (String name : artifactConfig.getUse()) {
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
        for (ArtifactInterceptor interceptor : interceptorsFor(ctx.getArtifactKind())) {
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
