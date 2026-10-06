package com.carlos.system.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.news.pojo.entity.SysNewsRead;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 公告已读记录 Mapper 接口
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysNewsReadMapper extends BaseMapper<SysNewsRead> {

}
