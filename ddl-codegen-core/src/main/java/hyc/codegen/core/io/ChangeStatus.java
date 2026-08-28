package hyc.codegen.core.io;

/**
 * 生成文件的变更状态。
 * <p>
 * 顶层枚举而非嵌套类成员：checkstyle 的 {@code ClassMemberImpliedModifier} 与
 * {@code RedundantModifier} 在嵌套 enum 上互相矛盾（见 STATIC-RULES-REVIEW.md §3），
 * 提为顶层类同时被 {@link FileWriter} 与 {@link ChangeReport} 共用。
 */
public enum ChangeStatus {

    /** 文件不存在，新建。 */
    CREATED,

    /** 文件已存在且内容变化，覆盖。 */
    UPDATED,

    /** 文件已存在且内容一致，未写盘。 */
    UNCHANGED,

    /** 文件被删除（drop table 等）。 */
    DELETED

}
