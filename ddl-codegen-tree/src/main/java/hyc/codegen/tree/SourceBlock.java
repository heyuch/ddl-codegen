package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TreeVisitor;

public final class SourceBlock implements BlockTree {

    private String code;

    public SourceBlock(String code) {
        this.code = code;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public List<? extends StatementTree> getStatements() {
        return new ArrayList<>();
    }

    @Override
    public Kind getKind() {
        return Kind.BLOCK;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitBlock(this, data);
    }

    /**
     * 返回方法体原始源码字符串。
     */
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }

}
