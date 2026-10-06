package com.carlos.fx.gitlab.entity;

import lombok.Data;

import java.util.Date;

/**
 * GitLab User Entity
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class GitlabUser {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String state;
    private String avatarUrl;
    private String webUrl;
    private Date createdAt;
    private String bio;
    private String location;
    private String publicEmail;
    private String skype;
    private String linkedin;
    private String twitter;
    private String websiteUrl;
    private String organization;
    private Boolean isAdmin;
    private Boolean canCreateGroup;
    private Boolean canCreateProject;
    private Integer projectsLimit;
    private Date currentSignInAt;
    private Date lastSignInAt;
    private Boolean confirmed;
    private String externalUid;
    private String provider;
}
