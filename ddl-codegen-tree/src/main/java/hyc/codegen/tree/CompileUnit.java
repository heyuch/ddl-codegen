package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.Name;
import javax.tools.JavaFileObject;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;

public final class CompileUnit implements CompilationUnitTree {

    PackageTree pkg;

    List<AnnotationTree> pkgAnnotations = new ArrayList<>();

    List<ImportTree> imports = new ArrayList<>();

    List<ClassTree> classes = new ArrayList<>();

    @Override
    public List<? extends AnnotationTree> getPackageAnnotations() {
        return new ArrayList<>(pkgAnnotations);
    }

    @Override
    public ExpressionTree getPackageName() {
        PackageTree p = pkg;
        return p.getPackageName();
    }

    @Override
    public PackageTree getPackage() {
        return pkg;
    }

    public void setPackage(Package pkg) {
        this.pkg = pkg;
    }

    @Override
    public List<? extends ImportTree> getImports() {
        return new ArrayList<>(imports);
    }

    @Override
    public List<? extends Tree> getTypeDecls() {
        return new ArrayList<>(classes);
    }

    @Nullable
    public Class getClass(CharSequence name) {
        if (name == null) {
            return null;
        }

        List<? extends Tree> decls = getTypeDecls();

        for (Tree decl : decls) {
            if (decl instanceof Class) {
                Class c = (Class)decl;
                Name className = c.getSimpleName();
                if (className.contentEquals(name)) {
                    return c;
                }
            }
        }

        return null;
    }

    @Override
    public JavaFileObject getSourceFile() {
        return null;
    }

    @Override
    public LineMap getLineMap() {
        return null;
    }

    @Override
    public Kind getKind() {
        return Kind.COMPILATION_UNIT;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitCompilationUnit(this, data);
    }

    public void addClass(Class c) {
        if (pkg == null) {
            pkg = c.pkg;
        }

        Class exist = getClass(c.getSimpleName());
        if (exist != null) {
            classes.remove(exist);
        }

        classes.add(c);
        imports.addAll(c.getImports());
    }

}
