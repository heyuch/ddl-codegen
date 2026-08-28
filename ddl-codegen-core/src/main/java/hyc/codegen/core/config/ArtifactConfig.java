package hyc.codegen.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * 单个 artifact（生成物类型）的配置。
 * <p>
 * 对应 {@code artifacts.<kind>.*} 配置段；kind 即生成器名字（entity/pojo/mybatisMapper/...），
 * 配置了即启用。module 为空表示项目根。
 */
public final class ArtifactConfig {

    private final String kind;

    @Nullable
    private String module;

    @Nullable
    private String pkg;

    private String suffix = "";

    @Nullable
    private String path;

    private final List<String> use = new ArrayList<>();

    private final Map<String, String> options = new LinkedHashMap<>();

    public ArtifactConfig(String kind) {
        this.kind = kind;
    }

    /** 生成器名字（config 键 {@code artifacts.<kind>}）。 */
    public String getKind() {
        return kind;
    }

    /** 所属模块 = 项目根下的一级子目录；空表示项目根。 */
    @Nullable
    public String getModule() {
        return module;
    }

    public void setModule(@Nullable String module) {
        this.module = module;
    }

    /** Java 包名；XML 类 artifact 可省略（用 path）。 */
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

    /** 资源路径（XML 用，相对 module）；Java 类 artifact 走 package。 */
    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    /** 拦截器链（{@code use=lombok,jsr303}），按序执行。 */
    public List<String> getUse() {
        return new ArrayList<>(use);
    }

    public void setUse(List<String> use) {
        this.use.clear();
        this.use.addAll(use);
    }

    /** 该 artifact 的其余任意键值（如 repositoryImpl.di=field）。 */
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

    /** 是否 Java 类 artifact（package 路径），而非资源文件（path）。 */
    public boolean isJavaArtifact() {
        return pkg != null && !pkg.isEmpty();
    }

}
