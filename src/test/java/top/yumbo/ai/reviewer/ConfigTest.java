package top.yumbo.ai.reviewer;

public class ConfigTest {
    public static void main(String[] args) {
        System.out.println("=== 配置加载测试 ===");

        try {
            // 测试从类路径加载配置
            System.out.println("测试从类路径加载hackathon-config.yaml...");
            top.yumbo.ai.reviewer.config.Config config =
                top.yumbo.ai.reviewer.config.Config.loadFromFile("hackathon-config.yaml");

            System.out.println("✅ 配置加载成功!");
            System.out.println("AI服务提供商: " + config.getAiService().getProvider());
            System.out.println("模型: " + config.getAiService().getModel());

        } catch (Exception e) {
            System.err.println("❌ 配置加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
