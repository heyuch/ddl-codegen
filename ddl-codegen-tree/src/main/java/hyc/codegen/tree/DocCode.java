package hyc.codegen.tree;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.TextTree;

public final class DocCode implements LiteralTree {

    String code;

    public DocCode(String code) {
        this.code = code;
    }

    @Override
    public TextTree getBody() {
        return new DocText(code);
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.CODE;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitLiteral(this, data);
    }

}
