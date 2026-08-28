package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;

public final class ParameterizedType implements ParameterizedTypeTree {

    private TypeReference type;

    private List<Tree> typeArguments;

    public ParameterizedType(TypeReference type, Tree... typeArguments) {
        this(type, Arrays.asList(typeArguments));
    }

    public ParameterizedType(TypeReference type, List<Tree> typeArguments) {
        this.type = type;
        this.typeArguments = new ArrayList<>(typeArguments);
    }

    @Override
    public Tree getType() {
        return type;
    }

    @Override
    public List<? extends Tree> getTypeArguments() {
        return new ArrayList<>(typeArguments);
    }

    @Override
    public Kind getKind() {
        return Kind.PARAMETERIZED_TYPE;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitParameterizedType(this, data);
    }

    public List<Import> getImports() {
        List<Import> imports = new ArrayList<>();

        imports.add(new Import(type));

        for (Tree arg : typeArguments) {
            if (arg instanceof TypeReference) {
                if (((TypeReference)arg).getPkg() != null) {
                    imports.add(((TypeReference)arg).getImport());
                }
            } else if (arg instanceof ParameterizedType) {
                imports.addAll(((ParameterizedType)arg).getImports());
            } else if (arg instanceof TypeParameter) {
                imports.addAll(((TypeParameter)arg).getImports());
            }
        }

        return imports;
    }

}
