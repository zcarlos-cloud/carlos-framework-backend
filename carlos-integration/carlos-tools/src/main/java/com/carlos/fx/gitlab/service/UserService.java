package com.carlos.fx.gitlab.service;

import com.carlos.fx.gitlab.entity.GitlabUser;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GitLab User Service
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class UserService {

    private final GitLabApi gitLabApi;

    public UserService(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }

    /**
     * List all users
     */
    public List<GitlabUser> listUsers() throws GitLabApiException {
        List<User> users = gitLabApi.getUserApi().getUsers();
        return users.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public GitlabUser getUser(Long userId) throws GitLabApiException {
        User user = gitLabApi.getUserApi().getUser(userId);
        return convertToEntity(user);
    }

    /**
     * Get user by username
     */
    public GitlabUser getUserByUsername(String username) throws GitLabApiException {
        User user = gitLabApi.getUserApi().getUser(username);
        return convertToEntity(user);
    }

    /**
     * Create user
     */
    public GitlabUser createUser(String username, String email, String name, String password) throws GitLabApiException {
        User userParams = new User();
        userParams.setUsername(username);
        userParams.setEmail(email);
        userParams.setName(name);

        User user = gitLabApi.getUserApi().createUser(userParams, password, false);
        return convertToEntity(user);
    }

    /**
     * Update user
     */
    public GitlabUser updateUser(Long userId, String email, String name) throws GitLabApiException {
        User userParams = new User();
        userParams.setEmail(email);
        userParams.setName(name);

        User user = gitLabApi.getUserApi().updateUser(userParams, String.valueOf(userId));
        return convertToEntity(user);
    }

    /**
     * Delete user
     */
    public void deleteUser(Long userId) throws GitLabApiException {
        gitLabApi.getUserApi().deleteUser(userId);
    }

    /**
     * Block user
     */
    public void blockUser(Long userId) throws GitLabApiException {
        gitLabApi.getUserApi().blockUser(userId);
    }

    /**
     * Unblock user
     */
    public void unblockUser(Long userId) throws GitLabApiException {
        gitLabApi.getUserApi().unblockUser(userId);
    }

    /**
     * Batch create users
     */
    public List<GitlabUser> batchCreateUsers(List<UserData> userData) throws GitLabApiException {
        List<GitlabUser> createdUsers = new java.util.ArrayList<>();

        for (UserData data : userData) {
            try {
                GitlabUser user = createUser(data.getUsername(), data.getEmail(), data.getName(), data.getPassword());
                createdUsers.add(user);
            } catch (GitLabApiException e) {
                // Log error but continue
                log.error("Failed to create user: {} - {}", data.getUsername(), e.getMessage());
            }
        }

        return createdUsers;
    }

    /**
     * Batch delete users
     */
    public List<Long> batchDeleteUsers(List<Long> userIds) throws GitLabApiException {
        List<Long> deletedUsers = new java.util.ArrayList<>();

        for (Long userId : userIds) {
            try {
                gitLabApi.getUserApi().deleteUser(userId);
                deletedUsers.add(userId);
            } catch (GitLabApiException e) {
                // Log error but continue
                log.error("Failed to delete user #{} - {}", userId, e.getMessage());
            }
        }

        return deletedUsers;
    }

    /**
     * Search users
     */
    public List<GitlabUser> searchUsers(String search) throws GitLabApiException {
        List<User> users = gitLabApi.getUserApi().findUsers(search);
        return users.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Get active users
     */
    public List<GitlabUser> getActiveUsers() throws GitLabApiException {
        List<User> users = gitLabApi.getUserApi().getActiveUsers();
        return users.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Get blocked users
     */
    public List<GitlabUser> getBlockedUsers() throws GitLabApiException {
        List<User> users = gitLabApi.getUserApi().getBlockedUsers();
        return users.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    /**
     * Convert GitLab4J User to entity
     */
    private GitlabUser convertToEntity(User user) {
        GitlabUser entity = new GitlabUser();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setState(user.getState() != null ? user.getState().toString() : null);
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setWebUrl(user.getWebUrl());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setBio(user.getBio());
        entity.setLocation(user.getLocation());
        entity.setPublicEmail(user.getPublicEmail());
        entity.setSkype(user.getSkype());
        entity.setLinkedin(user.getLinkedin());
        entity.setTwitter(user.getTwitter());
        entity.setWebsiteUrl(user.getWebsiteUrl());
        entity.setOrganization(user.getOrganization());
        entity.setIsAdmin(user.getIsAdmin());
        entity.setCanCreateGroup(user.getCanCreateGroup());
        entity.setCanCreateProject(user.getCanCreateProject());
        entity.setProjectsLimit(user.getProjectsLimit());
        entity.setCurrentSignInAt(user.getCurrentSignInAt());
        entity.setLastSignInAt(user.getLastSignInAt());
        entity.setConfirmed(user.getConfirmedAt() != null);
        entity.setExternalUid(user.getExternUid());
        entity.setProvider(user.getProvider());

        return entity;
    }

    /**
     * User Data for batch creation
     */
    public static class UserData {
        private String username;
        private String email;
        private String name;
        private String password;

        public UserData() {
        }

        public UserData(String username, String email, String name, String password) {
            this.username = username;
            this.email = email;
            this.name = name;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
