package hyc.codegen.core.io;

import java.nio.file.Path;

/**
 * 生成文件路径解析：项目根 + module（根下一级子目录，空=根）+ package 路径 / 资源路径。
 */
public final class PathResolver {

    private PathResolver() {
        throw new AssertionError("no instances");
    }

    /**
     * Java 类文件路径：{@code 根/module/包路径/类名.java}。
     *
     * @param root      项目根（config 所在目录）
     * @param module    module 名；空或 null 表示直接落在根下
     * @param pkg       Java 包名（点分隔）
     * @param className 类名
     */
    public static Path javaFile(Path root, String module, String pkg, String className) {
        return modulePath(root, module).resolve(pkg.replace('.', '/')).resolve(className + ".java");
    }

    /**
     * 资源文件路径（XML 等）：{@code 根/module/相对资源路径/文件名}。
     *
     * @param root         项目根
     * @param module       module 名；空或 null 表示根下
     * @param resourcePath 相对资源路径（如 {@code src/main/resources/mapper}）
     * @param fileName     文件名（含扩展名）
     */
    public static Path xmlFile(Path root, String module, String resourcePath, String fileName) {
        return modulePath(root, module).resolve(resourcePath).resolve(fileName);
    }

    private static Path modulePath(Path root, String module) {
        if (module == null || module.isEmpty()) {
            return root;
        }
        return root.resolve(module);
    }

}
