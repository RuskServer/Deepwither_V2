package com.ruskserver.deepwither_V2.core.di.container;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Module;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.exceptions.CircularDependencyException;
import com.ruskserver.deepwither_V2.core.di.exceptions.DependencyResolutionException;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class DIContainer {
    
    private final Map<Class<?>, Object> instances = new LinkedHashMap<>();
    private final LinkedHashSet<Class<?>> resolvingClasses = new LinkedHashSet<>();
    private final Map<Class<?>, List<Class<?>>> dependencyGraph = new HashMap<>();
    private final Set<Class<?>> scannedClasses = new HashSet<>();
    
    private Logger logger;
    private boolean debugMode = false;

    public DIContainer() {
        // Register the container itself so it can be injected if needed
        registerInstance(DIContainer.class, this);
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Manually registers a specific instance for a class type.
     */
    public <T> void registerInstance(Class<T> type, T instance) {
        instances.put(type, instance);
        if (!dependencyGraph.containsKey(type)) {
            dependencyGraph.put(type, new ArrayList<>());
        }
    }

    /**
     * Gets or creates an instance of the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        if (instances.containsKey(type)) {
            return (T) instances.get(type);
        }

        // Circular dependency check
        if (resolvingClasses.contains(type)) {
            List<String> path = new ArrayList<>();
            for (Class<?> clazz : resolvingClasses) {
                path.add(clazz.getSimpleName());
            }
            path.add(type.getSimpleName());
            String trace = String.join(" -> ", path);
            throw new CircularDependencyException("Circular dependency detected! Resolution path: " + trace);
        }

        if (debugMode && logger != null) {
            logger.info("[DI-Container] Resolving dependencies for: " + type.getName());
        }

        resolvingClasses.add(type);

        try {
            T instance = createInstance(type);
            instances.put(type, instance);
            
            if (debugMode && logger != null) {
                logger.info("[DI-Container] Instantiated: " + type.getName());
            }
            
            return instance;
        } finally {
            resolvingClasses.remove(type);
        }
    }

    /**
     * Creates an instance by looking for an @Inject constructor or a default constructor.
     */
    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> type) {
        if (type.isInterface()) {
            throw new DependencyResolutionException("Cannot instantiate an interface: " + type.getName() + ". Please bind an implementation or use concrete classes.");
        }

        Constructor<?>[] constructors = type.getDeclaredConstructors();
        Constructor<?> targetConstructor = null;

        // Look for @Inject
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                targetConstructor = constructor;
                break;
            }
        }

        // If no @Inject, fallback to default constructor (no-args)
        if (targetConstructor == null) {
            try {
                targetConstructor = type.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new DependencyResolutionException("No @Inject constructor found and no default constructor found for: " + type.getName());
            }
        }

        targetConstructor.setAccessible(true);
        java.lang.reflect.Parameter[] parametersInfo = targetConstructor.getParameters();
        Object[] parameters = new Object[parametersInfo.length];
        
        List<Class<?>> dependencies = new ArrayList<>();

        for (int i = 0; i < parametersInfo.length; i++) {
            java.lang.reflect.Parameter param = parametersInfo[i];
            Class<?> paramType = param.getType();

            if (List.class.isAssignableFrom(paramType)) {
                // Handle List<T> injection
                Type genericType = param.getParameterizedType();
                if (genericType instanceof ParameterizedType) {
                    Type[] actualArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (actualArgs.length > 0 && actualArgs[0] instanceof Class<?>) {
                        Class<?> elementClass = (Class<?>) actualArgs[0];
                        parameters[i] = resolveAll(elementClass);
                        // Add all implementations as dependencies for the graph
                        for (Object impl : (List<?>) parameters[i]) {
                            dependencies.add(impl.getClass());
                        }
                        continue;
                    }
                }
            }

            dependencies.add(paramType);
            parameters[i] = resolve(paramType); // Recursively resolve dependencies
        }
        
        dependencyGraph.put(type, dependencies);

        try {
            return (T) targetConstructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new DependencyResolutionException("Failed to instantiate class: " + type.getName(), e);
        }
    }

    /**
     * Resolves all instances that implement or extend the specified type.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> resolveAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        
        // Find all scanned classes that implement the interface or extend the class
        for (Class<?> clazz : scannedClasses) {
            if (type.isAssignableFrom(clazz) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                result.add((T) resolve(clazz));
            }
        }
        
        // Also check manually registered instances
        for (Map.Entry<Class<?>, Object> entry : instances.entrySet()) {
            if (type.isAssignableFrom(entry.getKey()) && !result.contains(entry.getValue())) {
                result.add((T) entry.getValue());
            }
        }
        
        return result;
    }

    /**
     * Scans a package and automatically resolves and registers all annotated classes
     */
    public void scanAndRegister(ClassLoader classLoader, String basePackage) {
        Set<Class<?>> targetClasses = new HashSet<>();
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Component.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Service.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Repository.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Module.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Command.class, logger, debugMode));

        scannedClasses.addAll(targetClasses);

        for (Class<?> clazz : targetClasses) {
            // Force resolution and instantiation of scanned classes
            resolve(clazz);
        }
    }

    /**
     * 依存グラフをトポロジカルソート（Kahnのアルゴリズム）し、
     * 起動順（依存先 → 依存元）のインスタンスリストを返します。
     */
    private List<Object> computeStartOrder() {
        // dependencyGraph の中から、instances として登録されているクラスのみを対象にする
        Set<Class<?>> nodes = new HashSet<>(dependencyGraph.keySet());
        nodes.retainAll(instances.keySet());

        // 逆方向グラフを構築 (A が B に依存 → 逆グラフでは B → A)
        Map<Class<?>, List<Class<?>>> reverseGraph = new HashMap<>();
        for (Class<?> node : nodes) {
            reverseGraph.put(node, new ArrayList<>());
        }
        for (Class<?> node : nodes) {
            for (Class<?> dep : dependencyGraph.getOrDefault(node, Collections.emptyList())) {
                if (nodes.contains(dep)) {
                    reverseGraph.get(dep).add(node);
                }
            }
        }

        // 入次数（逆グラフ上）の計算
        Map<Class<?>, Integer> inDegree = new HashMap<>();
        for (Class<?> node : nodes) {
            inDegree.put(node, 0);
        }
        for (Class<?> node : nodes) {
            for (Class<?> neighbor : reverseGraph.getOrDefault(node, Collections.emptyList())) {
                inDegree.merge(neighbor, 1, Integer::sum);
            }
        }

        // Kahnのアルゴリズム (BFS)
        Queue<Class<?>> queue = new ArrayDeque<>();
        for (Class<?> node : nodes) {
            if (inDegree.get(node) == 0) {
                queue.add(node);
            }
        }

        List<Object> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            Object instance = instances.get(current);
            if (instance != null) {
                ordered.add(instance);
            }
            for (Class<?> neighbor : reverseGraph.getOrDefault(current, Collections.emptyList())) {
                int newDegree = inDegree.merge(neighbor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // トポロジカルソートで処理できなかったノードが残った場合（サイクル）はそのまま追加
        if (ordered.size() < nodes.size()) {
            if (logger != null) {
                logger.warning("[DI] Topological sort incomplete - possible cycle detected. Remaining nodes appended in arbitrary order.");
            }
            Set<Object> alreadyAdded = new HashSet<>(ordered);
            for (Class<?> node : nodes) {
                Object instance = instances.get(node);
                if (instance != null && !alreadyAdded.contains(instance)) {
                    ordered.add(instance);
                }
            }
        }

        return ordered;
    }

    /**
     * Calls start() on all Startable instances in dependency-topological order.
     * (Dependency-first: e.g. DatabaseManager starts before PlayerDataRepository)
     */
    public void startAll() {
        List<Object> ordered = computeStartOrder();
        if (logger != null) {
            logger.info("[DI] Starting " + ordered.stream()
                    .filter(o -> o instanceof Startable)
                    .map(o -> o.getClass().getSimpleName())
                    .collect(Collectors.joining(" -> ")));
        }
        for (Object instance : ordered) {
            if (instance instanceof Startable) {
                ((Startable) instance).start();
            }
        }
    }

    /**
     * Calls stop() on all Stoppable instances in reverse dependency order.
     * (Dependent-first: e.g. PlayerDataRepository stops before DatabaseManager)
     */
    public void stopAll() {
        List<Object> ordered = computeStartOrder();
        Collections.reverse(ordered);
        if (logger != null) {
            logger.info("[DI] Stopping " + ordered.stream()
                    .filter(o -> o instanceof Stoppable)
                    .map(o -> o.getClass().getSimpleName())
                    .collect(Collectors.joining(" -> ")));
        }
        for (Object instance : ordered) {
            if (instance instanceof Stoppable) {
                try {
                    ((Stoppable) instance).stop();
                } catch (Exception e) {
                    if (logger != null) {
                        logger.warning("Error stopping component " + instance.getClass().getName() + ": " + e.getMessage());
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Returns a collection of all registered instances.
     */
    public Collection<Object> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }
    
    /**
     * Prints the entire dependency tree to the configured logger.
     */
    public void printDependencyTree() {
        if (logger == null) return;
        
        logger.info("=== [DI Tree] Registered Components ===");
        
        // Find root nodes (classes that no one depends on)
        Set<Class<?>> allDeps = new HashSet<>();
        for (List<Class<?>> deps : dependencyGraph.values()) {
            allDeps.addAll(deps);
        }
        
        Set<Class<?>> roots = new HashSet<>(dependencyGraph.keySet());
        roots.removeAll(allDeps);
        
        // Sort roots alphabetically
        List<Class<?>> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort(Comparator.comparing(Class::getName));
        
        for (Class<?> root : sortedRoots) {
            printNode(root, "", true, new HashSet<>());
        }
        logger.info("=======================================");
    }
    
    private void printNode(Class<?> node, String prefix, boolean isTail, Set<Class<?>> visited) {
        if (logger == null) return;
        
        String nodeName = node.getSimpleName();
        // Prevent infinite printing loop in case of unhandled circular refs (though should be blocked earlier)
        if (visited.contains(node)) {
            logger.info(prefix + (isTail ? "└── " : "├── ") + nodeName + " (Circular Reference)");
            return;
        }
        visited.add(node);
        
        logger.info(prefix + (isTail ? "└── " : "├── ") + nodeName);
        
        List<Class<?>> deps = dependencyGraph.getOrDefault(node, new ArrayList<>());
        for (int i = 0; i < deps.size() - 1; i++) {
            printNode(deps.get(i), prefix + (isTail ? "    " : "│   "), false, new HashSet<>(visited));
        }
        if (deps.size() > 0) {
            printNode(deps.get(deps.size() - 1), prefix + (isTail ? "    " : "│   "), true, new HashSet<>(visited));
        }
    }
}
