package hyc.codegen.core.gen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.VariableTree;
import hyc.codegen.core.io.ChangeStatus;
import hyc.codegen.core.io.FileWriter;
import hyc.codegen.core.io.PathResolver;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.CompileUnit;
import hyc.codegen.tree.JavaCodegen;
import hyc.codegen.tree.JavaParser;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.Variable;

/**
 * Java artifact 生成器基类：定位 / 解析 / @Generated 成员级 reconcile / 拦截器 / 打印 / 写盘 的完整管线。
 * <p>
 * 子类只需实现 {@link #buildClass}（从模型构建期望成员）与 {@link #kind}，并可覆盖
 * {@link #shouldGenerate}（如 enum 文件仅在存在 enum 列时生成）。
 * <p>
 * reconcile 语义（DESIGN §1/§4）：模型有而文件无 → 增；有而模型无 → 删；类型/签名变 → 替换；一致 → 跳过。
 * 只动 {@code @Generated} 成员，用户手写成员永不触碰；解析失败 → 记警告跳过（不覆盖）。
 */
public abstract class AbstractJavaArtifactGenerator implements ArtifactGenerator {

    private final JavaParser parser = new JavaParser();

    /** 生成/更新该表该 artifact 的 Java 文件。 */
    public void generate(TableContext ctx, GenerationContext gctx) {
        if (!shouldGenerate(ctx)) {
            deleteIfExists(ctx, gctx);
            return;
        }
        generateClass(ctx, gctx, ctx.className(), builder -> buildClass(builder, ctx, gctx));
    }

    /**
     * 生成单个类文件（支持一表多文件的 artifact，如 enum 类按列生成多个文件）。
     *
     * @param ctx       表上下文
     * @param gctx      全局上下文
     * @param className 目标类名（决定文件路径与 reconcile 的类名匹配）
     * @param builderFn 用给定 builder 构建该类成员
     */
    protected void generateClass(TableContext ctx, GenerationContext gctx, String className,
            java.util.function.Consumer<Class.Builder> builderFn) {
        Class fresh = buildFresh(ctx, gctx, className, builderFn);
        File file = PathResolver.javaFile(gctx.getProjectRoot(),
                ctx.getArtifactConfig().getModule(), ctx.packageName(), className).toFile();

        CompileUnit existingCu = parse(file);
        Class target;
        CompileUnit cu;
        if (existingCu == null) {
            target = fresh;
            cu = new CompileUnit();
        } else {
            Class existingClass = existingCu.getClass(className);
            if (existingClass == null) {
                // 契约：以 config 为准，文件里没有期望类名 → 视为新建（用户迁移代码未改 config 的后果自负）
                target = fresh;
            } else {
                reconcile(existingClass, fresh);
                target = existingClass;
            }
            // 复用原 CU：保留用户 import；包与类以 config 为准（addClass 会替换同名类）
            existingCu.setPackage(hyc.codegen.tree.Package.of(ctx.packageName()));
            target.setPkg(hyc.codegen.tree.Package.of(ctx.packageName()));
            cu = existingCu;
        }

        gctx.applyInterceptors(target, ctx);

        cu.addClass(target);
        for (hyc.codegen.tree.Import imp : extraImports(ctx, gctx)) {
            cu.addImport(imp);
        }
        String code = JavaCodegen.generateCode(cu);
        writeFile(file.toPath(), ctx, gctx, code, className);
    }

    /**
     * 额外 import（方法体字符串引用的类型不会自动收集，由子类在此显式登记，见 PROGRESS.md 决策）。
     */
    protected List<hyc.codegen.tree.Import> extraImports(TableContext ctx, GenerationContext gctx) {
        return java.util.Collections.emptyList();
    }

    /** artifact 类型名（对应 config {@code artifacts.<kind>}）。 */
    @Override
    public abstract String kind();

    /** 从模型构建期望成员（所有成员会由基类自动打上 {@code @Generated}）。 */
    protected abstract void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx);

    /** 是否应该生成此文件；返回 false 时若文件已存在则删除（如 enum 文件随 enum 列消失而清理）。 */
    protected boolean shouldGenerate(TableContext ctx) {
        return true;
    }

    /** 构建期望模型并标记全部成员。 */
    private Class buildFresh(TableContext ctx, GenerationContext gctx, String className,
            java.util.function.Consumer<Class.Builder> builderFn) {
        Class.Builder builder = Class.builder()
                .name(className)
                .pkg(ctx.packageName())
                .modifiers(Modifier.PUBLIC);
        builderFn.accept(builder);
        Class fresh = builder.build();
        markGenerated(fresh);
        return fresh;
    }

    /** 成员级 reconcile：把现有类的 {@code @Generated} 成员对齐到期望模型。 */
    private void reconcile(Class target, Class fresh) {
        reconcileFields(target, fresh.getFields(), generatedFields(target));
        reconcileMethods(target, fresh.getMethods(), generatedMethods(target));
    }

    private void reconcileFields(Class target, List<Variable> expected, List<Variable> generated) {
        for (Variable exp : expected) {
            Variable match = findField(generated, exp.getName().toString());
            if (match == null) {
                target.addField(exp);
                continue;
            }
            generated.remove(match);
            replaceIfSignatureChanged(target, exp, match);
        }
        for (Variable stale : generated) {
            target.removeField(stale);
        }
    }

    private void reconcileMethods(Class target, List<Method> expected, List<Method> generated) {
        for (Method exp : expected) {
            Method match = findMethod(generated, exp.getName().toString());
            if (match == null) {
                target.addMethod(exp);
                continue;
            }
            generated.remove(match);
            replaceIfSignatureChanged(target, exp, match);
        }
        for (Method stale : generated) {
            target.removeMethod(stale);
        }
    }

    private void replaceIfSignatureChanged(Class target, Variable expected, Variable match) {
        if (signature(expected).equals(signature(match))) {
            return;
        }
        target.removeField(match);
        target.addField(expected);
    }

    private void replaceIfSignatureChanged(Class target, Method expected, Method match) {
        if (signature(expected).equals(signature(match))) {
            return;
        }
        target.removeMethod(match);
        target.addMethod(expected);
    }

    private List<Variable> generatedFields(Class cls) {
        List<Variable> generated = new ArrayList<>();
        for (Variable field : cls.getFields()) {
            if (GeneratedSupport.isGenerated(field)) {
                generated.add(field);
            }
        }
        return generated;
    }

    private List<Method> generatedMethods(Class cls) {
        List<Method> generated = new ArrayList<>();
        for (Method method : cls.getMethods()) {
            if (GeneratedSupport.isGenerated(method)) {
                generated.add(method);
            }
        }
        return generated;
    }

    private Variable findField(List<Variable> fields, String name) {
        for (Variable field : fields) {
            if (name.equals(field.getName().toString())) {
                return field;
            }
        }
        return null;
    }

    private Method findMethod(List<Method> methods, String name) {
        for (Method method : methods) {
            if (name.equals(method.getName().toString())) {
                return method;
            }
        }
        return null;
    }

    /** 字段签名：类型。 */
    private String signature(Variable field) {
        return String.valueOf(field.getType());
    }

    /** 方法签名：返回类型 + 参数类型序列 + 方法体（空白归一化，体变更也能触发替换）。 */
    private String signature(Method method) {
        StringBuilder sb = new StringBuilder();
        if (method.getReturnType() != null) {
            sb.append(method.getReturnType());
        }
        sb.append('(');
        for (VariableTree p : method.getParameters()) {
            sb.append(p.getType()).append(',');
        }
        sb.append(')');
        if (method.getBody() != null) {
            sb.append(':')
                    .append(method.getBody().toString().replaceAll("\\s+", " "));
        }
        return sb.toString();
    }

    /** 给期望模型的全部成员打上生成标记。 */
    private void markGenerated(Class fresh) {
        for (Variable f : fresh.getFields()) {
            GeneratedSupport.mark(f);
        }
        for (Method m : fresh.getMethods()) {
            GeneratedSupport.mark(m);
        }
    }

    /** 解析现有文件；不存在或解析失败返回 null（解析失败已记警告）。 */
    private CompileUnit parse(File file) {
        if (!file.isFile()) {
            return null;
        }
        try {
            List<CompileUnit> units = parser.parse(file);
            return units.isEmpty() ? null : units.get(0);
        } catch (Exception e) {
            // 契约：解析失败不碰文件，报告错误；--force 由 CLI 层处理
            throw new IllegalStateException("解析失败: " + file + "（" + e.getMessage() + "），未修改该文件", e);
        }
    }

    private void writeFile(Path file, TableContext ctx, GenerationContext gctx, String code, String className) {
        try {
            ChangeStatus status = FileWriter.writeIfChanged(file, code);
            gctx.getReport().add(file, status, ctx.getArtifactKind() + " " + className);
        } catch (IOException e) {
            throw new IllegalStateException("写文件失败: " + file, e);
        }
    }

    /** 不再需要该文件时删除（drop 的删除由 CodeGenerator 处理，此处处理 shouldGenerate=false 的清理）。 */
    protected void deleteIfExists(TableContext ctx, GenerationContext gctx) {
        try {
            if (FileWriter.deleteIfExists(ctx.javaFile(gctx.getProjectRoot()))) {
                gctx.getReport()
                        .add(ctx.javaFile(gctx.getProjectRoot()), ChangeStatus.DELETED,
                                ctx.getArtifactKind() + " " + ctx.className() + "（不再适用）");
            }
        } catch (IOException e) {
            throw new IllegalStateException("删除文件失败: " + ctx.javaFile(gctx.getProjectRoot()), e);
        }
    }

}
