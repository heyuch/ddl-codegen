package hyc.codegen.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 单个产物（artifact）的配置。
 * <p>
 * 对应 config 顶层键段：第一段 = 产物名，其余属性在此（如 {@code entity.generator=pojo}）。
 * 产物名自由定义；{@code generator} 引用注册的生成器；配置了即启用。
 * module 为空表示项目根。
 */
public final class ArtifactConfig {

    private final String name;

    private @Nullable String generator;

    private @Nullable String module;

    private @Nullable String pkg;

    private String suffix = "";

    private @Nullable String path;

    private @Nullable String source;

    private @Nullable String target;

    private final Map<String, String> options = new LinkedHashMap<>();

    public ArtifactConfig(String name) {
        this.name = name;
    }

    /** 注册的生成器名（如 pojo/converter/mybatisMapper）。 */
    public @Nullable String getGenerator() {
        return generator;
    }

    /** 所属模块 = 项目根下的一级子目录；空表示项目根。 */
    public @Nullable String getModule() {
        return module;
    }

    /** 产物名（config 顶层键第一段）。 */
    public String getName() {
        return name;
    }

    /** 取一个额外选项；未配置返回 null。 */
    public @Nullable String getOption(String key) {
        return options.get(key);
    }

    /** 该产物的其余任意键值。 */
    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(options);
    }

    /** 资源路径（XML 用，相对 module）；Java 类产物走 package。 */
    public @Nullable String getPath() {
        return path;
    }

    /** Java 包名；XML 类产物可省略（用 path）。 */
    public @Nullable String getPkg() {
        return pkg;
    }

    /** 源产物引用（converter 用，如 source=po）。 */
    public @Nullable String getSource() {
        return source;
    }

    /** 类名后缀（user → UserMapper 的 Mapper）。 */
    public String getSuffix() {
        return suffix;
    }

    /** 目标产物引用（converter/mapper/repository 的返回类型，如 target=entity）。 */
    public @Nullable String getTarget() {
        return target;
    }

    /** 是否 Java 类产物（package 路径），而非资源文件（path）。 */
    public boolean isJavaArtifact() {
        return pkg != null && !pkg.isEmpty();
    }

    public void putOption(String key, String value) {
        options.put(key, value);
    }

    public void setGenerator(@Nullable String generator) {
        this.generator = generator;
    }

    public void setModule(@Nullable String module) {
        this.module = module;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    public void setPkg(@Nullable String pkg) {
        this.pkg = pkg;
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    public void setSuffix(@Nullable String suffix) {
        this.suffix = suffix == null ? "" : suffix;
    }

    public void setTarget(@Nullable String target) {
        this.target = target;
    }

}
