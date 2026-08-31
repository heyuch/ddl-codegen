package hyc.codegen.core.gen;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.Variable;

/**
 * {@code @Generated} 成员标记工具：区分工具拥有与用户手写的成员。
 * <p>
 * 契约（DESIGN §1）：带 {@code @Generated}（{@code javax.annotation.processing.Generated}，
 * JDK 自带，零依赖）的成员归工具所有，reconcile 时增删改；其余成员永不触碰。
 */
public final class GeneratedSupport {

    /** 生成标记注解全限定名。 */
    public static final String GENERATED = "javax.annotation.processing.Generated";

    private GeneratedSupport() {
        throw new AssertionError("no instances");
    }

    private static boolean hasGenerated(ModifiersTree mods) {
        if (mods == null) {
            return false;
        }
        for (AnnotationTree annotation : mods.getAnnotations()) {
            String name = String.valueOf(annotation.getAnnotationType());
            if ("Generated".equals(name) || name.endsWith(".Generated")) {
                return true;
            }
        }
        return false;
    }

    /** 是否为工具生成的方法（带 {@code @Generated}）。 */
    public static boolean isGenerated(Method member) {
        return hasGenerated(member.getModifiers());
    }

    /** 是否为工具生成的字段（带 {@code @Generated}）。 */
    public static boolean isGenerated(Variable member) {
        return hasGenerated(member.getModifiers());
    }

    /** 给方法打上生成标记（已存在则跳过）。 */
    public static void mark(Method member) {
        if (!isGenerated(member)) {
            member.addAnnotation(Annotation.of(GENERATED));
        }
    }

    /** 给字段打上生成标记（已存在则跳过）。 */
    public static void mark(Variable member) {
        if (!isGenerated(member)) {
            member.addAnnotation(Annotation.of(GENERATED));
        }
    }

}
