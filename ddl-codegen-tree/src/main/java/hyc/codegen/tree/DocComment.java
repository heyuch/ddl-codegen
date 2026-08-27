package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;

public final class DocComment implements DocCommentTree {

    List<DocTree> summary = new ArrayList<>();

    List<DocTree> body = new ArrayList<>();

    List<DocTree> tags = new ArrayList<>();

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<? extends DocTree> getFirstSentence() {
        return new ArrayList<>(summary);
    }

    @Override
    public List<? extends DocTree> getBody() {
        return new ArrayList<>(body);
    }

    @Override
    public List<? extends DocTree> getBlockTags() {
        return new ArrayList<>(tags);
    }

    @Override
    public Kind getKind() {
        return Kind.DOC_COMMENT;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitDocComment(this, data);
    }

    public void addTag(DocTagVersion tag) {
        tags.add(tag);
    }

    public static final class Builder {

        private final DocComment d;

        public Builder() {
            this.d = new DocComment();
        }

        public Builder summary(Object... args) {
            if (args == null) {
                return this;
            }

            for (Object arg : args) {
                if (arg instanceof String) {
                    d.summary.add(new DocText((String)arg));
                } else if (arg instanceof DocTree) {
                    d.summary.add((DocTree)arg);
                }
            }

            return this;
        }

        public Builder body(Object... args) {
            if (args == null) {
                return this;
            }

            for (Object arg : args) {
                if (arg instanceof String) {
                    d.body.add(new DocText((String)arg));
                } else if (arg instanceof DocTree) {
                    d.body.add((DocTree)arg);
                } else if (arg instanceof List) {
                    List<?> list = (List<?>)arg;
                    for (Object item : list) {
                        if (item instanceof String) {
                            d.body.add(new DocText((String)item));
                        } else if (item instanceof DocTree) {
                            d.body.add((DocTree)item);
                        }
                    }
                }
            }

            d.body.add(new DocText(System.lineSeparator()));

            return this;
        }

        public Builder tag(DocTree tag) {
            d.tags.add(tag);
            return this;
        }

        public DocComment build() {
            return d;
        }

    }

}
