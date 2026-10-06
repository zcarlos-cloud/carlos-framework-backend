package com.carlos.fx.gitlab.service;

import com.carlos.fx.gitlab.entity.GitlabBranch;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.CompareResults;
import org.gitlab4j.api.models.ProtectedBranch;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GitLab Branch Service
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class BranchService {

    private final GitLabApi gitLabApi;

    public BranchService(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }

    /**
     * List all branches
     */
    public List<GitlabBranch> listBranches(Long projectId) throws GitLabApiException {
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(projectId);
        return branches.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Create branch
     */
    public GitlabBranch createBranch(Long projectId, String branchName, String ref) throws GitLabApiException {
        Branch branch = gitLabApi.getRepositoryApi().createBranch(projectId, branchName, ref);
        return convertToEntity(branch);
    }

    /**
     * Delete branch
     */
    public void deleteBranch(Long projectId, String branchName) throws GitLabApiException {
        gitLabApi.getRepositoryApi().deleteBranch(projectId, branchName);
    }

    /**
     * Protect branch
     */
    public void protectBranch(Long projectId, String branchName) throws GitLabApiException {
        gitLabApi.getProtectedBranchesApi().protectBranch(projectId, branchName);
    }

    /**
     * Unprotect branch
     */
    public void unprotectBranch(Long projectId, String branchName) throws GitLabApiException {
        gitLabApi.getProtectedBranchesApi().unprotectBranch(projectId, branchName);
    }

    /**
     * Compare branches
     */
    public String compareBranches(Long projectId, String fromBranch, String toBranch) throws GitLabApiException {
        CompareResults results = gitLabApi.getRepositoryApi().compare(projectId, fromBranch, toBranch);

        StringBuilder diff = new StringBuilder();
        diff.append("Comparing ").append(fromBranch).append(" -> ").append(toBranch).append("\n\n");

        if (results.getCommits() != null && !results.getCommits().isEmpty()) {
            diff.append("Commits (").append(results.getCommits().size()).append("):\n");
            results.getCommits().forEach(commit -> {
                diff.append("  - [").append(commit.getShortId()).append("] ")
                        .append(commit.getTitle()).append("\n");
            });
            diff.append("\n");
        }

        if (results.getDiffs() != null && !results.getDiffs().isEmpty()) {
            diff.append("File Changes (").append(results.getDiffs().size()).append("):\n");
            results.getDiffs().forEach(diffItem -> {
                diff.append("  - ").append(diffItem.getNewPath()).append("\n");
            });
        }

        return diff.toString();
    }

    /**
     * Delete merged branches
     */
    public List<String> deleteMergedBranches(Long projectId, String excludeBranch) throws GitLabApiException {
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(projectId);
        List<String> deletedBranches = new java.util.ArrayList<>();

        for (Branch branch : branches) {
            // Skip protected branches and excluded branch
            if (branch.getProtected() || branch.getName().equals(excludeBranch)) {
                continue;
            }

            // Check if branch is merged
            if (branch.getMerged() != null && branch.getMerged()) {
                try {
                    gitLabApi.getRepositoryApi().deleteBranch(projectId, branch.getName());
                    deletedBranches.add(branch.getName());
                } catch (GitLabApiException e) {
                    // Log error but continue
                    log.error("Failed to delete branch: {} - {}", branch.getName(), e.getMessage());
                }
            }
        }

        return deletedBranches;
    }

    /**
     * Get branch details
     */
    public GitlabBranch getBranch(Long projectId, String branchName) throws GitLabApiException {
        Branch branch = gitLabApi.getRepositoryApi().getBranch(projectId, branchName);
        return convertToEntity(branch);
    }

    /**
     * Check if branch is protected
     */
    public boolean isBranchProtected(Long projectId, String branchName) throws GitLabApiException {
        try {
            List<ProtectedBranch> protectedBranches = gitLabApi.getProtectedBranchesApi().getProtectedBranches(projectId);
            return protectedBranches.stream().anyMatch(pb -> pb.getName().equals(branchName));
        } catch (GitLabApiException e) {
            return false;
        }
    }

    /**
     * Convert GitLab4J Branch to entity
     */
    private GitlabBranch convertToEntity(Branch branch) {
        GitlabBranch entity = new GitlabBranch();
        entity.setName(branch.getName());
        entity.setMerged(branch.getMerged());
        entity.setProtectedBranch(branch.getProtected());
        entity.setDefaultBranch(branch.getDefault());
        entity.setDevelopersCanPush(branch.getDevelopersCanPush());
        entity.setDevelopersCanMerge(branch.getDevelopersCanMerge());
        entity.setCanPush(branch.getCanPush());

        if (branch.getCommit() != null) {
            entity.setCommitId(branch.getCommit().getId());
            entity.setCommitShortId(branch.getCommit().getShortId());
            entity.setCommitTitle(branch.getCommit().getTitle());
            entity.setCommitMessage(branch.getCommit().getMessage());
            entity.setCommitAuthorName(branch.getCommit().getAuthorName());
            entity.setCommitAuthorEmail(branch.getCommit().getAuthorEmail());
            entity.setCommitCreatedAt(branch.getCommit().getCreatedAt());
        }

        return entity;
    }
}
