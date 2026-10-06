package com.carlos.fx.gitlab.service;

import com.carlos.fx.gitlab.entity.GitlabMergeRequest;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GitLab Merge Request Service
 *
 * @author Carlos
 * @since 3.0.0
 */


public class MergeRequestService {

    private final GitLabApi gitLabApi;

    public MergeRequestService(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }

    /**
     * Create merge request
     */
    public GitlabMergeRequest createMergeRequest(Long projectId, String sourceBranch, String targetBranch,
                                                 String title, String description) throws GitLabApiException {
        MergeRequest mr = gitLabApi.getMergeRequestApi().createMergeRequest(
                projectId, sourceBranch, targetBranch, title, description, null
        );
        return convertToEntity(mr);
    }

    /**
     * List merge requests by state
     */
    public List<GitlabMergeRequest> listMergeRequests(Long projectId, String state) throws GitLabApiException {
        List<MergeRequest> mergeRequests;
        if (state != null && !state.isEmpty()) {
            mergeRequests = gitLabApi.getMergeRequestApi().getMergeRequests(projectId,
                    org.gitlab4j.api.Constants.MergeRequestState.valueOf(state.toUpperCase()));
        } else {
            mergeRequests = gitLabApi.getMergeRequestApi().getMergeRequests(projectId);
        }
        return mergeRequests.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Get merge request details
     */
    public GitlabMergeRequest getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        MergeRequest mr = gitLabApi.getMergeRequestApi().getMergeRequest(projectId, mergeRequestIid);
        return convertToEntity(mr);
    }

    /**
     * Approve merge request
     */
    public void approveMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        gitLabApi.getMergeRequestApi().approveMergeRequest(projectId, mergeRequestIid, null);
    }

    /**
     * Merge merge request
     */
    public GitlabMergeRequest mergeMergeRequest(Long projectId, Long mergeRequestIid, String mergeCommitMessage)
            throws GitLabApiException {
        MergeRequest mr = gitLabApi.getMergeRequestApi().acceptMergeRequest(
                projectId, mergeRequestIid, mergeCommitMessage, null, null, null
        );
        return convertToEntity(mr);
    }

    /**
     * Get merge request changes (diff)
     */
    public String getMergeRequestChanges(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        MergeRequest mr = gitLabApi.getMergeRequestApi().getMergeRequestChanges(projectId, mergeRequestIid);

        StringBuilder diff = new StringBuilder();
        if (mr.getChanges() != null) {
            for (Diff change : mr.getChanges()) {
                diff.append("File: ").append(change.getNewPath()).append("\n");
                diff.append(change.getDiff()).append("\n\n");
            }
        }

        return diff.toString();
    }

    /**
     * Add comment to merge request
     */
    public void addMergeRequestComment(Long projectId, Long mergeRequestIid, String comment)
            throws GitLabApiException {
        gitLabApi.getNotesApi().createMergeRequestNote(projectId, mergeRequestIid, comment);
    }

    /**
     * Get merge request comments
     */
    public List<String> getMergeRequestComments(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        List<Note> notes = gitLabApi.getNotesApi().getMergeRequestNotes(projectId, mergeRequestIid);
        List<String> comments = new ArrayList<>();

        for (Note note : notes) {
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
     * Close merge request
     */
    public void closeMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        gitLabApi.getMergeRequestApi().updateMergeRequest(
                projectId, mergeRequestIid, null, null, null, null,
                org.gitlab4j.api.Constants.StateEvent.CLOSE, null, null, null, null, null, null
        );
    }

    /**
     * Get merge request commits
     */
    public List<String> getMergeRequestCommits(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        List<Commit> commits = gitLabApi.getMergeRequestApi().getCommits(projectId, mergeRequestIid);
        List<String> commitMessages = new ArrayList<>();

        for (Commit commit : commits) {
            String message = String.format("[%s] %s: %s",
                    commit.getShortId(),
                    commit.getAuthorName(),
                    commit.getTitle()
            );
            commitMessages.add(message);
        }

        return commitMessages;
    }

    /**
     * Convert GitLab4J MergeRequest to entity
     */
    private GitlabMergeRequest convertToEntity(MergeRequest mr) {
        GitlabMergeRequest entity = new GitlabMergeRequest();
        entity.setId(mr.getId());
        entity.setIid(mr.getIid());
        entity.setTitle(mr.getTitle());
        entity.setDescription(mr.getDescription());
        entity.setState(mr.getState() != null ? mr.getState().toString() : null);
        entity.setSourceBranch(mr.getSourceBranch());
        entity.setTargetBranch(mr.getTargetBranch());

        if (mr.getAuthor() != null) {
            entity.setAuthorName(mr.getAuthor().getName());
            entity.setAuthorUsername(mr.getAuthor().getUsername());
        }

        entity.setCreatedAt(mr.getCreatedAt());
        entity.setUpdatedAt(mr.getUpdatedAt());
        entity.setMergedAt(mr.getMergedAt());

        if (mr.getMergedBy() != null) {
            entity.setMergedBy(mr.getMergedBy().getName());
        }

        entity.setWebUrl(mr.getWebUrl());
        entity.setUpvotes(mr.getUpvotes());
        entity.setDownvotes(mr.getDownvotes());
        entity.setWorkInProgress(mr.getWorkInProgress());
        // Note: getDraft() may not be available in all GitLab API versions
        entity.setDraft(mr.getWorkInProgress()); // Use WorkInProgress as fallback
        entity.setLabels(mr.getLabels());
        entity.setUserNotesCount(mr.getUserNotesCount());

        return entity;
    }
}
