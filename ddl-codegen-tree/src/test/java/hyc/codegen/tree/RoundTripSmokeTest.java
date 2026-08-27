package hyc.codegen.tree;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

public class RoundTripSmokeTest {

    private void roundTrip(File file) throws Exception {
        JavaParser parser = new JavaParser();
        List<CompileUnit> units = parser.parse(file);
        StringBuilder sb = new StringBuilder();
        for (CompileUnit unit : units) {
            sb.append(JavaCodegen.generateCode(unit));
        }
        String original = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        String[] o = original.split("\n", -1);
        String[] n = sb.toString().split("\n", -1);
        int diff = 0;
        for (int i = 0; i < Math.max(o.length, n.length); i++) {
            String ol = i < o.length ? o[i] : "<EOF>";
            String nl = i < n.length ? n[i] : "<EOF>";
            if (!ol.equals(nl)) {
                diff++;
                if (diff <= 10) {
                    System.out.println("L" + (i + 1) + " O: " + ol);
                    System.out.println("L" + (i + 1) + " N: " + nl);
                }
            }
        }
        System.out.println(file.getName() + ": total lines " + o.length + " -> " + n.length + ", diff lines " + diff);
    }

    @Test
    public void smoke() throws Exception {
        roundTrip(new File("src/main/java/hyc/codegen/tree/JavaCodegen.java"));
        roundTrip(new File("src/main/java/hyc/codegen/tree/utils/CodePrinter.java"));
        roundTrip(new File("src/test/java/hyc/codegen/tree/Demo.java"));
    }

}
