package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Name;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;

public final class TypeParameter implements TypeParameterTree {

    private final Name name;

    private final List<Tree> bounds = new ArrayList<>();

    private final List<Annotation> annotations = new ArrayList<>();

    private TypeParameter(Name name) {
        this.name = name;
    }

    public static TypeParameter of(String name) {
        return new TypeParameter(new StringName(name));
    }

    public static TypeParameter wildcard() {
        return new TypeParameter(new StringName("?"));
    }

    public static TypeParameter wildcardExtends(TypeReference ext) {
        TypeParameter p = new TypeParameter(new StringName("? extends "));
        p.bounds.add(ext);
        return p;
    }

    public static TypeParameter wildcardSuper(TypeReference sup) {
        TypeParameter p = new TypeParameter(new StringName("? super "));
        p.bounds.add(sup);
        return p;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public List<? extends Tree> getBounds() {
        return new ArrayList<>(bounds);
    }

    @Override
    public List<? extends AnnotationTree> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    @Override
    public Kind getKind() {
        return Kind.TYPE_PARAMETER;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public List<Import> getImports() {
        List<Import> imports = new ArrayList<>();

        for (Tree bound : bounds) {
            if (bound instanceof TypeReference) {
                if (((TypeReference)bound).getPkg() != null) {
                    imports.add(((TypeReference)bound).getImport());
                }
            }
        }

        return imports;
    }

}
