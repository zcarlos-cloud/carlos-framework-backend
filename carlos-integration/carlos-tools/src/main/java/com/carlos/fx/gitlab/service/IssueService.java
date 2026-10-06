package com.carlos.fx.gitlab.service;

import com.carlos.fx.gitlab.entity.GitlabIssue;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.IssueFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GitLab Issue Service
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class IssueService {

    private final GitLabApi gitLabApi;

    public IssueService(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }

    /**
     * Create issue
     */
    public GitlabIssue createIssue(Long projectId, String title, String description) throws GitLabApiException {
        Issue issue = gitLabApi.getIssuesApi().createIssue(projectId, title, description);
        return convertToEntity(issue);
    }

    /**
     * List issues by state
     */
    public List<GitlabIssue> listIssues(Long projectId, String state) throws GitLabApiException {
        IssueFilter filter = new IssueFilter();
        if (state != null && !state.isEmpty()) {
            filter.setState(org.gitlab4j.api.Constants.IssueState.valueOf(state.toUpperCase()));
        }

        List<Issue> issues = gitLabApi.getIssuesApi().getIssues(projectId, filter);
        return issues.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Get issue details
     */
    public GitlabIssue getIssue(Long projectId, Long issueIid) throws GitLabApiException {
        Issue issue = gitLabApi.getIssuesApi().getIssue(projectId, issueIid);
        return convertToEntity(issue);
    }

    /**
     * Close issue
     */
    public GitlabIssue closeIssue(Long projectId, Long issueIid) throws GitLabApiException {
        Issue issue = gitLabApi.getIssuesApi().closeIssue(projectId, issueIid);
        return convertToEntity(issue);
    }

    /**
     * Reopen issue
     */
    public GitlabIssue reopenIssue(Long projectId, Long issueIid) throws GitLabApiException {
        Issue issue = gitLabApi.getIssuesApi().updateIssue(projectId, issueIid, null, null, null,
                null, null, null, org.gitlab4j.api.Constants.StateEvent.REOPEN, null, null);
        return convertToEntity(issue);
    }

    /**
     * Assign issue to user
     */
    public void assignIssue(Long projectId, Long issueIid, Long assigneeId) throws GitLabApiException {
        gitLabApi.getIssuesApi().updateIssue(projectId, issueIid, null, null, null,
                java.util.Arrays.asList(assigneeId), null, null, null, null, null);
    }

    /**
     * Add label to issue
     */
    public void addLabel(Long projectId, Long issueIid, String label) throws GitLabApiException {
        Issue issue = gitLabApi.getIssuesApi().getIssue(projectId, issueIid);
        List<String> labels = new java.util.ArrayList<>(issue.getLabels());
        labels.add(label);

        gitLabApi.getIssuesApi().updateIssue(projectId, issueIid, null, null, null,
                null, null, String.join(",", labels), null, null, null);
    }

    /**
     * Link issue to merge request
     */
    public void linkToMergeRequest(Long projectId, Long issueIid, Long mergeRequestIid) throws GitLabApiException {
        String comment = String.format("Related to !%d", mergeRequestIid);
        gitLabApi.getNotesApi().createIssueNote(projectId, issueIid, comment);
    }

    /**
     * Batch close issues
     */
    public List<Long> batchCloseIssues(Long projectId, List<Long> issueIids) throws GitLabApiException {
        List<Long> closedIssues = new java.util.ArrayList<>();

        for (Long issueIid : issueIids) {
            try {
                gitLabApi.getIssuesApi().closeIssue(projectId, issueIid);
                closedIssues.add(issueIid);
            } catch (GitLabApiException e) {
                // Log error but continue
                log.error("Failed to close issue #{} - {}", issueIid, e.getMessage());
            }
        }

        return closedIssues;
    }

    /**
     * Add comment to issue
     */
    public void addIssueComment(Long projectId, Long issueIid, String comment) throws GitLabApiException {
        gitLabApi.getNotesApi().createIssueNote(projectId, issueIid, comment);
    }

    /**
     * Get issue comments
     */
    public List<String> getIssueComments(Long projectId, Long issueIid) throws GitLabApiException {
        List<org.gitlab4j.api.models.Note> notes = gitLabApi.getNotesApi().getIssueNotes(projectId, issueIid);
        List<String> comments = new java.util.ArrayList<>();

        for (org.gitlab4j.api.models.Note note : notes) {
            String comment = String.format("[%s] %s: %s",
                    note.getCreatedAt(),
                    note.getAuthor().getName(),
                    note.getBody()
            );
            comments.add(comment);
        }

        return comments;
    }

    /**
     * Update issue
     */
    public GitlabIssue updateIssue(Long projectId, Long issueIid, String title, String description)
            throws GitLabApiException {
        Issue issue = gitLabApi.getIssuesApi().updateIssue(projectId, issueIid, title, description, null,
                null, null, null, null, null, null);
        return convertToEntity(issue);
    }

    /**
     * Delete issue
     */
    public void deleteIssue(Long projectId, Long issueIid) throws GitLabApiException {
        gitLabApi.getIssuesApi().deleteIssue(projectId, issueIid);
    }

    /**
     * Convert GitLab4J Issue to entity
     */
    private GitlabIssue convertToEntity(Issue issue) {
        GitlabIssue entity = new GitlabIssue();
        entity.setId(issue.getId());
        entity.setIid(issue.getIid());
        entity.setTitle(issue.getTitle());
        entity.setDescription(issue.getDescription());
        entity.setState(issue.getState() != null ? issue.getState().toString() : null);

        if (issue.getAuthor() != null) {
            entity.setAuthorName(issue.getAuthor().getName());
            entity.setAuthorUsername(issue.getAuthor().getUsername());
        }

        if (issue.getAssignee() != null) {
            entity.setAssigneeName(issue.getAssignee().getName());
            entity.setAssigneeUsername(issue.getAssignee().getUsername());
        }

        entity.setCreatedAt(issue.getCreatedAt());
        entity.setUpdatedAt(issue.getUpdatedAt());
        entity.setClosedAt(issue.getClosedAt());

        if (issue.getClosedBy() != null) {
            entity.setClosedBy(issue.getClosedBy().getName());
        }

        entity.setWebUrl(issue.getWebUrl());
        entity.setLabels(issue.getLabels());
        entity.setUpvotes(issue.getUpvotes());
        entity.setDownvotes(issue.getDownvotes());
        entity.setUserNotesCount(issue.getUserNotesCount());

        if (issue.getMilestone() != null) {
            entity.setMilestone(issue.getMilestone().getTitle());
        }

        entity.setWeight(issue.getWeight());
        entity.setDueDate(issue.getDueDate());

        return entity;
    }
}
