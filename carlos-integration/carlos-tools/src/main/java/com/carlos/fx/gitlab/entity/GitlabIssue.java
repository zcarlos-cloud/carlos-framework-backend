package com.carlos.fx.gitlab.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * GitLab Issue Entity
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class GitlabIssue {

    private Long id;
    private Long iid;
    private String title;
    private String description;
    private String state;
    private String authorName;
    private String authorUsername;
    private String assigneeName;
    private String assigneeUsername;
    private Date createdAt;
    private Date updatedAt;
    private Date closedAt;
    private String closedBy;
    private String webUrl;
    private List<String> labels;
    private Integer upvotes;
    private Integer downvotes;
    private Integer userNotesCount;
    private String milestone;
    private Integer weight;
    private Date dueDate;
}
