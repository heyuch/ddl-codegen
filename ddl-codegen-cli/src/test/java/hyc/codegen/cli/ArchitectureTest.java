package hyc.codegen.cli;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * CLI 架构规则：薄壳必须走 {@code Codegen} 门面，不得直连 core 的生成层与 tree。
 */
@AnalyzeClasses(packages = {"hyc.codegen.cli", "hyc.codegen.core", "hyc.codegen.tree"},
        importOptions = ImportOption.DoNotIncludeTests.class)
// ArchUnit 规则字段命名惯例为小驼峰，与 checkstyle 常量规则冲突
@SuppressWarnings("ConstantName")
class ArchitectureTest {

    @ArchTest
    static final ArchRule cliOnlyUsesCodegenFacade = noClasses()
            .that()
            .resideInAPackage("hyc.codegen.cli..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "hyc.codegen.core.gen..",
                    "hyc.codegen.tree..");

}
