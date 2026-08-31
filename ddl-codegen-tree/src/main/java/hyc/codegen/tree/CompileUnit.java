package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
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
import org.checkerframework.checker.nullness.qual.Nullable;

// 可修改 AST 节点（AGENTS.md「自研可修改 Java AST」）：字段由 setter/转换器在构造后设置，
public final class CompileUnit implements CompilationUnitTree {

    private @Nullable PackageTree pkg;

    private List<AnnotationTree> pkgAnnotations = new ArrayList<>();

    private List<ImportTree> imports = new ArrayList<>();

    private List<ClassTree> classes = new ArrayList<>();

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitCompilationUnit(this, data);
    }

    public void addClass(Class c) {
        if (pkg == null) {
            pkg = c.getPkg();
        }

        Class exist = getClass(c.getSimpleName());
        if (exist != null) {
            classes.remove(exist);
        }

        classes.add(c);
        imports.addAll(c.getImports());
    }

    /**
     * 添加 import 声明。
     */
    public void addImport(ImportTree imp) {
        this.imports.add(imp);
    }

    public @Nullable Class getClass(CharSequence name) {
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
    public List<? extends ImportTree> getImports() {
        return new ArrayList<>(imports);
    }

    @Override
    public Kind getKind() {
        return Kind.COMPILATION_UNIT;
    }

    @Override
    @Nullable
    // javac tree API 语义：本库不追踪源码位置（生成的 AST 无源文件/行号）
    @SuppressWarnings("override.return")
    public LineMap getLineMap() {
        return null;
    }

    @Override
    @Nullable
    // javac tree API 语义：无 package 声明的编译单元 getPackage 返回 null
    @SuppressWarnings("override.return")
    public PackageTree getPackage() {
        return pkg;
    }

    @Override
    public List<? extends AnnotationTree> getPackageAnnotations() {
        return new ArrayList<>(pkgAnnotations);
    }

    @Override
    @Nullable
    // javac tree API 语义：无 package 声明的编译单元 getPackageName 返回 null
    @SuppressWarnings("override.return")
    public ExpressionTree getPackageName() {
        PackageTree p = pkg;
        return p == null ? null : p.getPackageName();
    }

    @Override
    @Nullable
    // javac tree API 语义：本库不追踪源码位置（生成的 AST 无源文件/行号）
    @SuppressWarnings("override.return")
    public JavaFileObject getSourceFile() {
        return null;
    }

    @Override
    public List<? extends Tree> getTypeDecls() {
        return new ArrayList<>(classes);
    }

    public void setPackage(PackageTree pkg) {
        this.pkg = pkg;
    }

}
