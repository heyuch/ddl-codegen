package hyc.codegen.core.naming;

/**
 * 表名 → 基类名策略 SPI（逃生口）。
 * <p>
 * 内置的配置化变换链（剥前缀/剥分表后缀/snake→Pascal）覆盖 95% 场景；
 * 长尾需求（如 {@code sys_} 前缀表要保留、日期分表转枚举等）实现本接口整体替换表→基类名逻辑。
 */
public interface TableNameStrategy {

    /**
     * 表名转基类名（不含 artifact 后缀）。
     *
     * @param tableName 原始表名（如 {@code t_user}）
     * 
     * @return 基类名（如 {@code User}）
     */
    String toBaseClassName(String tableName);

}
