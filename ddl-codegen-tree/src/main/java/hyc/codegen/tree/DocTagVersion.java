package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.VersionTree;

public final class DocTagVersion implements VersionTree {

    private String version;

    public DocTagVersion(String version) {
        this.version = version;
    }

    @Override
    public List<? extends DocTree> getBody() {
        List<DocTree> body = new ArrayList<>();

        if (version != null) {
            body.add(new DocText(version));
        }

        return body;
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.VERSION;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitVersion(this, data);
    }

}
