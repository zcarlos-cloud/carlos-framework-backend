package com.carlos.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.org.pojo.entity.OrgRoleOperation;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 角色-操作权限关联 Mapper 接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Mapper
public interface OrgRoleOperationMapper extends BaseMapper<OrgRoleOperation> {

}
