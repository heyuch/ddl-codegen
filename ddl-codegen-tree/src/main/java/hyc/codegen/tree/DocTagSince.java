package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.SinceTree;

public final class DocTagSince implements SinceTree {

    private String since;

    public DocTagSince(String since) {
        this.since = since;
    }

    @Override
    public List<? extends DocTree> getBody() {
        if (since == null) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(new DocText(since));
        }
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.SINCE;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitSince(this, data);
    }

}
