package hyc.codegen.tree;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;

/**
 * 数组类型（如 {@code byte[]}）：可变的 {@link ArrayTypeTree} 模型。
 */
public final class ArrayType implements ArrayTypeTree {

    private TypeReference component;

    public ArrayType(TypeReference component) {
        this.component = component;
    }

    public TypeReference getComponentType() {
        return component;
    }

    public void setComponentType(TypeReference component) {
        this.component = component;
    }

    @Override
    public Tree getType() {
        return component;
    }

    @Override
    public Kind getKind() {
        return Kind.ARRAY_TYPE;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitArrayType(this, data);
    }

    @Override
    public String toString() {
        return component + "[]";
    }

}
