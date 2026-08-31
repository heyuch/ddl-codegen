package hyc.codegen.tree;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.TextTree;

public final class DocText implements TextTree {

    private String body;

    public DocText(String body) {
        this.body = body;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitText(this, data);
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public Kind getKind() {
        return Kind.TEXT;
    }

}
