package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import hyc.codegen.tree.utils.CodePrinter;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

/**
 * 生成 import 语句：过滤隐式导入（java.lang、同包）、去重、按前缀分组排序后输出。
 * <p>
 * 从 JavaCodegen 拆出，让打印器只负责节点分发，import 策略收敛到单一职责类。
 */
final class ImportManager {

    private static final List<String> GROUP_PREFIXES = Arrays.asList("java");

    private ImportManager() {
        throw new AssertionError("no instances");
    }

    /**
     * 打印 import 语句；每个非空分组后输出一个空行。
     *
     * @param writer 单条 import 的写出回调（由调用方决定模型类如何渲染）
     */
    static void print(List<? extends ImportTree> imports, @Nullable PackageTree pkg, CodePrinter out,
            BiConsumer<ImportTree, CodePrinter> writer) {
        if (imports == null || imports.isEmpty()) {
            return;
        }

        List<? extends ImportTree> remain = removeImplicit(imports, pkg);
        remain = removeDuplicates(remain);

        for (List<? extends ImportTree> group : groupByPrefix(remain)) {
            if (group.isEmpty()) {
                continue;
            }
            for (ImportTree imp : sort(group)) {
                writer.accept(imp, out);
            }
            out.newline();
        }
    }

    private static List<? extends ImportTree> removeImplicit(List<? extends ImportTree> imports,
            @Nullable PackageTree pkg) {
        if (imports == null || imports.isEmpty()) {
            return new ArrayList<@UnknownKeyFor ImportTree>();
        }

        List<ImportTree> result = new ArrayList<>();
        for (ImportTree imp : imports) {
            if (!isImplicit(imp, pkg)) {
                result.add(imp);
            }
        }

        return result;
    }

    private static boolean isImplicit(ImportTree imp, @Nullable PackageTree pkg) {
        if (imp == null) {
            return true;
        }

        Tree qid = imp.getQualifiedIdentifier();
        if (qid == null) {
            return true;
        }
        String qname = qid.toString();

        // java.lang 包无需显式导入
        if (qname.startsWith("java.lang")) {
            return true;
        }

        // 当前包内的类无需导入
        if (pkg == null) {
            return false;
        }
        ExpressionTree pt = pkg.getPackageName();
        if (pt == null) {
            return false;
        }
        String currentPkg = pt.toString();

        String[] parts = qname.split("\\.");
        StringJoiner j = new StringJoiner(".");
        for (int i = 0; i < parts.length - 1; i++) {
            j.add(parts[i]);
        }

        return currentPkg.equals(j.toString());
    }

    private static List<? extends ImportTree> removeDuplicates(List<? extends ImportTree> imports) {
        if (imports == null || imports.isEmpty()) {
            return new ArrayList<@UnknownKeyFor ImportTree>();
        }

        Map<String, ImportTree> map = new HashMap<>();
        for (ImportTree imp : imports) {
            Tree qid = imp.getQualifiedIdentifier();
            if (qid != null) {
                map.putIfAbsent(qid.toString(), imp);
            }
        }

        return new ArrayList<@UnknownKeyFor ImportTree>(map.values());
    }

    private static List<List<? extends ImportTree>> groupByPrefix(List<? extends ImportTree> imports) {
        if (imports == null || imports.isEmpty()) {
            return Arrays.asList(new ArrayList<@UnknownKeyFor ImportTree>());
        }

        Map<String, ImportTree> map = new HashMap<>();
        for (ImportTree imp : imports) {
            map.putIfAbsent(imp.toString(), imp);
        }

        List<List<? extends ImportTree>> groups = new ArrayList<>();
        List<? extends ImportTree> remains = new ArrayList<@UnknownKeyFor ImportTree>(map.values());

        for (String prefix : GROUP_PREFIXES) {
            List<ImportTree> group = new ArrayList<>();
            for (ImportTree imp : new ArrayList<@UnknownKeyFor ImportTree>(remains)) {
                Tree qid = imp.getQualifiedIdentifier();
                if (qid.toString().startsWith(prefix)) {
                    group.add(imp);
                    remains.remove(imp);
                }
            }
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }

        if (!remains.isEmpty()) {
            groups.add(new ArrayList<@UnknownKeyFor ImportTree>(remains));
        }

        return groups;
    }

    static List<? extends ImportTree> sort(List<? extends ImportTree> imports) {
        if (imports == null || imports.isEmpty()) {
            return imports;
        }

        List<ImportTree> sorted = new ArrayList<@UnknownKeyFor ImportTree>(imports);
        sorted.sort(ImportManager::compare);

        return sorted;
    }

    private static int compare(ImportTree o1, ImportTree o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        return s1.compareTo(s2);
    }

}
