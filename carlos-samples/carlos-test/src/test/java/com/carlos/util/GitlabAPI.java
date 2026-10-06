package com.carlos.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.jupiter.api.Test;

import java.util.*;

@Slf4j
public class GitlabAPI {

    @Data
    public static class GitlabUser {

        private String accountPrefix;
        private String account;
        private String password;
        private String name;
        private String email;
        private String groupIds;
        private String projectIds;
        private Integer level;
        private Date expire;

    }

    @Test
    public void createUser() throws GitLabApiException {

        ExcelReader reader = ExcelUtil.getReader("D:\\syncthing\\sync-work\\研发部管理办法集成\\申请流程\\gitlab申请\\西研-20251217.xlsx");
        List<GitlabUser> gitlabUsers = reader.readAll(GitlabUser.class);
        final String accessToken = "<SERVER_KEY_REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001", accessToken);
        for (GitlabUser gitlabUser : gitlabUsers) {
            String account = gitlabUser.getAccount();
            if (StrUtil.isNotBlank(gitlabUser.getAccountPrefix())) {
                account = gitlabUser.getAccountPrefix() + account;
            }
            String email = gitlabUser.getEmail();
            if (StrUtil.isBlank(email)) {
                email = gitlabUser.getAccount() + "@example.com";
            }
            User user = new User()
                    .withEmail(email)
                    .withName(gitlabUser.getName())
                    .withSkipConfirmation(true)
                    .withUsername(account);
            try {
                user = gitlab.getUserApi().createUser(user, gitlabUser.getPassword(), false);
            } catch (GitLabApiException e) {
                log.error("创建GitLab用户失败: {}", e.getMessage(), e);
            }
            Integer level = gitlabUser.getLevel();
            Date expire = gitlabUser.getExpire();
            String groupIds = gitlabUser.getGroupIds();
            if (StrUtil.isNotBlank(groupIds)) {
                List<String> groupIdss = StrUtil.split(groupIds, ",");
                for (String groupId : groupIdss) {
                    gitlab.getGroupApi().addMember(groupId, user.getId(), level, expire);
                    System.out.println("用户 " + user.getUsername() + " 已被添加到组 " + groupId + "");
                }
            }
            String projectIds = gitlabUser.getProjectIds();
            if (StrUtil.isNotBlank(projectIds)) {
                List<String> projectIdss = StrUtil.split(projectIds, ",");
                for (String projectId : projectIdss) {
                    gitlab.getProjectApi().addMember(projectId, user.getId(), level, expire);
                    System.out.println("用户 " + user.getUsername() + " 已被添加到仓" + projectId + "");
                }
            }
        }
    }

    @Test
    public void exportUser() {
        final String accessToken = "<SERVER_KEY_REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001", accessToken);
        List<User> activeUsers = null;
        try {
            activeUsers = gitlab.getUserApi().getActiveUsers();
        } catch (GitLabApiException e) {
            log.error("获取活跃用户失败: {}", e.getMessage(), e);
        }
        List<GitlabUser> gitlabUsers = new ArrayList<>();
        for (User user : activeUsers) {
            String username = user.getUsername();
            String name = user.getName();
            String email = user.getEmail();
            GitlabUser gitlabUser = new GitlabUser();
            gitlabUser.setAccount(username);
            gitlabUser.setName(name);
            gitlabUser.setEmail(email);
            gitlabUsers.add(gitlabUser);
        }
        ExcelWriter writer = ExcelUtil.getWriter("D:\\云津工作文件\\研发部管理办法集成\\Gitlab用户导出.xlsx");
        writer.write(gitlabUsers, true);
        writer.flush();
    }

    @Test
    public void printProject() {
        final String accessToken = "<SERVER_KEY_REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001", accessToken);
        List<Project> projects = null;
        try {
            projects = gitlab.getGroupApi().getProjects("111");
        } catch (GitLabApiException e) {
            log.error("获取项目失败: {}", e.getMessage(), e);
        }
        ProjectApi projectApi = gitlab.getProjectApi();
        for (Project project : projects) {
            System.out.println(project.getId() + "     " + project.getName());

        }
    }

    @Test
    public void delProjectMembers() throws GitLabApiException {
        final String accessToken = "<SERVER_KEY_REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001", accessToken);

        Set<String> users = Sets.newHashSet(
                "longj_moyp6",
                "longj_zhaoy",
                "longj_wangyh1105",
                "longj_wudey",
                "longj_wangrp",
                "longj_yanght",
                "longj_wymj",
                "longj_zhoupy",
                "longj_zhanggq",
                "longj_dutp",
                "xiyan_liuhd836",
                "xiyan_liy1172",
                "xiyan_niang",
                "xiyan_bianj16",
                "xiyan_hek7",
                "xiyan_haoym22",
                "carlos_lijl7318",
                "carlos_fengcs15"
        );
        Set<Long> sets = new HashSet<>();
        for (String userName : users) {
            User user = gitlab.getUserApi().getUser(userName);
            sets.add(user.getId());
        }

        Set<String> projectIds = Sets.newHashSet(
                "365",
                "333",
                "306",
                "247",
                "227",
                "220",
                "217",
                "209",
                "206",
                "203",
                "144",
                "143",
                "142",
                "141",
                "140",
                "201",
                "199",
                "366"
        );

        ProjectApi projectApi = gitlab.getProjectApi();
        for (String projectId : projectIds) {
            for (Long userId : sets) {
                try {
                    projectApi.removeMember(projectId, userId);
                } catch (GitLabApiException e) {
                    System.out.println(e.getMessage());
                }
                System.out.println("remove   " + projectId + "     " + userId);
            }
        }
    }

    @Test
    public void createRep() {

        final String accessToken = "glpat-<REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://192.168.23.211:18080/", accessToken);
        ProjectApi projectApi = gitlab.getProjectApi();

        Set<String> sets = Sets.newHashSet(
                "",
                "carlos-szt-boot",
                "carlos-szt-grid",
                "carlos-szt-form",
                "carlos-szt-task",
                "carlos-szt-bi",
                "carlos-szt-app",
                "carlos-szt-org",
                "carlos-szt-log",
                "carlos-szt-data",
                "carlos-szt-parent",
                "carlos-szt-report",
                "carlos-szt-analysis",
                "carlos-szt-magic",
            "carlos--flowable"
        );

        // for (String set : sets) {
        //     project.withPublic(false).withName(set).withInitializeWithReadme(false).withNamespaceId(252L).withPath(set).withVisibilityLevel(0);
        //     try {
        //         projectApi.createProject(project);
        //     } catch (GitLabApiException e) {
        //         e.printStackTrace();
        //     }
        // }


        Project project = new Project();
    }

    @Test
    public void unProtectBranch() {

        final String groupPath = "carlos-ai-company/bbt/v2/";
        final String accessToken = "g65mtvqpWoFNpnZMuWL8";
        GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001/", accessToken);
        ProjectApi projectApi = gitlab.getProjectApi();

        ProtectedBranchesApi protectedBranchesApi = gitlab.getProtectedBranchesApi();

        RepositoryApi repositoryApi = gitlab.getRepositoryApi();
        GroupApi groupApi = gitlab.getGroupApi();
        List<Project> projects = null;
        try {
            projects = groupApi.getProjects("311");
        } catch (GitLabApiException e) {
            log.error("获取组项目失败: {}", e.getMessage(), e);
            return;
        }


        Set<String> sets = Sets.newHashSet(
                // "carlos-szt-boot",
                // "carlos-szt-grid",
                // "carlos-szt-form",
                // "carlos-szt-home",
                // "carlos-szt-task",
                // "carlos-szt-bi",
                // "carlos-szt-app",
                // "carlos-szt-org",
                // "carlos-szt-log",
                // "carlos-szt-data",
                // "carlos-szt-report",
                // "carlos-szt-analysis",
                // // "carlos-szt-magic",
            // "carlos--flowable",
                // "carlos-szt-scenarios-population"
        );


        // for (Project project : projects) {
        //     Long id = project.getId();
        //     if (!sets.contains(project.getPath())) {
        //         continue;
        //     }
        //     try {
        //         protectedBranchesApi.unprotectBranch(id, "main");
        //         System.out.println("保护分支解除成功   " + id + "    " + project.getPath());
        //     } catch (GitLabApiException e) {
        //         System.out.println("保护分支解除失败   " + id + "    " + project.getPath());
        //         e.printStackTrace();
        //     }
        // }


        // for (Project project : projects) {
        //     Long id = project.getId();
        //     if (!sets.contains(project.getPath())) {
        //         continue;
        //     }
        //     project.setDefaultBranch("dev");
        //     try {
        //         projectApi.updateProject(project);
        //         repositoryApi.deleteBranch(id, "main");
        //         System.out.println("分支删除成功   " + id + "    " + project.getPath());
        //     } catch (GitLabApiException e) {
        //         System.out.println("分支删除失败   " + id + "    " + project.getPath());
        //         e.printStackTrace();
        //     }
        // }


        for (Project project : projects) {
            Long id = project.getId();
            if (CollUtil.isNotEmpty(sets)) {
                if (!sets.contains(project.getPath())) {
                    continue;
                }
            }

            try {
                repositoryApi.createBranch(id, "main", "dev-2.0-test");
                protectedBranchesApi.protectBranch(id, "main");
                System.out.println("分支创建成功   " + id + "    " + project.getPath());
            } catch (GitLabApiException e) {
                System.out.println("分支创建失败   " + id + "    " + project.getPath());
                log.error("分支操作失败: {}", e.getMessage(), e);
            }
        }


        // for (String set : sets) {
        //     try {
        //         projectApi.getProjectIdOrPath()
        //         protectedBranchesApi.unprotectBranch(groupPath + set, "main");
        //     } catch (GitLabApiException e) {
        //         e.printStackTrace();
        //     }
        // }
    }

    @Test
    public void cloneRep() throws GitLabApiException {
        final String format = "git clone %s";
        final String accessToken = "<SERVER_KEY_REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001/", accessToken);
        ProjectApi projectApi = gitlab.getProjectApi();
        GroupApi groupApi = gitlab.getGroupApi();
        List<Project> projects = groupApi.getProjects("311");


        Set<String> urls = new HashSet<>();
        for (Project project : projects) {
            String webUrl = project.getHttpUrlToRepo();
            String name = project.getName();
            String cloneUrl = String.format(format, webUrl);
            urls.add(cloneUrl);
            System.out.println(cloneUrl);

        }
        // RuntimeUtil.exec(null, new File("D:\\ide_project\\carlos\\carlos--event"), urls.toArray(new String[]{}));

    }


    @Test
    public void renameBranch() {

        final String accessToken = "glpat-<REDACTED>";
        GitLabApi gitlab = new GitLabApi("http://192.168.23.211:18080/", accessToken);
        ProjectApi projectApi = gitlab.getProjectApi();

        final String sourceBranch = "main";
        ProtectedBranchesApi protectedBranchesApi = gitlab.getProtectedBranchesApi();

        RepositoryApi repositoryApi = gitlab.getRepositoryApi();

        Set<String> sets = Sets.newHashSet(
                "",
                "carlos-szt-boot",
                "carlos-szt-grid",
                "carlos-szt-form",
                "carlos-szt-task",
                "carlos-szt-bi",
                "carlos-szt-app",
                "carlos-szt-org",
                "carlos-szt-log",
                "carlos-szt-data",
                "carlos-szt-parent",
                "carlos-szt-report",
                "carlos-szt-analysis",
                "carlos-szt-magic",
            "carlos--flowable"
        );

        for (String set : sets) {

            try {
                protectedBranchesApi.unprotectBranch(set, sourceBranch);

            } catch (GitLabApiException e) {
                log.error("解除分支保护失败: {}", e.getMessage(), e);
            }
        }


    }
}
