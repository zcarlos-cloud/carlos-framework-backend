package com.carlos.fx.gitlab.entity;


import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * <p>
 * gitlab group
 * </p>
 *
 * @author Carlos
 * @date 2024/4/29 16:57
 */
@Data
public class GitlabProjectBranch {

    @ExcelProperty("项目ID")
    private Long projectId;
    @ExcelProperty("项目路径")
    private String projectPath;
    @ExcelProperty("项目名称")
    private String projectName;
    @ExcelProperty("项目描述")
    private String projectDesc;
    @ExcelProperty("项目Web地址")
    private String webUrl;
    @ExcelProperty("默认分支")
    private String defaultBranch;
    @ExcelProperty("项目创建时间")
    private String projectCreatedAt;
    @ExcelProperty("项目最后活动时间")
    private String projectLastActivity;

    @ExcelProperty("分支名")
    private String branchName;
    @ExcelProperty("是否已合并")
    private Boolean merged;
    @ExcelProperty("是否受保护")
    private Boolean isProtected;
    @ExcelProperty("最新提交ID")
    private String commitId;
    @ExcelProperty("最新提交信息")
    private String commitMessage;
    @ExcelProperty("提交作者")
    private String commitAuthor;
    @ExcelProperty("提交时间")
    private String commitDate;

}
