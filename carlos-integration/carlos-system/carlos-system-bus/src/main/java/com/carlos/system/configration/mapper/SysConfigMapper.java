package com.carlos.system.configration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.configration.pojo.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统配置 查询接口
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {


}
