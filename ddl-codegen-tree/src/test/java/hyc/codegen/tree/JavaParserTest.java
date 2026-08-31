package hyc.codegen.tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaParserTest {

    private static String getFileContent(File file) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line)
                        .append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    @Test
    public void parse() throws IOException {
        File file = new File("src/test/java/hyc/codegen/tree/Demo.java");
        JavaParser parser = new JavaParser();
        List<CompileUnit> units = parser.parse(file);

        StringBuilder sb = new StringBuilder();
        for (CompileUnit unit : units) {
            String code = JavaCodegen.generateCode(unit);
            sb.append(code);
        }

        String expected = getFileContent(file);

        Assertions.assertEquals(expected, sb.toString());
    }

}
