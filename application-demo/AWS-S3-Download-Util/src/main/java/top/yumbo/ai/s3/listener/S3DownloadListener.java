package top.yumbo.ai.s3.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.yumbo.ai.s3.service.S3DownloadService;

@Component
@Slf4j
public class S3DownloadListener {

    private final S3DownloadService s3DownloadService;

    public S3DownloadListener(S3DownloadService s3DownloadService) {
        this.s3DownloadService = s3DownloadService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始下载S3文件...");
        try {
            s3DownloadService.downloadAllFiles();
        } catch (Exception e) {
            log.error("启动时下载S3文件失败", e);
        }
    }
}

