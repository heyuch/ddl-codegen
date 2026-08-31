package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ReturnTree;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

public final class DocTagReturn implements ReturnTree {

    private List<? extends DocTree> descs;

    public DocTagReturn(List<? extends DocTree> descs) {
        this.descs = new ArrayList<@UnknownKeyFor DocTree>(descs);
    }

    public DocTagReturn(String desc) {
        this(Arrays.asList(new DocText(desc)));
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitReturn(this, data);
    }

    @Override
    public List<? extends DocTree> getDescription() {
        return new ArrayList<@UnknownKeyFor DocTree>(descs);
    }

    @Override
    public Kind getKind() {
        return Kind.RETURN;
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

}
