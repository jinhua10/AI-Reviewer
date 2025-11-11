package com.example.calculator;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 工具类
 * 提供通用的辅助方法
 */
public class Utils {

    /**
     * 格式化结果输出
     */
    public static String formatResult(String message, int value) {
        return String.format("[%s] %s: %d", getCurrentTime(), message, value);
    }

    /**
     * 获取当前时间
     */
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    /**
     * 判断是否为偶数
     */
    public static boolean isEven(int n) {
        return n % 2 == 0;
    }

    /**
     * 判断是否为质数
     */
    public static boolean isPrime(int n) {
        if (n <= 1) {
            return false;
        }
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }
}

