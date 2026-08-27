package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.SeeTree;

public final class DocTagSee implements SeeTree {

    private List<DocTree> refs;

    public DocTagSee(List<DocTree> refs) {
        this.refs = new ArrayList<>(refs);
    }

    @Override
    public List<? extends DocTree> getReference() {
        return new ArrayList<>(refs);
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.SEE;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitSee(this, data);
    }

}
