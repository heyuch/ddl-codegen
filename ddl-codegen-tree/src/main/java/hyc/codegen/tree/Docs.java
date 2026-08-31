package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.source.doctree.DocTree;

public final class Docs {

    public static List<DocTree> html(String tag, String value) {
        return html(tag, value, Map.of());
    }

    public static List<DocTree> html(String tag, String value, Map<String, String> attrs) {
        List<DocTree> list = new ArrayList<>();

        List<DocTree> as = new ArrayList<>();
        attrs.forEach((k, v) -> {
            as.add(new DocAttr(k, v));
        });
        list.add(new DocElemStart(tag, false, as));
        list.add(new DocText(value));
        list.add(new DocElemEnd(tag));

        return list;
    }

    public static List<DocTree> http(String url, String text) {
        return html("a", text, Map.of("href", url));
    }

    public static List<DocTree> ref(String s) {
        List<DocTree> tree = new ArrayList<>();
        tree.add(new DocReference(s));
        return tree;
    }

}
