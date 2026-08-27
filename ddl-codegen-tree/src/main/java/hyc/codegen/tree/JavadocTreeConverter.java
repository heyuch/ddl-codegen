package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTreeScanner;

class JavadocTreeConverter extends DocTreeScanner<DocTree, Void> {

    @Nullable
    DocComment convert(DocCommentTree tree) {
        return (DocComment)scan(tree, null);
    }

    @Override
    public DocTree visitDocComment(DocCommentTree node, Void unused) {
        DocComment c = new DocComment();

        List<? extends DocTree> summary = node.getFirstSentence();
        if (summary != null) {
            c.summary = new ArrayList<>(summary);
        }

        List<? extends DocTree> body = node.getBody();
        if (body != null) {
            c.body = new ArrayList<>(body);
        }

        List<? extends DocTree> tags = node.getBlockTags();
        if (tags != null) {
            c.tags = new ArrayList<>(tags);
        }

        return c;
    }

}
