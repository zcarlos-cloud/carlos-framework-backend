package com.carlos.fx.gitlab.entity;

import lombok.Data;

import java.util.Date;

/**
 * GitLab 分支实体类
 * 封装 GitLab 分支的详细信息
 *
 * <p>该类用于存储从 GitLab API 获取的分支信息，包括：</p>
 * <ul>
 *   <li>分支基本信息：名称、是否合并、是否受保护等</li>
 *   <li>权限信息：开发者是否可以推送、合并等</li>
 *   <li>最后提交信息：提交 ID、标题、作者、时间等</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>在 TableView 中显示分支列表</li>
 *   <li>在对话框中显示分支详情</li>
 *   <li>作为分支操作的参数传递</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class GitlabBranch {

    /**
     * 分支名称
     * 例如：main, develop, feature/user-auth
     */
    private String name;

    /**
     * 是否已合并
     * true 表示该分支已经合并到默认分支
     */
    private Boolean merged;

    /**
     * 是否受保护
     * 受保护的分支不能被强制推送或删除，通常用于主分支
     */
    private Boolean protectedBranch;

    /**
     * 是否为默认分支
     * 默认分支通常是 main 或 master
     */
    private Boolean defaultBranch;

    /**
     * 开发者是否可以推送
     * 控制普通开发者是否有权限推送到该分支
     */
    private Boolean developersCanPush;

    /**
     * 开发者是否可以合并
     * 控制普通开发者是否有权限合并到该分支
     */
    private Boolean developersCanMerge;

    /**
     * 当前用户是否可以推送
     * 根据当前用户的权限判断是否可以推送到该分支
     */
    private Boolean canPush;

    /**
     * 最后提交的完整 ID
     * 40 位的 SHA-1 哈希值，例如：a1b2c3d4e5f6...
     */
    private String commitId;

    /**
     * 最后提交的短 ID
     * 通常是前 8 位，例如：a1b2c3d4
     */
    private String commitShortId;

    /**
     * 最后提交的标题
     * 提交消息的第一行
     */
    private String commitTitle;

    /**
     * 最后提交的完整消息
     * 包含提交的详细描述
     */
    private String commitMessage;

    /**
     * 最后提交的作者名称
     */
    private String commitAuthorName;

    /**
     * 最后提交的作者邮箱
     */
    private String commitAuthorEmail;

    /**
     * 最后提交的创建时间
     */
    private Date commitCreatedAt;


}
