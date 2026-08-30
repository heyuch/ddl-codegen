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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// 可修改 AST 节点（AGENTS.md「自研可修改 Java AST」）：字段由静态工厂/builder 在构造后设置，
public final class Method implements MethodTree {

    private @Nullable DocCommentTree javadoc;

    @MonotonicNonNull
    private ModifiersTree modifiers;

    @MonotonicNonNull
    private Name name;

    private @Nullable Tree returnType;

    private List<TypeParameterTree> typeParameters = new ArrayList<>();

    private List<VariableTree> parameters = new ArrayList<>();

    private @Nullable VariableTree receiverParameter;

    private List<ExpressionTree> throwsList = new ArrayList<>();

    private @Nullable BlockTree body;

    private @Nullable Tree defaultValue;

    /**
     * 返回方法 javadoc 注释。
     */
    public @Nullable DocCommentTree getJavadoc() {
        return javadoc;
    }

    /**
     * 设置方法 javadoc 注释。
     */
    public void setJavadoc(@Nullable DocCommentTree javadoc) {
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
    public void setReceiverParameter(@Nullable VariableTree receiverParameter) {
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
    public void setDefaultValue(@Nullable Tree defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ModifiersTree getModifiers() {
        if (modifiers == null) {
            throw new IllegalStateException("modifiers 未初始化（构建器/转换器未设置）");
        }
        return modifiers;
    }

    @Override
    public Name getName() {
        if (name == null) {
            throw new IllegalStateException("name 未初始化（构建器/转换器未设置）");
        }
        return name;
    }

    @Override
    @Nullable
    // javac tree API 语义：构造器/抽象方法可无返回类型/方法体（MethodTree.getReturnType 对构造器返回 null）
    @SuppressWarnings("override.return")
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
    @Nullable
    // javac tree API 语义：无 receiver 参数时 getReceiverParameter 返回 null
    @SuppressWarnings("override.return")
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
    @Nullable
    // javac tree API 语义：接口/抽象方法无方法体（MethodTree.getBody 对抽象方法返回 null）
    @SuppressWarnings("override.return")
    public BlockTree getBody() {
        return body;
    }

    @Override
    @Nullable
    // javac tree API 语义：注解方法无默认值时 getDefaultValue 返回 null
    @SuppressWarnings("override.return")
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

        Tree rt = returnType;
        if (rt instanceof TypeReference) {
            if (((TypeReference)rt).getPkg() != null) {
                imports.add(((TypeReference)rt).getImport());
            }
        } else if (rt instanceof ParameterizedType) {
            imports.addAll(((ParameterizedType)rt).getImports());
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
            m.modifiers = new Modifiers();
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
