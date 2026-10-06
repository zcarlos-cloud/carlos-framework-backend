package com.carlos.auth.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.audit.pojo.entity.SecurityAlert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecurityAlertMapper extends BaseMapper<SecurityAlert> {
}
