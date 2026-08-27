package hyc.codegen.tree;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.InlineTagTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.util.DocTreeScanner;
import hyc.codegen.tree.utils.CodePrinter;

// 扇出抑制依据（元素驱动而非逻辑混杂，见 STATIC-RULES-REVIEW.md §6）：
// DocTreeScanner 分发器，引用类型 ≈ doctree 节点类型数（20），无逻辑可抽取。
@SuppressWarnings("ClassFanOutComplexity")
public final class JavadocCodegen extends DocTreeScanner<Boolean, CodePrinter> {

    public static String generateCode(DocTree node) {
        StringWriter out = new StringWriter();
        generate(node, out);
        return out.toString();
    }

    public static void generate(DocTree node, Writer out) {
        generate(node, new CodePrinter(out));
    }

    public static void generate(DocTree node, CodePrinter out) {
        JavadocCodegen g = new JavadocCodegen();
        g.scan(node, out);
    }

    @Override
    public Boolean visitDocComment(DocCommentTree node, CodePrinter p) {
        p.println("/**");

        List<? extends DocTree> first = node.getFirstSentence();
        if (first != null && !first.isEmpty()) {
            p.print(" * ");
            for (DocTree d : first) {
                d.accept(this, p);
            }
            p.println();
        }

        List<? extends DocTree> body = node.getBody();
        if (body != null && !body.isEmpty()) {
            StringWriter sw = new StringWriter();
            for (DocTree d : body) {
                d.accept(this, new CodePrinter(sw));
            }
            String s = sw.toString();
            if (!s.isEmpty()) {
                String[] lines = s.split(System.lineSeparator());
                for (String line : lines) {
                    String trimmed = removeRedundantSpace(line);
                    p.println(" * ", trimmed);
                }
            }
        }

        List<? extends DocTree> tags = node.getBlockTags();
        if (tags != null && !tags.isEmpty()) {
            p.println(" *");
            for (DocTree tag : tags) {
                p.print(" * ");
                tag.accept(this, p);
                p.println();
            }
        }

        p.println(" */");
        return true;
    }

    private static String removeRedundantSpace(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        int i = 0;
        for (; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                break;
            }
        }

        if (i == 1) {
            return s.substring(1);
        } else {
            return s;
        }
    }

    @Override
    public Boolean visitText(TextTree node, CodePrinter p) {
        p.print(node.getBody());
        return true;
    }

    @Override
    public Boolean visitAuthor(AuthorTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getName());
    }

    private boolean visitBlockTag(BlockTagTree node, CodePrinter w, Object... args) {
        w.print("@", node.getTagName());
        visitVarargs(args, w);
        return true;
    }

    private void visitVarargs(Object[] args, CodePrinter p) {
        if (args == null) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof DocTree) {
                p.print(" ");
                ((DocTree)arg).accept(this, p);
            } else if (arg instanceof List) {
                List<?> list = (List<?>)arg;
                if (!list.isEmpty()) {
                    p.print(" ");
                    for (Object item : list) {
                        if (item instanceof DocTree) {
                            ((DocTree)item).accept(this, p);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Boolean visitSince(SinceTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getBody());
    }

    @Override
    public Boolean visitVersion(VersionTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getBody());
    }

    @Override
    public Boolean visitSee(SeeTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getReference());
    }

    @Override
    public Boolean visitParam(ParamTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getName(), node.getDescription());
    }

    @Override
    public Boolean visitReturn(ReturnTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getDescription());
    }

    @Override
    public Boolean visitThrows(ThrowsTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getExceptionName(), node.getDescription());
    }

    @Override
    public Boolean visitDeprecated(DeprecatedTree node, CodePrinter p) {
        return visitBlockTag(node, p, node.getBody());
    }

    @Override
    public Boolean visitStartElement(StartElementTree node, CodePrinter p) {
        p.print("<", node.getName());

        List<? extends DocTree> attrs = node.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            p.print(" ");
            visitForeach(attrs, p, " ");
        }

        if (node.isSelfClosing()) {
            p.print(" />");
        } else {
            p.print(">");
        }

        return true;
    }

    private void visitForeach(List<? extends DocTree> body, CodePrinter p, String seperator) {
        if (body == null) {
            return;
        }

        for (int i = 0, size = body.size(), last = size - 1; i < size; i++) {
            DocTree d = body.get(i);
            d.accept(this, p);
            if (seperator != null && i < last) {
                p.print(seperator);
            }
        }
    }

    @Override
    public Boolean visitEndElement(EndElementTree node, CodePrinter p) {
        p.print("</", node.getName(), ">");
        return true;
    }

    @Override
    public Boolean visitAttribute(AttributeTree node, CodePrinter p) {
        p.print(node.getName());

        AttributeTree.ValueKind kind = node.getValueKind();
        if (kind == AttributeTree.ValueKind.EMPTY) {
            return true;
        }

        p.print("=");

        if (kind == AttributeTree.ValueKind.DOUBLE) {
            p.print("\"");
        } else if (kind == AttributeTree.ValueKind.SINGLE) {
            p.print("'");
        }

        visitForeach(node.getValue(), p, " ");

        if (kind == AttributeTree.ValueKind.DOUBLE) {
            p.print("\"");
        } else if (kind == AttributeTree.ValueKind.SINGLE) {
            p.print("'");
        }

        return true;
    }

    @Override
    public Boolean visitReference(ReferenceTree node, CodePrinter p) {
        String s = node.getSignature();
        if (s != null) {
            p.print(s);
        }
        return true;
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree node, CodePrinter p) {
        if (node.getName() != null) {
            p.print(node.getName());
        }
        return true;
    }

    @Override
    public Boolean visitLink(LinkTree node, CodePrinter p) {
        return visitInlineTag(node, p, node.getReference(), node.getLabel());
    }

    private boolean visitInlineTag(InlineTagTree node, CodePrinter p, Object... args) {
        p.print("{@", node.getTagName());
        visitVarargs(args, p);
        p.print("}");
        return true;
    }

    @Override
    public Boolean visitLiteral(LiteralTree node, CodePrinter p) {
        return visitInlineTag(node, p, node.getBody());
    }

}
