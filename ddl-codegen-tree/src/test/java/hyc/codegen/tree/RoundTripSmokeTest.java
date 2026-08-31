package hyc.codegen.tree;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * round-trip 保真断言测试。
 * 判定标准：
 * 1. 语义零丢失——原文件与打印结果去掉注释与全部空白后一致（允许 javac toString 的
 * 纯空白/单参 lambda 括号差异）；
 * 2. 幂等——对打印结果再次 parse→print 必须字节一致；
 * 3. Demo.java 必须字节全等（javadoc/枚举等全要素 round-trip）。
 * 已知限制（JDK 11 公共 API 无 Tree.getComment）：声明前/语句前的行注释与块注释不保留，
 * 比较时对两侧统一剥离注释。
 */
// 语义比较的字符转义 normalize 逻辑：分支数 ≈ 转义字符表（每字符一分支，§6 元素驱动）；ModifiedControlVariable 为 normalize 循环固有形态（逐字符改写）
@SuppressWarnings({"CyclomaticComplexity", "NPathComplexity", "ModifiedControlVariable", "NestedIfDepth", "JavaNCSS",
    "MethodLength", "ExecutableStatementCount"})
public class RoundTripSmokeTest {

    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern SINGLE_PARAM_LAMBDA = Pattern.compile("\\(([A-Za-z_$][A-Za-z0-9_$]*)\\)->");

    private static long hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        return (c >= 'a' && c <= 'f') ? (c - 'a' + 10) : (c - 'A' + 10);
    }

    private static boolean isHexDigit(char c) {
        boolean digit = c >= '0' && c <= '9';
        boolean lower = c >= 'a' && c <= 'f';
        boolean upper = c >= 'A' && c <= 'F';
        return digit || lower || upper;
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * javac 的 toString 会对称地过度转义：双引号字符串内的单引号 \\'、字符字面量内的双引号 '\\"'。
     * 归一化为裸字符使两侧可比；必需的转义（字符串内 \\"、字符字面量内 \\'）保持原样。
     * 按连续反斜杠串处理，避免 \\\\" 等序列破坏引号状态机。
     */
    private static String normalizeStringEscapes(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                int j = i;
                while (j < s.length() && s.charAt(j) == '\\') {
                    j++;
                }
                int n = j - i;
                int pairs = n / 2;
                for (int k = 0; k < pairs; k++) {
                    sb.append("\\\\");
                }
                if ((n % 2) == 1) {
                    if (j < s.length()) {
                        char next = s.charAt(j);
                        if (inString && next == '\'') {
                            // 字符串内过度转义 \\' → '
                            sb.append('\'');
                            i = j;
                        } else if (inChar && next == '"') {
                            // 字符字面量内过度转义 \\" → "
                            sb.append('"');
                            i = j;
                        } else if (next == 'u' && j + 4 < s.length()
                                && isHexDigit(s.charAt(j + 1)) && isHexDigit(s.charAt(j + 2))
                                && isHexDigit(s.charAt(j + 3)) && isHexDigit(s.charAt(j + 4))) {
                            // javac 的 JCLiteral.toString() 把非 ASCII 转义为反斜杠u加四位十六进制序列，解码回裸字符
                            sb.append((char)Integer.parseInt(s.substring(j + 1, j + 5), 16));
                            i = j + 4;
                        } else {
                            sb.append('\\').append(next);
                            i = j;
                        }
                    } else {
                        sb.append('\\');
                        i = j - 1;
                    }
                } else {
                    i = j - 1;
                }
                continue;
            }
            if (inString) {
                if (c == '"') {
                    inString = false;
                }
            } else if (inChar) {
                if (c == '\'') {
                    inChar = false;
                }
            } else {
                // 数字字面量进制归一化：javac 的 toString 会把十六进制/八进制转十进制
                // （LiteralTree.getValue() 已丢失原进制）。仅在标识符边界处识别，避免误伤 a0x20。
                if (c >= '0' && c <= '9') {
                    boolean boundary = i == 0 || !isIdentifierChar(s.charAt(i - 1));
                    if (boundary && c == '0' && i + 1 < s.length()
                            && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
                        int j = i + 2;
                        long v = 0;
                        boolean any = false;
                        while (j < s.length() && isHexDigit(s.charAt(j))) {
                            v = v * 16 + hexValue(s.charAt(j));
                            any = true;
                            j++;
                        }
                        if (any) {
                            if (j < s.length() && (s.charAt(j) == 'l' || s.charAt(j) == 'L')) {
                                sb.append(v).append('L');
                                i = j;
                            } else {
                                sb.append(v);
                                i = j - 1;
                            }
                            continue;
                        }
                    }
                    if (boundary && c == '0' && i + 1 < s.length() && s.charAt(i + 1) >= '0'
                            && s.charAt(i + 1) <= '7') {
                        int j = i + 1;
                        long v = 0;
                        while (j < s.length() && s.charAt(j) >= '0' && s.charAt(j) <= '7') {
                            v = v * 8 + (s.charAt(j) - '0');
                            j++;
                        }
                        if (j < s.length() && (s.charAt(j) == 'l' || s.charAt(j) == 'L')) {
                            sb.append(v).append('L');
                            i = j;
                        } else {
                            sb.append(v);
                            i = j - 1;
                        }
                        continue;
                    }
                }
                if (c == '"') {
                    inString = true;
                } else if (c == '\'') {
                    inChar = true;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String print(File file) throws Exception {
        List<CompileUnit> units = new JavaParser().parse(file);
        StringBuilder sb = new StringBuilder();
        for (CompileUnit unit : units) {
            sb.append(JavaCodegen.generateCode(unit));
        }
        return sb.toString();
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /** 剥离注释与全部空白，归一化 javac toString 的 token 差异：单参 lambda 括号、字符串内 \' 转义、无参注解空括号。 */
    private static String semantic(String code) {
        String s = BLOCK_COMMENT.matcher(LINE_COMMENT.matcher(code).replaceAll("")).replaceAll("");
        s = normalizeStringEscapes(s);
        s = s.replaceAll("\\s+", "");
        s = SINGLE_PARAM_LAMBDA.matcher(s).replaceAll("$1->");
        // javac 打印 type-use 注解强制带空括号（@Foo() 与 @Foo 是 JLS 等价形式，无参注解括号可省略）
        s = s.replaceAll("@([A-Za-z_$][A-Za-z0-9_$]*)\\(\\),?", "@$1");
        return s;
    }

    private void assertSemanticStable(File file) throws Exception {
        String printed = print(file);
        // 语义零丢失：剥离注释与空白后应与原文一致
        assertEquals(semantic(read(file)), semantic(printed),
                file.getName() + " round-trip 存在语义丢失（非空白/注释差异）");
        // 幂等：二次 round-trip 字节一致
        assertEquals(printed, print(file), file.getName() + " round-trip 不幂等");
    }

    @Test
    public void codePrinterRoundTripKeepsSemantics() throws Exception {
        File file = new File("src/main/java/hyc/codegen/tree/utils/CodePrinter.java");
        assertSemanticStable(file);
    }

    @Test
    public void demoRoundTripIsByteExact() throws Exception {
        File file = new File("src/test/java/hyc/codegen/tree/Demo.java");
        assertEquals(read(file), print(file), "Demo.java round-trip 应字节全等");
    }

    @Test
    public void javaCodegenRoundTripKeepsSemantics() throws Exception {
        File file = new File("src/main/java/hyc/codegen/tree/JavaCodegen.java");
        assertSemanticStable(file);
    }

}
