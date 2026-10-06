package com.carlos.system.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.dict.pojo.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统字典 查询接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {


}
