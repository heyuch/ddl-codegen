package hyc.codegen.tree;

import javax.lang.model.type.TypeKind;

import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.TreeVisitor;

public final class PrimitiveType implements PrimitiveTypeTree {

    TypeKind typeKind;

    public PrimitiveType(TypeKind typeKind) {
        this.typeKind = typeKind;
    }

    @Override
    public TypeKind getPrimitiveTypeKind() {
        return typeKind;
    }

    @Override
    public Kind getKind() {
        return Kind.PRIMITIVE_TYPE;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitPrimitiveType(this, data);
    }

}
