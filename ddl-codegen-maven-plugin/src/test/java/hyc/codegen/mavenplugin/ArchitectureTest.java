package hyc.codegen.mavenplugin;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Maven 插件架构规则：薄壳必须走 {@code Codegen} 门面，不得直连 core 的生成层与 tree。
 */
@AnalyzeClasses(packages = {"hyc.codegen.mavenplugin", "hyc.codegen.core", "hyc.codegen.tree"},
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule pluginOnlyUsesCodegenFacade = noClasses()
            .that()
            .resideInAPackage("hyc.codegen.mavenplugin..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "hyc.codegen.core.gen..",
                    "hyc.codegen.tree..");

}
