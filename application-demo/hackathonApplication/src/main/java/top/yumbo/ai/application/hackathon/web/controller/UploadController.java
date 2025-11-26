package top.yumbo.ai.application.hackathon.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.yumbo.ai.application.hackathon.web.service.AccountService;
import top.yumbo.ai.application.hackathon.web.service.FileUploadService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Web控制器 - 团队文件上传接口
 */
@Slf4j
@Controller
public class UploadController {

    private static final String ACCESS_CODE_COOKIE = "TEAM_ACCESS_CODE";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7天

    @Autowired
    private AccountService accountService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 登录页面
     */
    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        // 检查是否已经认证
        String accessCode = getAccessCodeFromCookie(request);
        if (accessCode != null && accountService.validateAccessCode(accessCode) != null) {
            return "redirect:/upload";
        }
        return "login";
    }

    /**
     * 处理登录请求
     */
    @PostMapping("/login")
    public String login(@RequestParam("accessCode") String accessCode,
                       HttpServletResponse response,
                       RedirectAttributes redirectAttributes) {

        AccountService.TeamAccount account = accountService.validateAccessCode(accessCode);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "访问码无效，请重试。");
            return "redirect:/";
        }

        // 设置Cookie
        Cookie cookie = new Cookie(ACCESS_CODE_COOKIE, accessCode);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        log.info("团队登录: {} ({})", account.getTeamId(), account.getLeadEmail());
        return "redirect:/upload";
    }

    /**
     * 上传页面
     */
    @GetMapping("/upload")
    public String uploadPage(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        String accessCode = getAccessCodeFromCookie(request);
        AccountService.TeamAccount account = accountService.validateAccessCode(accessCode);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录。");
            return "redirect:/";
        }

        model.addAttribute("teamId", account.getTeamId());
        model.addAttribute("leadEmail", account.getLeadEmail());

        try {
            String[] zipFiles = fileUploadService.listZipFiles(account.getTeamId());
            model.addAttribute("uploadedFiles", zipFiles);
            model.addAttribute("hasDone", fileUploadService.hasDoneFile(account.getTeamId()));
        } catch (Exception e) {
            log.error("列出文件失败，团队: {}", account.getTeamId(), e);
            model.addAttribute("uploadedFiles", new String[0]);
            model.addAttribute("hasDone", false);
        }

        return "upload";
    }

    /**
     * 处理文件上传
     */
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam(value = "file") MultipartFile file,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {

        String accessCode = getAccessCodeFromCookie(request);
        AccountService.TeamAccount account = accountService.validateAccessCode(accessCode);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录。");
            return "redirect:/";
        }

        try {
            fileUploadService.uploadZipFile(account.getTeamId(), file);
            redirectAttributes.addFlashAttribute("success",
                "文件上传成功: " + file.getOriginalFilename());
            log.info("团队 {} 上传文件: {}", account.getTeamId(), file.getOriginalFilename());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "文件上传失败: " + e.getMessage());
            log.error("文件上传失败，团队: {}", account.getTeamId(), e);
        }

        return "redirect:/upload";
    }

    /**
     * 标记提交完成
     */
    @PostMapping("/done")
    public String markDone(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String accessCode = getAccessCodeFromCookie(request);
        AccountService.TeamAccount account = accountService.validateAccessCode(accessCode);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录。");
            return "redirect:/";
        }

        try {
            fileUploadService.createDoneFile(account.getTeamId());
            redirectAttributes.addFlashAttribute("success",
                "提交已标记为完成！您的项目即将被AI评审。");
            log.info("团队 {} 标记提交完成", account.getTeamId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "标记完成失败: " + e.getMessage());
            log.error("创建done.txt失败，团队: {}", account.getTeamId(), e);
        }

        return "redirect:/upload";
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        Cookie cookie = new Cookie(ACCESS_CODE_COOKIE, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        redirectAttributes.addFlashAttribute("success", "退出登录成功。");
        return "redirect:/";
    }

    /**
     * 从Cookie获取访问码
     */
    private String getAccessCodeFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> ACCESS_CODE_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

