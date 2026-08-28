package hyc.codegen.core.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.Variable;

/**
 * 拦截器工具：幂等注解替换（先移除自身管理的注解类型，再按目标集合重算）。
 */
public final class InterceptorSupport {

    private InterceptorSupport() {
        throw new AssertionError("no instances");
    }

    /**
     * 幂等替换成员注解：移除 {@code managedTypes} 中的现存注解，再追加 {@code targets}（去重）。
     *
     * @param member       目标成员（字段或方法）
     * @param managedTypes 本拦截器管理的注解类型名（简单名即可，匹配忽略包名）
     * @param targets      按模型重算出的目标注解
     */
    public static void replaceAnnotations(Variable member, List<String> managedTypes, List<AnnotationTree> targets) {
        replace(member, managedTypes, targets);
    }

    /** {@link #replaceAnnotations(Variable, List, List)} 的方法版。 */
    public static void replaceAnnotations(Method member, List<String> managedTypes, List<AnnotationTree> targets) {
        replace(member, managedTypes, targets);
    }

    /** 类级（Modifiers 容器）注解替换；{@code holder} 必须为 {@link hyc.codegen.tree.Modifiers} 模型实例。 */
    public static void replaceAnnotations(ModifiersTree holder, List<String> managedTypes,
            List<AnnotationTree> targets) {
        replace(holder, managedTypes, targets);
    }

    private static void replace(ModifiersTree holder, List<String> managedTypes, List<AnnotationTree> targets) {
        if (holder == null) {
            return;
        }
        for (String managed : managedTypes) {
            if (holder instanceof hyc.codegen.tree.Modifiers) {
                ((hyc.codegen.tree.Modifiers)holder).removeAnnotation(managed);
            }
        }
        for (AnnotationTree target : targets) {
            if (!hasAnnotation(holder, target)) {
                if (holder instanceof hyc.codegen.tree.Modifiers) {
                    ((hyc.codegen.tree.Modifiers)holder).addAnnotation(target);
                }
            }
        }
    }

    private static void replace(Variable member, List<String> managedTypes, List<AnnotationTree> targets) {
        replace(member.getModifiers(), managedTypes, targets);
    }

    private static void replace(Method member, List<String> managedTypes, List<AnnotationTree> targets) {
        replace(member.getModifiers(), managedTypes, targets);
    }

    private static boolean hasAnnotation(ModifiersTree holder, AnnotationTree target) {
        String targetName = String.valueOf(target.getAnnotationType());
        for (AnnotationTree existing : holder.getAnnotations()) {
            String name = String.valueOf(existing.getAnnotationType());
            if (name.equals(targetName) || name.endsWith("." + targetName)) {
                return true;
            }
        }
        return false;
    }

    /** 便捷：把可变参数转列表。 */
    public static List<String> managed(String... types) {
        List<String> list = new ArrayList<>();
        for (String type : types) {
            list.add(type);
        }
        return list;
    }

}
