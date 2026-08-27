package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;

public final class DocTagAuthor implements AuthorTree {

    String author;

    public DocTagAuthor(String author) {
        this.author = author;
    }

    @Override
    public List<? extends DocTree> getName() {
        if (author == null) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(new DocText(author));
        }
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.AUTHOR;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitAuthor(this, data);
    }

}
