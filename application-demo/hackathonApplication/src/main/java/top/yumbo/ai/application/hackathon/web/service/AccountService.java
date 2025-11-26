package top.yumbo.ai.application.hackathon.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing team accounts from account.csv
 */
@Slf4j
@Service
public class AccountService {

    private final Map<String, TeamAccount> accessCodeToAccount = new HashMap<>();
    private String accountCsvPath;

    /**
     * Load accounts from CSV file
     */
    public void loadAccounts(String projectRootPath) {
        this.accountCsvPath = projectRootPath + "/account.csv";
        Path csvPath = Paths.get(accountCsvPath);

        if (!Files.exists(csvPath)) {
            log.warn("Account CSV file not found: {}", accountCsvPath);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String teamId = parts[0].trim();
                    String leadEmail = parts[1].trim();
                    String accessCode = parts[2].trim();

                    TeamAccount account = new TeamAccount(teamId, leadEmail, accessCode);
                    accessCodeToAccount.put(accessCode, account);
                    log.debug("Loaded account: teamId={}, email={}", teamId, leadEmail);
                }
            }

            log.info("Loaded {} team accounts from {}", accessCodeToAccount.size(), accountCsvPath);

        } catch (IOException e) {
            log.error("Failed to load account CSV: {}", accountCsvPath, e);
        }
    }

    /**
     * Validate access code and return team account
     */
    public TeamAccount validateAccessCode(String accessCode) {
        if (accessCode == null || accessCode.trim().isEmpty()) {
            return null;
        }
        return accessCodeToAccount.get(accessCode.trim());
    }

    /**
     * Get team account by access code
     */
    public TeamAccount getAccount(String accessCode) {
        return accessCodeToAccount.get(accessCode);
    }

    /**
     * Check if accounts are loaded
     */
    public boolean isLoaded() {
        return !accessCodeToAccount.isEmpty();
    }

    /**
     * Team account data class
     */
    public static class TeamAccount {
        private final String teamId;
        private final String leadEmail;
        private final String accessCode;

        public TeamAccount(String teamId, String leadEmail, String accessCode) {
            this.teamId = teamId;
            this.leadEmail = leadEmail;
            this.accessCode = accessCode;
        }

        public String getTeamId() {
            return teamId;
        }

        public String getLeadEmail() {
            return leadEmail;
        }

        public String getAccessCode() {
            return accessCode;
        }
    }
}

