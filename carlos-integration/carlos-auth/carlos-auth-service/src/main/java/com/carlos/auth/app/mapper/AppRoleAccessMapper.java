package com.carlos.auth.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.app.pojo.entity.AppRoleAccess;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用角色访问控制 Mapper
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Mapper
public interface AppRoleAccessMapper extends BaseMapper<AppRoleAccess> {
}
