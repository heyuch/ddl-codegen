package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.TreeVisitor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class Package implements PackageTree {

    private List<AnnotationTree> annotations = new ArrayList<>();

    private String path;

    public Package(String path) {
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        this.path = path;
    }

    public static Package of(String path) {
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        return new Package(path);
    }

    /**
     * 返回包路径。
     */
    public String getPath() {
        return path;
    }

    public Package subPackage(String path) {
        return new Package(this.path + "." + path);
    }

    @Override
    public List<? extends AnnotationTree> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    @Override
    public ExpressionTree getPackageName() {
        return new SourceExpr(path);
    }

    @Override
    public Kind getKind() {
        return Kind.PACKAGE;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitPackage(this, data);
    }

}
