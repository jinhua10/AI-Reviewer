package com.example.calculator;

/**
 * 计算器类
 * 实现基本的四则运算功能
 */
public class Calculator {

    /**
     * 加法运算
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * 减法运算
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * 乘法运算
     */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * 除法运算
     * @throws ArithmeticException 当除数为0时
     */
    public double divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("除数不能为0");
        }
        return (double) a / b;
    }

    /**
     * 计算平方
     */
    public int square(int n) {
        return n * n;
    }

    /**
     * 计算绝对值
     */
    public int abs(int n) {
        return n < 0 ? -n : n;
    }
}

