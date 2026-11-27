package com.example.MyProject.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleBoundaryArchitectureTest {

    private static final Path MAIN_ROOT = Path.of("src/main/java/com/example/MyProject");
    private static final Set<String> MODULES = Set.of("AUTH", "PRODUCT", "ORDER", "REVIEW");
    private static final Pattern REPO_IMPORT_PATTERN = Pattern.compile(
            "^\\s*import\\s+com\\.example\\.MyProject\\.(AUTH|PRODUCT|ORDER|REVIEW)\\.repository\\.[A-Za-z0-9_.*]+;",
            Pattern.MULTILINE
    );

    @Test
    void domainModulesNeverImportAnotherModulesRepositories() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String module : MODULES) {
            Path moduleDir = MAIN_ROOT.resolve(module);
            try (Stream<Path> walk = Files.walk(moduleDir)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        Matcher matcher = REPO_IMPORT_PATTERN.matcher(content);
                        while (matcher.find()) {
                            String targetModule = matcher.group(1);
                            if (!module.equals(targetModule)) {
                                violations.add(module + " -> " + file + " imports " + targetModule + " repository: " + matcher.group().trim());
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        assertThat(violations)
                .as("Cross-module repository imports must be replaced by public Service contracts")
                .isEmpty();
    }

    @Test
    void allServiceImplClassesFollowStandardServiceImplNamingConvention() throws IOException {
        List<String> invalidNames = new ArrayList<>();

        for (String module : MODULES) {
            Path implDir = MAIN_ROOT.resolve(module).resolve("service").resolve("impl");
            assertThat(Files.isDirectory(implDir))
                    .as("Module %s should use service/impl directory", module)
                    .isTrue();

            try (Stream<Path> files = Files.list(implDir)) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                    String fileName = file.getFileName().toString();
                    if (!fileName.endsWith("ServiceImpl.java") || fileName.matches("^I[A-Z].*")) {
                        invalidNames.add(module + "/service/impl/" + fileName);
                    }
                });
            }
        }

        assertThat(invalidNames)
                .as("Every implementation under service/impl/ must be named <Name>ServiceImpl.java")
                .isEmpty();
    }

    @Test
    void controllersNeverInjectRepositoriesDirectly() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String module : MODULES) {
            Path controllerDir = MAIN_ROOT.resolve(module).resolve("controller");
            try (Stream<Path> files = Files.list(controllerDir)) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        if (REPO_IMPORT_PATTERN.matcher(content).find()) {
                            violations.add(file.toString());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        assertThat(violations).isEmpty();
    }
}
