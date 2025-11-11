package top.yumbo.ai.reviewer;

public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("开始测试...");

        try {
            System.out.println("创建HackathonReviewer...");
            top.yumbo.ai.reviewer.HackathonReviewer reviewer =
                new top.yumbo.ai.reviewer.HackathonReviewer();
            System.out.println("✅ 成功创建HackathonReviewer");
            reviewer.shutdown();
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("测试完成");
    }
}
