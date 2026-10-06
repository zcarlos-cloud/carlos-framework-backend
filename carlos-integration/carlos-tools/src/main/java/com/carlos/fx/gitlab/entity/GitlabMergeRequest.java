package com.carlos.fx.gitlab.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * GitLab Merge Request Entity
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class GitlabMergeRequest {

    private Long id;
    private Long iid;
    private String title;
    private String description;
    private String state;
    private String sourceBranch;
    private String targetBranch;
    private String authorName;
    private String authorUsername;
    private Date createdAt;
    private Date updatedAt;
    private Date mergedAt;
    private String mergedBy;
    private String webUrl;
    private Integer upvotes;
    private Integer downvotes;
    private Boolean workInProgress;
    private Boolean draft;
    private List<String> labels;
    private Integer userNotesCount;
}
