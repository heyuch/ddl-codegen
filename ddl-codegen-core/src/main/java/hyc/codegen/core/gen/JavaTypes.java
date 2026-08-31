package hyc.codegen.core.gen;

import com.sun.source.tree.Tree;
import hyc.codegen.tree.ArrayType;
import hyc.codegen.tree.TypeReference;

/**
 * Java 类型字符串 → tree 类型节点（处理数组类型 {@code byte[]} 等）。
 */
public final class JavaTypes {

    private JavaTypes() {
        throw new AssertionError("no instances");
    }

    /**
     * 把全限定名/简单名/数组类型字符串转为 tree 类型节点。
     */
    public static Tree typeTree(String javaType) {
        if (javaType.endsWith("[]")) {
            return new ArrayType(new TypeReference(javaType.substring(0, javaType.length() - 2)));
        }
        return new TypeReference(javaType);
    }

}
