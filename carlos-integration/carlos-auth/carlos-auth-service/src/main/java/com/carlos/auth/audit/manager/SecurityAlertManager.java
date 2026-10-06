package com.carlos.auth.audit.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carlos.auth.audit.pojo.entity.SecurityAlert;

import java.util.List;

/**
 * <p>
 * 安全告警 Manager 接口
 * </p>
 *
 * <p>数据查询封装层，继承 BaseService，实现增删改查等原子操作</p>
 *
 * @author Carlos
 * @date 2026-04-08
 */
public interface SecurityAlertManager extends IService<SecurityAlert> {

    /**
     * 根据用户ID查询告警列表
     *
     * @param userId 用户ID
     * @return 告警列表
     */
    List<SecurityAlert> selectByUserId(Long userId);

    /**
     * 查询未处理的告警
     *
     * @return 未处理告警列表
     */
    List<SecurityAlert> selectUnhandled();
}
