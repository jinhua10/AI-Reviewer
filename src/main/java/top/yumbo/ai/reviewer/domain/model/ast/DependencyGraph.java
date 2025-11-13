package top.yumbo.ai.reviewer.domain.model.ast;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 依赖图
 *
 * 表示类之间的依赖关系
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
public class DependencyGraph {

    @Builder.Default
    private Map<String, Set<String>> dependencies = new HashMap<>();  // 类 -> 依赖的类集合

    /**
     * 添加依赖关系
     */
    public void addDependency(String from, String to) {
        dependencies.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    /**
     * 获取指定类的依赖
     */
    public Set<String> getDependencies(String className) {
        return dependencies.getOrDefault(className, new HashSet<>());
    }

    /**
     * 获取依赖数量（出度）
     */
    public int getDependencyCount(String className) {
        return getDependencies(className).size();
    }

    /**
     * 获取被依赖数量（入度）
     */
    public int getDependentCount(String className) {
        return (int) dependencies.values().stream()
            .filter(deps -> deps.contains(className))
            .count();
    }

    /**
     * 获取总依赖数量
     */
    public int getTotalDependencies() {
        return dependencies.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * 检测是否存在循环依赖
     */
    public boolean hasCyclicDependencies() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String className : dependencies.keySet()) {
            if (!visited.contains(className)) {
                if (detectCycle(className, visited, recursionStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检测指定类的循环依赖
     */
    public boolean hasCyclicDependency(String className) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        return detectCycle(className, visited, recursionStack);
    }

    private boolean detectCycle(String node, Set<String> visited, Set<String> recursionStack) {
        visited.add(node);
        recursionStack.add(node);

        Set<String> deps = getDependencies(node);
        for (String dep : deps) {
            if (!visited.contains(dep)) {
                if (detectCycle(dep, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(dep)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }

    /**
     * 生成简单的文本描述
     */
    public String toSimpleString() {
        StringBuilder sb = new StringBuilder();

        // 找出核心依赖节点
        Map<String, Integer> dependencyCounts = new HashMap<>();
        dependencies.forEach((from, tos) -> {
            dependencyCounts.put(from, tos.size());
        });

        // 按依赖数量排序，显示前10个
        dependencyCounts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .forEach(entry -> {
                sb.append(String.format("- %s: 依赖%d个类\n",
                    entry.getKey(), entry.getValue()));
            });

        return sb.toString();
    }
}

