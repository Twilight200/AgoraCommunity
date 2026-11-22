package com.example.agora;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PlatformArchitectureTests {

    @Test
    void platformMustNotDependOnBusinessModules() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.example.agora");

        noClasses()
                .that().resideInAPackage("com.example.agora.platform..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.example.agora.identity..",
                        "com.example.agora.forum..",
                        "com.example.agora.reaction..",
                        "com.example.agora.reading..",
                        "com.example.agora.social..",
                        "com.example.agora.feed..",
                        "com.example.agora.ranking..",
                        "com.example.agora.notification.."
                )
                .check(classes);
    }
}