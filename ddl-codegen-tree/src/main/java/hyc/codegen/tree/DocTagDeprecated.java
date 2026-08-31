package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

public final class DocTagDeprecated implements DeprecatedTree {

    private List<? extends DocTree> body;

    public DocTagDeprecated(List<? extends DocTree> body) {
        this.body = new ArrayList<@UnknownKeyFor DocTree>(body);
    }

    public DocTagDeprecated(String desc) {
        this(Arrays.asList(new DocText(desc)));
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitDeprecated(this, data);
    }

    @Override
    public List<? extends DocTree> getBody() {
        return new ArrayList<@UnknownKeyFor DocTree>(body);
    }

    @Override
    public Kind getKind() {
        return Kind.DEPRECATED;
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

}
