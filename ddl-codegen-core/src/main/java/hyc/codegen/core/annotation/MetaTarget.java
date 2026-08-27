package hyc.codegen.core.annotation;

/**
 * DDL 注解可出现的位置。
 */
public enum MetaTarget {
    /** 表注释（comment）。 */
    TABLE,
    /** 列注释。 */
    COLUMN,
    /** 索引注释。 */
    INDEX
}
