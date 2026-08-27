package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import hyc.codegen.tree.utils.Names;

public final class Class implements ClassTree {

    private DocComment javadoc;

    private Package pkg;

    private ModifiersTree modifiers;

    private Kind kind;

    private Name name;

    private List<TypeParameterTree> typeParameters = new ArrayList<>();

    private Tree extend;

    private List<Tree> impls = new ArrayList<>();

    private List<VariableTree> fields = new ArrayList<>();

    private List<MethodTree> methods = new ArrayList<>();

    private List<ClassTree> innerClasses = new ArrayList<>();

    /**
     * 返回类 javadoc 注释。
     */
    public DocComment getJavadoc() {
        return javadoc;
    }

    /**
     * 设置类 javadoc 注释。
     */
    public void setJavadoc(DocComment javadoc) {
        this.javadoc = javadoc;
    }

    /**
     * 返回所属包。
     */
    public Package getPkg() {
        return pkg;
    }

    /**
     * 设置所属包。
     */
    public void setPkg(Package pkg) {
        this.pkg = pkg;
    }

    /**
     * 设置修饰符。
     */
    public void setModifiers(ModifiersTree modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * 设置类类型（CLASS/ENUM/INTERFACE）。
     */
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    /**
     * 设置类名。
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * 设置类型参数。
     */
    public void setTypeParameters(List<? extends TypeParameterTree> typeParameters) {
        this.typeParameters = new ArrayList<>(typeParameters);
    }

    /**
     * 设置父类。
     */
    public void setExtendsClause(Tree extend) {
        this.extend = extend;
    }

    /**
     * 添加实现接口。
     */
    public void addImplements(Tree impl) {
        this.impls.add(impl);
    }

    @Override
    public ModifiersTree getModifiers() {
        return modifiers;
    }

    @Override
    public Name getSimpleName() {
        return name;
    }

    @Override
    public List<? extends TypeParameterTree> getTypeParameters() {
        return new ArrayList<>(typeParameters);
    }

    @Override
    public Tree getExtendsClause() {
        return extend;
    }

    @Override
    public List<? extends Tree> getImplementsClause() {
        return new ArrayList<>(impls);
    }

    @Override
    public List<? extends Tree> getMembers() {
        List<Tree> members = new ArrayList<>();

        members.addAll(fields);
        members.addAll(methods);
        members.addAll(innerClasses);

        return members;
    }

    public void addMember(Tree member) {
        if (member instanceof VariableTree) {
            fields.add((VariableTree)member);
        } else if (member instanceof MethodTree) {
            methods.add((MethodTree)member);
        } else if (member instanceof ClassTree) {
            innerClasses.add((ClassTree)member);
        }
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitClass(this, data);
    }

    public List<Import> getImports() {
        return ImportCollector.collect(this);
    }

    public void addAnnotation(AnnotationTree a) {
        if (a == null) {
            return;
        }

        if (modifiers instanceof Modifiers) {
            ((Modifiers)modifiers).addAnnotation(a);
        } else {
            Modifiers mod = new Modifiers();
            modifiers = mod;
        }
    }

    public void addField(VariableTree field) {
        if (field == null) {
            return;
        }
        fields.add(field);
    }

    public void addGetter(VariableTree prop, @Nullable Consumer<Method> fn) {
        String propName = String.valueOf(prop.getName());
        Method getter = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(prop.getType())
                .name("get" + Names.capitalize(propName))
                .body("return " + propName + ";")
                .build();

        if (fn != null) {
            fn.accept(getter);
        }

        addMethod(getter);
    }

    public void addSetter(VariableTree prop, @Nullable Consumer<Method> fn) {
        String propName = String.valueOf(prop.getName());
        Method setter = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(Types.VOID)
                .name("set" + Names.capitalize(propName))
                .parameter(Variable.builder()
                        .type(prop.getType())
                        .name(propName)
                        .build())
                .body(String.format("this.%s = %s;", propName, propName))
                .build();

        if (fn != null) {
            fn.accept(setter);
        }

        addMethod(setter);
    }

    public void addMethod(MethodTree method) {
        if (method == null) {
            return;
        }
        this.methods.add(method);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        Class c;

        public Builder() {
            this.c = new Class();
            c.kind = Kind.CLASS;
        }

        public Builder pkg(String path) {
            if (path == null) {
                return this;
            }
            c.pkg = Package.of(path);
            return this;
        }

        public Builder pkg(Package pkg) {
            c.pkg = pkg;
            return this;
        }

        public Builder kind(Kind kind) {
            c.kind = kind;
            return this;
        }

        public Builder javadoc(DocComment doc) {
            c.javadoc = doc;
            return this;
        }

        public Builder annotation(Annotation anno) {
            if (c.modifiers == null) {
                c.modifiers = new Modifiers();
            }

            if (c.modifiers instanceof Modifiers) {
                ((Modifiers)c.modifiers).addAnnotation(anno);
            }

            return this;
        }

        public Builder modifiers(Modifier... modifiers) {
            Modifiers mods = Modifiers.of(modifiers);
            if (c.modifiers != null) {
                if (c.modifiers instanceof Modifiers) {
                    mods.addAnnotations(((Modifiers)c.modifiers).getAnnotations());
                }
            }
            c.modifiers = mods;
            return this;
        }

        public Builder name(String name) {
            c.name = new StringName(name);
            return this;
        }

        public Builder typeParameter(TypeParameter p) {
            c.typeParameters.add(p);
            return this;
        }

        public Builder extend(TypeReference p) {
            c.extend = p;
            return this;
        }

        public Builder extend(ParameterizedType p) {
            c.extend = p;
            return this;
        }

        public Builder implement(TypeReference i) {
            c.impls.add(i);
            return this;
        }

        public Builder implement(ParameterizedType i) {
            c.impls.add(i);
            return this;
        }

        public Builder enumConstant(Variable f) {
            if (f.getVariableKind() == null) {
                f.setVariableKind(VariableKind.ENUM_CONSTANT);
            }
            c.fields.add(f);
            return this;
        }

        public Builder field(Variable f) {
            if (f.getVariableKind() == null) {
                f.setVariableKind(VariableKind.FIELD);
            }
            c.addField(f);
            return this;
        }

        public Builder method(Method m) {
            c.addMethod(m);
            return this;
        }

        public Class build() {
            return c;
        }

    }

}
