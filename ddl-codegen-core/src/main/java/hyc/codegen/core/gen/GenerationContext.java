package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.model.Table;
import hyc.codegen.core.naming.NamingService;
import hyc.codegen.core.types.TypeMapper;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 一次代码生成执行的全局上下文：配置、共享服务、拦截器注册表、变更报告。
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "生成上下文持 config 与 report（写入通道）")
public final class GenerationContext {

    private final DdlConfig config;

    private final Path projectRoot;

    private final NamingService naming;

    private final TypeMapper typeMapper;

    private final AnnotationRegistry annotationRegistry;

    private final ArtifactRegistry artifactRegistry;

    private final Map<String, ArtifactGenerator> generators;

    private final ChangeReport report;

    private final List<String> warnings = new ArrayList<>();

    GenerationContext(DdlConfig config, NamingService naming, TypeMapper typeMapper,
            AnnotationRegistry annotationRegistry, ArtifactRegistry artifactRegistry,
            Map<String, ArtifactGenerator> generators, ChangeReport report) {
        this.config = config;
        this.projectRoot = config.getRoot();
        this.naming = naming;
        this.typeMapper = typeMapper;
        this.annotationRegistry = annotationRegistry;
        this.artifactRegistry = artifactRegistry;
        this.generators = generators;
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

    /** 按产物名取配置；未配置时明确报错（生成流程的配置契约）。 */
    private ArtifactConfig requireArtifact(String artifactName) {
        ArtifactConfig artifactConfig = config.artifact(artifactName);
        if (artifactConfig == null) {
            throw new IllegalStateException("未配置产物: " + artifactName);
        }
        return artifactConfig;
    }

    /** 产物对应的生成器实例（config.generator → 注册表）。 */
    public ArtifactGenerator generatorFor(String artifactName) {
        ArtifactConfig artifactConfig = requireArtifact(artifactName);
        String generatorName = artifactConfig.getGenerator();
        if (generatorName == null) {
            throw new IllegalStateException("产物 '" + artifactName + "' 未配置 generator");
        }
        ArtifactGenerator generator = generators.get(generatorName);
        if (generator == null) {
            throw new IllegalStateException("产物 '" + artifactName + "' 引用了未注册的生成器: " + generatorName);
        }
        return generator;
    }

    /** enum 产物包（enums 特性开启时：唯一 enum 生成器产物，缺省/多实例报错）。 */
    public @Nullable String enumPackageFor(String artifactName) {
        if (!usesEnums(artifactName)) {
            return null;
        }
        List<ArtifactConfig> matches = new ArrayList<>();
        for (ArtifactConfig artifact : config.getArtifacts().values()) {
            if ("enum".equals(artifact.getGenerator())) {
                matches.add(artifact);
            }
        }
        if (matches.size() != 1) {
            throw new IllegalStateException("产物 '" + artifactName + "' 的 enums 特性需要 enum 产物"
                    + "（generator=enum 应唯一，当前 " + matches.size() + " 个）");
        }
        return matches.get(0).getPkg();
    }

    /** 产物 enums 特性开关。 */
    public boolean usesEnums(String artifactName) {
        ArtifactConfig artifactConfig = config.artifact(artifactName);
        return artifactConfig != null && Boolean.parseBoolean(artifactConfig.getOption("enums"));
    }

    /** 产物类的全限定名（包 + 类名；未启用报错）。 */
    public String artifactFqn(String tableName, String artifactName) {
        ArtifactConfig artifactConfig = requireArtifact(artifactName);
        String pkg = artifactConfig.getPkg();
        if (pkg == null) {
            throw new IllegalStateException(
                    "产物 '" + artifactName + "' 缺少 package 配置（Java 类产物必须配置 package）");
        }
        return pkg + "." + naming.artifactClassName(tableName, artifactName);
    }

    /**
     * 解析产物引用（设计见 docs/changes/2026-08-29-feat-parameterized-artifacts/design.md）：
     * 显式 {@code refKey}（source/target/任意选项）→ 该产物；缺省 → {@code defaultGenerator} 的唯一实例。
     * 无唯一实例/引用不存在 → 明确报错。
     */
    public ArtifactConfig resolveReference(String ownerName, String refKey, String defaultGenerator) {
        ArtifactConfig owner = requireArtifact(ownerName);
        String ref = refOf(owner, refKey);
        if (ref != null) {
            return requireArtifact(ref);
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

    private @Nullable String refOf(ArtifactConfig owner, String refKey) {
        if ("source".equals(refKey)) {
            return owner.getSource();
        }
        if ("target".equals(refKey)) {
            return owner.getTarget();
        }
        return owner.getOption(refKey);
    }

    /** enum 产物包（唯一 enum 实例；未配置返回 null，多实例报错）。 */
    public @Nullable String enumPackage() {
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

    /** 产物引用 → FQN（查询契约：路由到引用产物的生成器 className）。 */
    public String refFqn(String tableName, ArtifactConfig referenced) {
        TableContext refCtx = tableContext(syntheticTable(tableName, referenced), referenced.getName());
        return referenced.getPkg() + "." + generatorFor(referenced.getName()).className(refCtx);
    }

    private Table syntheticTable(String tableName, ArtifactConfig referenced) {
        return new Table(tableName, null);
    }

    /** 按产物配置创建表上下文。 */
    public TableContext tableContext(Table table, String artifactName) {
        ArtifactConfig artifactConfig = requireArtifact(artifactName);
        return new TableContext(table, artifactConfig, this);
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

    /**
     * 构造器。
     * 可修改构建对象：字段由 builder 方法在 build() 前设置，初始化时序检查不适用。
     */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "builder 模式持有待构建对象")
    public static final class Builder {

        private final Map<String, ArtifactGenerator> generators = new LinkedHashMap<>();

        @MonotonicNonNull
        private DdlConfig config;

        @MonotonicNonNull
        private NamingService naming;

        @MonotonicNonNull
        private TypeMapper typeMapper;

        @MonotonicNonNull
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

        public Builder generator(ArtifactGenerator generator) {
            generators.put(generator.kind(), generator);
            return this;
        }

        public GenerationContext build() {
            DdlConfig c = config;
            if (c == null) {
                throw new IllegalStateException("GenerationContext 构建缺失必填字段 config");
            }
            NamingService n = naming;
            if (n == null) {
                throw new IllegalStateException("GenerationContext 构建缺失必填字段 naming");
            }
            TypeMapper tm = typeMapper;
            if (tm == null) {
                throw new IllegalStateException("GenerationContext 构建缺失必填字段 typeMapper");
            }
            AnnotationRegistry ar = annotationRegistry;
            if (ar == null) {
                throw new IllegalStateException("GenerationContext 构建缺失必填字段 annotationRegistry");
            }
            return new GenerationContext(c, n, tm, ar, artifactRegistry, generators, report);
        }

    }

}
