package hyc.codegen.tree;

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class Import implements ImportTree {

    private TypeReference type;

    private boolean isStatic;

    public Import(String qname) {
        this(new TypeReference(qname), false);
    }

    public Import(TypeReference type) {
        this(type, false);
    }

    public Import(TypeReference type, boolean isStatic) {
        this.type = type;
        this.isStatic = isStatic;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitImport(this, data);
    }

    @Override
    public Kind getKind() {
        return Kind.IMPORT;
    }

    @Override
    public Tree getQualifiedIdentifier() {
        return type;
    }

    public boolean isModule() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("import ");
        if (isStatic) {
            sb.append("static ");
        }
        sb.append(type.getQualifiedName());

        return sb.toString();
    }

}
