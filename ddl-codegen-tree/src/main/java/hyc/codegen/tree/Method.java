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

    private DocCommentTree javadoc;

    private ModifiersTree modifiers;

    private Name name;

    private Tree returnType;

    private List<TypeParameterTree> typeParameters = new ArrayList<>();

    private List<VariableTree> parameters = new ArrayList<>();

    private VariableTree receiverParameter;

    private List<ExpressionTree> throwsList = new ArrayList<>();

    private BlockTree body;

    private Tree defaultValue;

    /**
     * 返回方法 javadoc 注释。
     */
    public DocCommentTree getJavadoc() {
        return javadoc;
    }

    /**
     * 设置方法 javadoc 注释。
     */
    public void setJavadoc(DocCommentTree javadoc) {
        this.javadoc = javadoc;
    }

    /**
     * 设置方法修饰符。
     */
    public void setModifiers(ModifiersTree modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * 设置方法名。
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * 设置返回类型。
     */
    public void setReturnType(Tree returnType) {
        this.returnType = returnType;
    }

    /**
     * 设置类型参数。
     */
    public void setTypeParameters(List<? extends TypeParameterTree> typeParameters) {
        this.typeParameters = new ArrayList<>(typeParameters);
    }

    /**
     * 添加参数。
     */
    public void addParameter(VariableTree parameter) {
        this.parameters.add(parameter);
    }

    /**
     * 设置接收者参数。
     */
    public void setReceiverParameter(VariableTree receiverParameter) {
        this.receiverParameter = receiverParameter;
    }

    /**
     * 设置方法体。
     */
    public void setBody(BlockTree body) {
        this.body = body;
    }

    /**
     * 设置注解类型元素的默认值。
     */
    public void setDefaultValue(Tree defaultValue) {
        this.defaultValue = defaultValue;
    }

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
        return new ArrayList<>(typeParameters);
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
        return new ArrayList<>(throwsList);
    }

    /**
     * 设置 throws 子句类型列表。
     */
    public void setThrowsList(List<? extends ExpressionTree> throwsList) {
        this.throwsList = new ArrayList<>(throwsList);
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

        if (modifiers == null) {
            Modifiers mod = new Modifiers();
            mod.addAnnotation(a);
            this.modifiers = mod;
        } else if (modifiers instanceof Modifiers) {
            ((Modifiers)modifiers).addAnnotation(a);
        } else {
            // 非模型 ModifiersTree（如解析出的 javac 节点）：复制现有注解与修饰符到模型容器，避免丢失
            Modifiers mod = new Modifiers(modifiers.getFlags());
            mod.addAnnotations(new ArrayList<>(modifiers.getAnnotations()));
            mod.addAnnotation(a);
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
                mods.addAnnotations(((Modifiers)m.modifiers).getAnnotations());
            }
            m.modifiers = mods;
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
            parameter.setVariableKind(VariableKind.PARAMETER);
            m.parameters.add(parameter);
            return this;
        }

        public Builder parameters(List<Variable> parameters) {
            parameters.forEach(p -> p.setVariableKind(VariableKind.PARAMETER));
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
