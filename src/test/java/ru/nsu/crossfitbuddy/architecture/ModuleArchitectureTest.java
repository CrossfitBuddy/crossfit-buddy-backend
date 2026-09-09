package ru.nsu.crossfitbuddy.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "ru.nsu.crossfitbuddy", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleArchitectureTest {
    @ArchTest
    static final ArchRule moduleDependencies = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Auth").definedBy("..auth..")
            .layer("Core").definedBy("..core..")
            .layer("Shared").definedBy("..shared..")
            .layer("Infrastructure").definedBy("..config..", "..common..")
            .whereLayer("Auth").mayNotBeAccessedByAnyLayer()
            .whereLayer("Core").mayOnlyBeAccessedByLayers("Auth", "Infrastructure")
            .whereLayer("Shared").mayOnlyBeAccessedByLayers("Auth", "Core", "Infrastructure");
}
