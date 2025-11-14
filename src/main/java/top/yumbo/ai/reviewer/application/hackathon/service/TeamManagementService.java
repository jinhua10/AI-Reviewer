package top.yumbo.ai.reviewer.application.hackathon.service;

import top.yumbo.ai.reviewer.domain.hackathon.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 团队管理服务
 *
 * 负责团队和项目的创建、查询、管理等功能
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class TeamManagementService {

    // 内存存储（实际应用中应使用数据库）
    private final Map<String, HackathonProject> projects = new ConcurrentHashMap<>();
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<String, Participant> participants = new ConcurrentHashMap<>();

    /**
     * 创建黑客松项目
     *
     * @param name 项目名称
     * @param description 项目描述
     * @param team 团队
     * @return 创建的项目
     */
    public HackathonProject createProject(String name, String description, Team team) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (team == null) {
            throw new IllegalArgumentException("团队不能为空");
        }

        // 确保团队已注册
        if (!teams.containsKey(team.getId())) {
            registerTeam(team);
        }

        HackathonProject project = HackathonProject.builder()
            .name(name)
            .description(description)
            .team(team)
            .build();

        projects.put(project.getId(), project);
        return project;
    }

    /**
     * 注册团队
     *
     * @param team 团队
     * @return 注册的团队
     */
    public Team registerTeam(Team team) {
        if (team == null || !team.isValid()) {
            throw new IllegalArgumentException("团队信息无效");
        }

        // 检查团队名称是否重复
        boolean nameExists = teams.values().stream()
            .anyMatch(t -> t.getName().equals(team.getName()));

        if (nameExists) {
            throw new IllegalArgumentException("团队名称已存在: " + team.getName());
        }

        // 注册所有成员
        for (Participant member : team.getMembers()) {
            if (!participants.containsKey(member.getId())) {
                participants.put(member.getId(), member);
            }
        }

        teams.put(team.getId(), team);
        return team;
    }

    /**
     * 注册参与者
     *
     * @param participant 参与者
     * @return 注册的参与者
     */
    public Participant registerParticipant(Participant participant) {
        if (participant == null || !participant.isValid()) {
            throw new IllegalArgumentException("参与者信息无效");
        }

        // 检查邮箱是否重复
        boolean emailExists = participants.values().stream()
            .anyMatch(p -> p.getEmail().equals(participant.getEmail()));

        if (emailExists) {
            throw new IllegalArgumentException("邮箱已被注册: " + participant.getEmail());
        }

        participants.put(participant.getId(), participant);
        return participant;
    }

    /**
     * 提交代码
     *
     * @param projectId 项目ID
     * @param githubUrl GitHub/Gitee URL
     * @param branch Git 分支
     * @param submitter 提交者
     * @return 更新后的项目
     */
    public HackathonProject submitCode(
            String projectId,
            String githubUrl,
            String branch,
            Participant submitter) {

        HackathonProject project = getProjectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }

        // 验证提交者是团队成员
        if (project.getTeam().memberOf(submitter)) {
            throw new IllegalArgumentException("只有团队成员才能提交代码");
        }

        // 验证 GitHub/Gitee URL 格式
        if (!isValidGitHubUrl(githubUrl)) {
            throw new IllegalArgumentException("无效的 GitHub/Gitee URL 格式");
        }

        // 创建提交记录
        Submission submission = Submission.builder()
            .githubUrl(githubUrl)
            .gitBranch(branch != null ? branch : "main")
            .submitter(submitter)
            .build();

        // 添加到项目
        project.addSubmission(submission);

        return project;
    }

    /**
     * 提交代码（带 commit hash）
     *
     * @param projectId 项目ID
     * @param githubUrl GitHub/Gitee URL
     * @param branch Git 分支
     * @param commitHash commit 哈希
     * @param submitter 提交者
     * @return 更新后的项目
     */
    public HackathonProject submitCodeWithCommit(
            String projectId,
            String githubUrl,
            String branch,
            String commitHash,
            Participant submitter) {

        HackathonProject project = getProjectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }

        // 验证提交者是团队成员
        if (project.getTeam().memberOf(submitter)) {
            throw new IllegalArgumentException("只有团队成员才能提交代码");
        }

        // 验证 GitHub/Gitee URL 格式
        if (!isValidGitHubUrl(githubUrl)) {
            throw new IllegalArgumentException("无效的 GitHub/Gitee URL 格式");
        }

        // 创建提交记录
        Submission submission = Submission.builder()
            .githubUrl(githubUrl)
            .gitBranch(branch != null ? branch : "main")
            .commitHash(commitHash)
            .submitter(submitter)
            .build();

        // 添加到项目
        project.addSubmission(submission);

        return project;
    }

    /**
     * 验证 GitHub/Gitee URL 格式
     */
    private boolean isValidGitHubUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        // 支持 GitHub 和 Gitee
        return url.matches("^https?://(github\\.com|gitee\\.com)/[\\w-]+/[\\w.-]+.*$");
    }

    /**
     * 根据ID获取项目
     *
     * @param projectId 项目ID
     * @return 项目，如果不存在返回 null
     */
    public HackathonProject getProjectById(String projectId) {
        return projects.get(projectId);
    }

    /**
     * 根据ID获取团队
     *
     * @param teamId 团队ID
     * @return 团队，如果不存在返回 null
     */
    public Team getTeamById(String teamId) {
        return teams.get(teamId);
    }

    /**
     * 根据名称获取团队
     *
     * @param teamName 团队名称
     * @return 团队，如果不存在返回 null
     */
    public Team getTeamByName(String teamName) {
        return teams.values().stream()
            .filter(team -> team.getName().equals(teamName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据ID获取参与者
     *
     * @param participantId 参与者ID
     * @return 参与者，如果不存在返回 null
     */
    public Participant getParticipantById(String participantId) {
        return participants.get(participantId);
    }

    /**
     * 根据邮箱获取参与者
     *
     * @param email 邮箱
     * @return 参与者，如果不存在返回 null
     */
    public Participant getParticipantByEmail(String email) {
        return participants.values().stream()
            .filter(p -> p.getEmail().equals(email))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有项目
     *
     * @return 项目列表
     */
    public List<HackathonProject> getAllProjects() {
        return new ArrayList<>(projects.values());
    }

    /**
     * 获取所有团队
     *
     * @return 团队列表
     */
    public List<Team> getAllTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * 获取团队的所有项目
     *
     * @param teamId 团队ID
     * @return 项目列表
     */
    public List<HackathonProject> getProjectsByTeam(String teamId) {
        return projects.values().stream()
            .filter(project -> project.getTeam().getId().equals(teamId))
            .collect(Collectors.toList());
    }

    /**
     * 获取待评审的项目
     *
     * @return 项目列表
     */
    public List<HackathonProject> getPendingProjects() {
        return projects.values().stream()
            .filter(project -> project.getStatus() == HackathonProjectStatus.SUBMITTED)
            .collect(Collectors.toList());
    }

    /**
     * 获取已评审的项目
     *
     * @return 项目列表
     */
    public List<HackathonProject> getReviewedProjects() {
        return projects.values().stream()
            .filter(project -> project.getStatus() == HackathonProjectStatus.REVIEWED)
            .collect(Collectors.toList());
    }

    /**
     * 关闭项目
     *
     * @param projectId 项目ID
     */
    public void closeProject(String projectId) {
        HackathonProject project = getProjectById(projectId);
        if (project != null) {
            project.close();
        }
    }

    /**
     * 删除项目
     *
     * @param projectId 项目ID
     * @return true 如果删除成功
     */
    public boolean deleteProject(String projectId) {
        return projects.remove(projectId) != null;
    }

    /**
     * 获取项目统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalProjects", projects.size());
        stats.put("totalTeams", teams.size());
        stats.put("totalParticipants", participants.size());

        long pendingCount = projects.values().stream()
            .filter(p -> p.getStatus() == HackathonProjectStatus.SUBMITTED)
            .count();
        stats.put("pendingProjects", pendingCount);

        long reviewedCount = projects.values().stream()
            .filter(p -> p.getStatus() == HackathonProjectStatus.REVIEWED)
            .count();
        stats.put("reviewedProjects", reviewedCount);

        long closedCount = projects.values().stream()
            .filter(p -> p.getStatus() == HackathonProjectStatus.CLOSED)
            .count();
        stats.put("closedProjects", closedCount);

        return stats;
    }

    /**
     * 搜索项目
     *
     * @param keyword 关键词（搜索项目名称和描述）
     * @return 匹配的项目列表
     */
    public List<HackathonProject> searchProjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProjects();
        }

        String lowerKeyword = keyword.toLowerCase();
        return projects.values().stream()
            .filter(project ->
                project.getName().toLowerCase().contains(lowerKeyword) ||
                (project.getDescription() != null &&
                 project.getDescription().toLowerCase().contains(lowerKeyword))
            )
            .collect(Collectors.toList());
    }

    /**
     * 按状态获取项目
     *
     * @param status 项目状态
     * @return 项目列表
     */
    public List<HackathonProject> getProjectsByStatus(HackathonProjectStatus status) {
        return projects.values().stream()
            .filter(project -> project.getStatus() == status)
            .collect(Collectors.toList());
    }

    /**
     * 获取最近提交的项目
     *
     * @param limit 数量限制
     * @return 项目列表
     */
    public List<HackathonProject> getRecentProjects(int limit) {
        return projects.values().stream()
            .sorted((p1, p2) -> p2.getUpdatedAt().compareTo(p1.getUpdatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 清空所有数据（仅用于测试）
     */
    public void clearAll() {
        projects.clear();
        teams.clear();
        participants.clear();
    }
}


