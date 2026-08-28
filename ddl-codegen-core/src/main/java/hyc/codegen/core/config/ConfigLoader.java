package hyc.codegen.core.config;

import java.nio.file.Path;

/**
 * 配置加载器 SPI。
 * <p>
 * 默认实现 {@link PropertiesConfigLoader} 使用 JDK Properties（零依赖）；
 * 需要 YAML/JSON 的用户实现本接口替换。
 */
public interface ConfigLoader {

    /**
     * 加载配置文件。
     *
     * @param configFile 配置文件路径，项目根 = 其父目录
     * 
     * @return 配置模型
     */
    DdlConfig load(Path configFile);

}
