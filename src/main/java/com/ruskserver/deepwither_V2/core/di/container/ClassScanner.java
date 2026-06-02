package com.ruskserver.deepwither_V2.core.di.container;

import com.ruskserver.deepwither_V2.core.di.annotations.Ignore;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ClassScanner {

    /**
     * Scans the specified package once and returns classes annotated with any of the given annotations.
     *
     * @param classLoader The ClassLoader to use. Usually the PluginClassLoader.
     * @param packageName The base package name to scan.
     * @param annotations The annotations to look for.
     * @param logger Optional logger for debug output.
     * @param debugMode If true, logs loading errors and discovered classes.
     * @return A set of classes annotated with any specified annotation.
     */
    public static Set<Class<?>> findClassesWithAnyAnnotation(
            ClassLoader classLoader,
            String packageName,
            Collection<Class<? extends Annotation>> annotations,
            Logger logger,
            boolean debugMode
    ) {
        Set<Class<?>> classes = new LinkedHashSet<>();

        try (ScanResult scanResult = new ClassGraph()
                .overrideClassLoaders(classLoader)
                .acceptPackages(packageName)
                .ignoreClassVisibility()
                .enableClassInfo()
                .enableAnnotationInfo()
                .scan()) {

            for (Class<? extends Annotation> annotation : annotations) {
                String annotationName = annotation.getName();
                for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(annotationName)) {
                    if (classInfo.hasAnnotation(Ignore.class.getName())) {
                        if (debugMode && logger != null) {
                            logger.info("[DI-Scanner] Skipped " + classInfo.getName() + " (Reason: @Ignore)");
                        }
                        continue;
                    }

                    try {
                        Class<?> clazz = Class.forName(classInfo.getName(), false, classLoader);
                        if (classes.add(clazz) && debugMode && logger != null) {
                            logger.info("[DI-Scanner] Found @" + annotation.getSimpleName() + ": " + clazz.getName());
                        }
                    } catch (NoClassDefFoundError | ClassNotFoundException | ExceptionInInitializerError e) {
                        if (debugMode && logger != null) {
                            logger.warning("[DI-Scanner] Failed to load class " + classInfo.getName() + " - " + e);
                        }
                    }
                }
            }
        }

        return classes;
    }

    /**
     * Compatibility wrapper for callers that still scan for a single annotation.
     */
    public static Set<Class<?>> findClassesWithAnnotation(
            ClassLoader classLoader,
            String packageName,
            Class<? extends Annotation> annotation,
            Logger logger,
            boolean debugMode
    ) {
        return findClassesWithAnyAnnotation(classLoader, packageName, Set.of(annotation), logger, debugMode);
    }
}
