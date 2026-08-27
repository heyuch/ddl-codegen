package hyc.codegen.tree.gen;

/**
 * 表达式片段助手：纯字符串组合，便于生成器拼接方法体。
 * <p>
 * 不做 import 登记——表达式引用的类型由生成器显式 addImport，保持助手无状态。
 */
public final class Expr {

    private Expr() {
        throw new AssertionError("no instances");
    }

    /** owner.method(args...) */
    public static String call(String owner, String method, String... args) {
        return owner + "." + method + "(" + String.join(", ", args) + ")";
    }

    /** owner.field */
    public static String member(String owner, String field) {
        return owner + "." + field;
    }

    /** cond ? yes : no */
    public static String ternary(String cond, String yes, String no) {
        return cond + " ? " + yes + " : " + no;
    }

    /** value == null ? null : thenExpr —— 可空引用做转换时的空安全三元。 */
    public static String nullSafe(String value, String thenExpr) {
        return value + " == null ? null : " + thenExpr;
    }

}
