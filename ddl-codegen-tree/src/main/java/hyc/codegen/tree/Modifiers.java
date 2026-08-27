package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.TreeVisitor;

public final class Modifiers implements ModifiersTree {

    private Set<Modifier> modifiers;

    private List<AnnotationTree> annotations = new ArrayList<>();

    private boolean annotationInline;

    public Modifiers() {
        this.modifiers = EnumSet.noneOf(Modifier.class);
    }

    public Modifiers(Set<Modifier> modifiers) {
        this.modifiers = EnumSet.copyOf(modifiers);
    }

    public static Modifiers of(Modifier... modifiers) {
        List<Modifier> list = Arrays.asList(modifiers);
        return new Modifiers(new HashSet<>(list));
    }

    /**
     * 返回注解是否内联打印（同一行）。
     */
    public boolean isAnnotationInline() {
        return annotationInline;
    }

    /**
     * 设置注解是否内联打印。
     */
    public void setAnnotationInline(boolean annotationInline) {
        this.annotationInline = annotationInline;
    }

    public void addAnnotations(java.util.Collection<? extends AnnotationTree> annotations) {
        if (annotations == null) {
            return;
        }
        this.annotations.addAll(annotations);
    }

    public void addAnnotation(AnnotationTree a) {
        if (a == null) {
            return;
        }
        annotations.add(a);
    }

    @Override
    public Set<Modifier> getFlags() {
        return EnumSet.copyOf(modifiers);
    }

    @Override
    public List<? extends AnnotationTree> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    @Override
    public Kind getKind() {
        return Kind.MODIFIERS;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitModifiers(this, data);
    }

    public List<Import> getImports() {
        List<Import> imports = new ArrayList<>();
        for (AnnotationTree a : annotations) {
            if (a instanceof Annotation) {
                imports.addAll(((Annotation)a).getImports());
            }
        }
        return imports;
    }

}
