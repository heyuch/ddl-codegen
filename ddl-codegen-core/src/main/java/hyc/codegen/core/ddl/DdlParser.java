package hyc.codegen.core.ddl;

import java.util.List;

/**
 * DDL 解析 SPI：把 SQL 文本解析为规范化模型操作。
 * <p>
 * 默认实现为 {@link DruidDdlParser}（基于 com.alibaba.druid 的 MySQL 方言解析）。
 * 用户可替换为其他解析器（如 ANTLR 语法），只需产出相同的 {@link DdlOperation} 序列。
 */
public interface DdlParser {

    /**
     * 解析一段 DDL（可含多条语句，用分号分隔）。
     *
     * @param ddl SQL 文本
     * 
     * @return 按出现顺序的模型操作；不识别的语句记 warning 并跳过
     */
    List<DdlOperation> parse(String ddl);

}
