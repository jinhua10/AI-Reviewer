package top.yumbo.ai.s3;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@EnableAutoConfiguration
public class AWSS3Application {

    public static void main(String[] args) {
        log.info("Starting AWS S3 Application with args: {}", String.join(", ", args));
        log.info("Classpath: {}", System.getProperty("java.class.path"));

        SpringApplication app = new SpringApplication(AWSS3Application.class);
        app.setAdditionalProfiles(); // 使用默认 profile
        app.run(args);
    }
}
