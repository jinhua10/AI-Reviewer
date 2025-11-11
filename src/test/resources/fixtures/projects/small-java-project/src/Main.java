package com.example.calculator;

/**
 * 计算器应用程序主类
 * 提供基本的计算功能
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== 简单计算器应用 ===");

        Calculator calculator = new Calculator();

        // 测试加法
        int sum = calculator.add(10, 5);
        System.out.println("10 + 5 = " + sum);

        // 测试减法
        int diff = calculator.subtract(10, 5);
        System.out.println("10 - 5 = " + diff);

        // 测试乘法
        int product = calculator.multiply(10, 5);
        System.out.println("10 * 5 = " + product);

        // 测试除法
        try {
            double quotient = calculator.divide(10, 5);
            System.out.println("10 / 5 = " + quotient);
        } catch (ArithmeticException e) {
            System.err.println("错误: " + e.getMessage());
        }

        // 使用工具类
        String formatted = Utils.formatResult("计算完成", sum);
        System.out.println(formatted);
    }
}

