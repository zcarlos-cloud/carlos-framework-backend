package com.carlos.test.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Data
@Accessors(chain = true)
public class ApiOrgUserParam implements Serializable {

    /**
     * 主键
     */
    private Long id;
    /**
     * 用户名
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
     * 性别，0：保密, 1：男，2：女，默认0
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
     * 最后登录时间
     */
    private LocalDateTime lastLogin;
    /**
     * 登录次数
     */
    private Integer loginCount;
    /**
     * 创建者
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改者
     */
    private Long updateBy;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}
