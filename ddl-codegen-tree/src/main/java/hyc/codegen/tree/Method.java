package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;

public final class Method implements MethodTree {

    DocCommentTree javadoc;

    ModifiersTree modifiers;

    Name name;

    Tree returnType;

    List<TypeParameterTree> typeParameters = new ArrayList<>();

    List<VariableTree> parameters = new ArrayList<>();

    VariableTree receiverParameter;

    BlockTree body;

    Tree defaultValue;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ModifiersTree getModifiers() {
        return modifiers;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public Tree getReturnType() {
        return returnType;
    }

    @Override
    public List<? extends TypeParameterTree> getTypeParameters() {
        return List.of();
    }

    @Override
    public List<? extends VariableTree> getParameters() {
        return new ArrayList<>(parameters);
    }

    @Override
    public VariableTree getReceiverParameter() {
        return receiverParameter;
    }

    @Override
    public List<? extends ExpressionTree> getThrows() {
        return new ArrayList<>();
    }

    @Override
    public BlockTree getBody() {
        return body;
    }

    @Override
    public Tree getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Kind getKind() {
        return Kind.METHOD;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitMethod(this, data);
    }

    public List<Import> getImports() {
        List<Import> imports = new ArrayList<>();

        if (modifiers instanceof Modifiers) {
            imports.addAll(((Modifiers)modifiers).getImports());
        }

        if (returnType instanceof TypeReference) {
            imports.add(((TypeReference)returnType).getImport());
        } else if (returnType instanceof ParameterizedType) {
            imports.addAll(((ParameterizedType)returnType).getImports());
        }

        for (VariableTree p : parameters) {
            if (p instanceof Variable) {
                imports.addAll(((Variable)p).getImports());
            }
        }

        return imports;
    }

    public void addAnnotation(AnnotationTree a) {
        if (a == null) {
            return;
        }

        if (modifiers instanceof Modifiers) {
            ((Modifiers)modifiers).addAnnotation(a);
        } else {
            Modifiers mod = new Modifiers();
            this.modifiers = mod;
        }
    }

    public static final class Builder {

        Method m;

        public Builder() {
            this.m = new Method();
        }

        public Builder javadoc(DocComment doc) {
            m.javadoc = doc;
            return this;
        }

        public Builder annotation(Annotation anno) {
            if (m.modifiers == null) {
                m.modifiers = new Modifiers();
            }
            if (m.modifiers instanceof Modifiers) {
                ((Modifiers)m.modifiers).addAnnotation(anno);
            }
            return this;
        }

        public Builder modifiers(Modifier... modifiers) {
            Modifiers mods = Modifiers.of(modifiers);
            if (m.modifiers instanceof Modifiers) {
                mods.annotations.addAll(((Modifiers)m.modifiers).annotations);
            }
            m.modifiers = mods;
            return this;
        }

        public Builder mods(Modifiers modifiers) {
            m.modifiers = modifiers;
            return this;
        }

        public Builder returnType(PrimitiveType type) {
            m.returnType = type;
            return this;
        }

        public Builder returnType(TypeReference type) {
            m.returnType = type;
            return this;
        }

        public Builder returnType(ParameterizedType type) {
            m.returnType = type;
            return this;
        }

        public Builder returnType(Tree type) {
            m.returnType = type;
            return this;
        }

        public Builder name(String name) {
            m.name = new StringName(name);
            return this;
        }

        public Builder name(Name name) {
            m.name = name;
            return this;
        }

        public Builder parameter(Variable parameter) {
            parameter.kind = VariableKind.PARAMETER;
            m.parameters.add(parameter);
            return this;
        }

        public Builder parameters(List<Variable> parameters) {
            parameters.forEach(p -> p.kind = VariableKind.PARAMETER);
            m.parameters = new ArrayList<>(parameters);
            return this;
        }

        public Builder body(String code) {
            m.body = new SourceBlock(code);
            return this;
        }

        public Method build() {
            return m;
        }

    }

}
