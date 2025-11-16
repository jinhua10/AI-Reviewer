package top.yumbo.ai.application.hackathon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Main application class for AI Reviewer
 */
@Slf4j
@SpringBootApplication
@EnableAutoConfiguration
public class HackathonApplication {

    public static void main(String[] args) {
        // 启用配置文件调试
        System.setProperty("logging.level.org.springframework.boot.context.config", "DEBUG");
        System.setProperty("logging.level.org.springframework.core.env", "DEBUG");

        log.info("Starting application with args: {}", String.join(", ", args));
        log.info("Classpath: {}", System.getProperty("java.class.path"));

        SpringApplication app = new SpringApplication(HackathonApplication.class);
        // 确保加载 application-example.yml
        app.setAdditionalProfiles(); // 使用默认 profile
        app.run(args);
    }


}
