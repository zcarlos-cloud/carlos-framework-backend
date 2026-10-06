package com.carlos.fx.gitlab.service;

import com.carlos.fx.gitlab.config.GitLabServerInfo;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.Test;

public class GitlabServiceTest {


    @Test
    public void loadProject() {
        GitlabService gitlabService = new GitlabService();
        GitLabServerInfo server = new GitLabServerInfo();
        server.setServerName("");
        server.setServerHost("http://100.127.2.47:10001/");
        server.setServerKey("<SERVER_KEY_REDACTED>");
        gitlabService.loadProject(server);
    }

    @Test
    public void exportCommit() {
        GitlabService gitlabService = new GitlabService();
        try {
            gitlabService.exportCommit(SetUtils.hashSet("248", "201", "199", "246", "254", "366"));
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void exportBranch() {
        GitlabService gitlabService = new GitlabService();
        try {
            gitlabService.exportBranchs("154");
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

}