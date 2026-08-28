package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.Tree;

/**
 * 收集模型节点引用的 import，集中各节点 getImports 的公共逻辑。
 */
final class ImportCollector {

    private ImportCollector() {}

    /**
     * 收集类节点（含继承、实现、字段、方法、内部类）引用的 import。
     */
    static List<Import> collect(Class c) {
        List<Import> imports = new ArrayList<>();

        if (c.getModifiers() instanceof Modifiers) {
            imports.addAll(((Modifiers)c.getModifiers()).getImports());
        }

        addTypeImports(imports, c.getExtendsClause());

        for (Tree impl : c.getImplementsClause()) {
            addTypeImports(imports, impl);
        }

        for (Tree member : c.getMembers()) {
            addMemberImports(imports, member);
        }

        return imports;
    }

    private static void addTypeImports(List<Import> imports, Tree type) {
        if (type instanceof TypeReference) {
            TypeReference tr = (TypeReference)type;
            if (tr.getPkg() != null) {
                imports.add(tr.getImport());
            }
        } else if (type instanceof ParameterizedType) {
            imports.addAll(((ParameterizedType)type).getImports());
        }
    }

    private static void addMemberImports(List<Import> imports, Tree member) {
        if (member instanceof Variable) {
            imports.addAll(((Variable)member).getImports());
        } else if (member instanceof Method) {
            imports.addAll(((Method)member).getImports());
        } else if (member instanceof Class) {
            imports.addAll(((Class)member).getImports());
        }
    }

}
