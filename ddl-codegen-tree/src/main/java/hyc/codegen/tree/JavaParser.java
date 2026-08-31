package hyc.codegen.tree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;

public final class JavaParser {

    private final JavaCompiler compiler;

    private final StandardJavaFileManager fileManager;

    public JavaParser() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.fileManager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);
    }

    public List<CompileUnit> parse(File file) throws IOException {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(file);

        return parseFileObjects(fileObjects);
    }

    public List<CompileUnit> parseCode(String code) throws IOException {
        SourceJavaFileObject object = new SourceJavaFileObject("__Mock", code);
        return parseFileObjects(Arrays.asList(object));
    }

    private List<CompileUnit> parseFileObjects(Iterable<? extends JavaFileObject> objects) throws IOException {
        JavacTask task = (JavacTask)compiler.getTask(null, fileManager, null, null, null, objects);

        DocTrees docTrees = DocTrees.instance(task);
        Iterable<? extends CompilationUnitTree> units = task.parse();

        List<CompileUnit> result = new ArrayList<>();
        JavaTreeConverter treeConverter = new JavaTreeConverter(docTrees);
        for (CompilationUnitTree unit : units) {
            result.add(treeConverter.convert(unit));
        }

        return result;
    }

}
