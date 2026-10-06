package com.carlos.test.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户 API提供的对外API Object)
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Data
public class OrgUserAO implements Serializable {

    /**
     * 主键
     */
    private Long id;
    /**
     * 用户
     */
    private String account;
    /**
     * 真实姓名
     */
    private String realname;
    /**
     * 密码
     */
    private String pwd;
    /**
     * 证件号码
     */
    private String identify;
    /**
     * 手机号码
     */
    private String phone;
    /**
     * 详细地址
     */
    private String address;
    /**
     * 性别：保 1：男：女，默
     */
    private Integer gender;
    /**
     * 头像文件的id
     */
    private String head;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String description;
    /**
     * 钉钉
     */
    private String dingding;
    /**
     * 政治面貌
     */
    private String politicalOutlook;
    /**
     * 学历
     */
    private String educationBackground;
    /**
     * 最后登录时
     */
    private LocalDateTime lastLogin;
    /**
     * 登录次数
     */
    private Integer loginCount;
    /**
     * 创建
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改
     */
    private Long updateBy;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}
