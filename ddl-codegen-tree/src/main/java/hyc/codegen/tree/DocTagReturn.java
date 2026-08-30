package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ReturnTree;

// KeyFor（Map key）子检查对 JDK 泛型通配符（List<? extends DocTree> 的 capture）推断缺陷，
// 与 Map 无关的误报；仅本类抑制，其余代码 KeyFor 检查保留。
@SuppressWarnings("keyfor")
public final class DocTagReturn implements ReturnTree {

    private List<? extends DocTree> descs;

    public DocTagReturn(String desc) {
        this(Arrays.asList(new DocText(desc)));
    }

    public DocTagReturn(List<? extends DocTree> descs) {
        this.descs = new ArrayList<>(descs);
    }

    @Override
    public List<? extends DocTree> getDescription() {
        return new ArrayList<>(descs);
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.RETURN;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitReturn(this, data);
    }

}
