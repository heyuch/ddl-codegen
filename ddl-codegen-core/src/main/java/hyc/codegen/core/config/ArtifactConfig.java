package hyc.codegen.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * 单个产物（artifact）的配置。
 * <p>
 * 对应 config 顶层键段：第一段 = 产物名，其余属性在此（如 {@code entity.generator=pojo}）。
 * 产物名自由定义；{@code generator} 引用注册的生成器；配置了即启用。
 * module 为空表示项目根。
 */
public final class ArtifactConfig {

    private final String name;

    @Nullable
    private String generator;

    @Nullable
    private String module;

    @Nullable
    private String pkg;

    private String suffix = "";

    @Nullable
    private String path;

    @Nullable
    private String source;

    @Nullable
    private String target;

    private final List<String> use = new ArrayList<>();

    private final Map<String, String> options = new LinkedHashMap<>();

    public ArtifactConfig(String name) {
        this.name = name;
    }

    /** 产物名（config 顶层键第一段）。 */
    public String getName() {
        return name;
    }

    /** 注册的生成器名（如 pojo/converter/mybatisMapper）。 */
    @Nullable
    public String getGenerator() {
        return generator;
    }

    public void setGenerator(@Nullable String generator) {
        this.generator = generator;
    }

    /** 所属模块 = 项目根下的一级子目录；空表示项目根。 */
    @Nullable
    public String getModule() {
        return module;
    }

    public void setModule(@Nullable String module) {
        this.module = module;
    }

    /** Java 包名；XML 类产物可省略（用 path）。 */
    @Nullable
    public String getPkg() {
        return pkg;
    }

    public void setPkg(@Nullable String pkg) {
        this.pkg = pkg;
    }

    /** 类名后缀（user → UserMapper 的 Mapper）。 */
    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(@Nullable String suffix) {
        this.suffix = suffix == null ? "" : suffix;
    }

    /** 资源路径（XML 用，相对 module）；Java 类产物走 package。 */
    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    /** 源产物引用（converter 用，如 source=po）。 */
    @Nullable
    public String getSource() {
        return source;
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    /** 目标产物引用（converter/mapper/repository 的返回类型，如 target=entity）。 */
    @Nullable
    public String getTarget() {
        return target;
    }

    public void setTarget(@Nullable String target) {
        this.target = target;
    }

    /** 拦截器链（{@code use=lombok,jsr303,enums}），按序执行/生效。 */
    public List<String> getUse() {
        return new ArrayList<>(use);
    }

    public void setUse(List<String> use) {
        this.use.clear();
        this.use.addAll(use);
    }

    /** 该产物的其余任意键值。 */
    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(options);
    }

    /** 取一个额外选项；未配置返回 null。 */
    @Nullable
    public String getOption(String key) {
        return options.get(key);
    }

    public void putOption(String key, String value) {
        options.put(key, value);
    }

    /** 是否 Java 类产物（package 路径），而非资源文件（path）。 */
    public boolean isJavaArtifact() {
        return pkg != null && !pkg.isEmpty();
    }

}
