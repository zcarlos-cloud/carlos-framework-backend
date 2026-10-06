package com.carlos.fx.gitlab.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.idev.excel.EasyExcel;
import com.carlos.fx.gitlab.config.GitLabServerInfo;
import com.carlos.fx.gitlab.entity.GitlabProjectBranch;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GitlabService {

    private GitLabServerInfo serverInfo;
    private GitLabApi gitlabApi;


    public void main() throws GitLabApiException {


        // final String format = "git clone %s";
        //
        // final String accessToken = "glpat-<REDACTED>";
        // GitLabApi gitlab = new GitLabApi("http://100.127.2.47:10001/", accessToken);
        // ProjectApi projectApi = gitlab.getProjectApi();
        // GroupApi groupApi = gitlab.getGroupApi();
        // List<Project> projects = groupApi.getProjects("union-ace-center");
        //
        // Set<String> urls = new HashSet<>();
        // for (Project project : projects) {
        //
        //     String webUrl = project.getHttpUrlToRepo();
        //     String name = project.getName();
        //     urls.add(String.format(format, webUrl));
        //     RuntimeUtil.exec(null, new File("D:\\ide_project"), "git", "clone", webUrl);
        // }

    }

    private final static String SERVER_INFO_JSON = new File("").getAbsolutePath() + File.separator + "server.json";


    public List<Commit> getCommits(Object projectIdOrPath, String ref, Date since, Date until,
                                   String path, Boolean all, Boolean withStats, Boolean firstParent) {
        try {
            // 获取提交记录
            List<Commit> commits = gitlabApi.getCommitsApi().getCommits(projectIdOrPath, ref, since, until, path, all, withStats, firstParent);
            return commits;
        } catch (GitLabApiException e) {
            log.error("获取提交记录失败: {}", e.getMessage(), e);
        }
        return null;
    }

    public void exportCommit() throws ParseException {

        GitLabServerInfo server = new GitLabServerInfo();
        server.setServerName("");
        server.setServerHost("http://100.127.2.47:10001/");
        server.setServerKey("<SERVER_KEY_REDACTED>");
        final String accessToken = "glpat-<REDACTED>";
        serverInfo = server;
        gitlabApi = initGitlabApi();

        final String groupName = "111";
        List<Project> projects = getGroupProjects(groupName);
        ExcelWriter writer = ExcelUtil.getWriter("/data/export/" + groupName + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx");
        for (Project project : projects) {
            List<Commit> commits = getCommits(project.getId(), "main", new Date(), new Date(), null, true, true, true);
            if (CollUtil.isEmpty(commits)) {
                continue;
            }
            // writer.setSheet(project.getName());
            // writer.write(commits, true);
            // ThreadUtil.sleep(3000);
        }
        // writer.flush();
        // writer.close();
    }

    public void exportCommit(Set<String> ids) throws Throwable {

        GitLabServerInfo server = new GitLabServerInfo();
        server.setServerName("");
        server.setServerHost("http://100.127.2.47:10001/");
        server.setServerKey("<SERVER_KEY_REDACTED>");
        final String accessToken = "glpat-<REDACTED>";
        serverInfo = server;
        gitlabApi = initGitlabApi();

        ExcelWriter writer = ExcelUtil.getWriter("/data/export/" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx");
        ProjectApi projectApi = gitlabApi.getProjectApi();
        for (String id : ids) {
            Project project = projectApi.getProject(id);
            List<Commit> commits = getCommits(project.getId(), "main", new Date(), new Date(), null, true, true, true);
            if (CollUtil.isEmpty(commits)) {
                continue;
            }
            // writer.setSheet(project.getName());
            // writer.write(commits, true);
        }
        // writer.flush();
        // writer.close();
    }

    public void exportBranchs(String groupName) throws Throwable {
        GitLabServerInfo server = new GitLabServerInfo();
        server.setServerName("");
        server.setServerHost("http://100.127.2.47:10001/");
        server.setServerKey("<SERVER_KEY_REDACTED>");
        serverInfo = server;
        gitlabApi = initGitlabApi();
        List<Project> projects = getGroupProjects(groupName);
        List<GitlabProjectBranch> rows = new ArrayList<>();

        for (Project p : projects) {
            List<Branch> branches = gitlabApi.getRepositoryApi().getBranches(p.getId());
            /* 如果一个分支都没有，也写一行空分支，方便统计 */
            if (branches.isEmpty()) {
                rows.add(buildRow(p, null));
            } else {
                for (Branch b : branches) {
                    rows.add(buildRow(p, b));
                }
            }
        }
        String fileName = "/data/export/" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
        FileUtil.touch(fileName);

        EasyExcel.write(fileName, GitlabProjectBranch.class)
                .sheet("项目分支汇总")
                .doWrite(rows);

        log.info(">>> 完成！共 {} 条记录。", rows.size());
        log.info(">>> 文件：{}", fileName);
    }


    private static GitlabProjectBranch buildRow(Project p, Branch b) {
        GitlabProjectBranch r = new GitlabProjectBranch();
        /* 项目维度 */
        r.setProjectId(p.getId());
        r.setProjectPath(p.getPathWithNamespace());
        r.setProjectName(p.getName());
        r.setProjectDesc(defaultStr(p.getDescription()));
        r.setWebUrl(p.getWebUrl());
        r.setDefaultBranch(defaultStr(p.getDefaultBranch()));
        r.setProjectCreatedAt(DateUtil.format(p.getCreatedAt(), DatePattern.NORM_DATETIME_PATTERN));
        r.setProjectLastActivity(DateUtil.format(p.getLastActivityAt(), DatePattern.NORM_DATETIME_PATTERN));

        /* 分支维度 */
        if (b != null) {
            r.setBranchName(b.getName());
            r.setMerged(b.getMerged());
            r.setIsProtected(b.getProtected());
            r.setCommitId(b.getCommit().getId());
            r.setCommitMessage(defaultStr(b.getCommit().getMessage()));
            r.setCommitAuthor(defaultStr(b.getCommit().getAuthorName()));
            r.setCommitDate(DateUtil.format(b.getCommit().getAuthoredDate(), DatePattern.NORM_DATETIME_PATTERN));
        } else {
            r.setBranchName("(无分支)");
        }
        return r;
    }

    /* --------------- 工具 --------------- */
    private static String defaultStr(String s) {
        return s == null ? "" : s;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /**
     * 保存服务信息
     *
     * @param serverInfo 服务信息
     * @author Carlos
     * @date 2024/4/29 13:21
     */
    public void saveServerInfo(GitLabServerInfo serverInfo) {
        File file = new File(SERVER_INFO_JSON);
        List<GitLabServerInfo> servers = new ArrayList<>();
        if (!FileUtil.exist(file)) {
            FileUtil.touch(file);
        } else {
            JSONArray array = JSONUtil.readJSONArray(file, StandardCharsets.UTF_8);
            servers = array.toList(GitLabServerInfo.class);
        }

        // 判断名称是否重复
        for (GitLabServerInfo info : servers) {
            if (info.getServerName().equals(serverInfo.getServerName())) {
                throw new IllegalStateException("服务名称重复");
            }
        }
        servers.add(serverInfo);
        String json = JSONUtil.toJsonStr(servers);
        FileUtil.writeString(json, file, StandardCharsets.UTF_8);
    }

    /**
     * 获取组下的项目
     *
     * @param groupName 组名称
     * @return
     * @author Carlos
     * @date 2024/4/29 13:21
     */
    public List<Project> getGroupProjects(String groupName) {
        GroupApi groupApi = initGitlabApi().getGroupApi();
        try {
            List<Project> projects = groupApi.getProjects(groupName);
            return projects;
        } catch (GitLabApiException e) {
            throw new IllegalStateException(e);
        }


    }

    public GitLabApi initGitlabApi() {
        return new GitLabApi(serverInfo.getServerHost(), serverInfo.getServerKey());
    }

    /**
     * 获取本地服务信息
     *
     * @author Carlos
     * @date 2024/4/29 13:21
     */
    public List<GitLabServerInfo> getLocationServers() {
        File file = new File(SERVER_INFO_JSON);
        List<GitLabServerInfo> servers;
        if (!FileUtil.exist(file)) {
            return new ArrayList<>();
        }
        JSONArray array = JSONUtil.readJSONArray(file, StandardCharsets.UTF_8);
        servers = array.toList(GitLabServerInfo.class);
        return servers;
    }


    /**
     * 保存服务信息
     *
     * @param serverInfo 服务信息
     * @return
     * @author Carlos
     * @date 2024/4/29 13:21
     */
    @SneakyThrows
    public List<Group> loadProject(GitLabServerInfo serverInfo) {
        GitLabApi gitlab = getGitlabApo(serverInfo);
        ProjectApi projectApi = gitlab.getProjectApi();
        ProtectedBranchesApi protectedBranchesApi = gitlab.getProtectedBranchesApi();
        RepositoryApi repositoryApi = gitlab.getRepositoryApi();
        GroupApi groupApi = gitlab.getGroupApi();
        List<Group> groups = groupApi.getGroups();
        for (Group group : groups) {
            Long id = group.getId();
            List<Project> projects = groupApi.getProjects(id);
            group.setProjects(projects);
        }
        groups = groups.stream().filter(group -> group.getProjects().size() > 0).collect(Collectors.toList());
        return groups;


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
    }


    /**
     * 获取api
     *
     * @param serverInfo 参数0
     * @return org.gitlab4j.api.GitLabApi
     * @author Carlos
     * @date 2024/4/29 16:41
     */
    private GitLabApi getGitlabApo(GitLabServerInfo serverInfo) {
        return new GitLabApi(serverInfo.getServerHost(), serverInfo.getServerKey());
    }


}
