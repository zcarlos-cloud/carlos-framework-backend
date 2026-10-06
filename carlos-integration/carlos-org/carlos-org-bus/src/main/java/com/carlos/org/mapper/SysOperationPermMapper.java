package com.carlos.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.org.pojo.entity.SysOperationPerm;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * sys_operation 精简 Mapper（仅用于权限查询）
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Mapper
public interface SysOperationPermMapper extends BaseMapper<SysOperationPerm> {
}
