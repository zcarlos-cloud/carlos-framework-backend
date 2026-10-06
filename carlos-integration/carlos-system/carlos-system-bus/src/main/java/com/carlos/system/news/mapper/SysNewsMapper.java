package com.carlos.system.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.news.pojo.entity.SysNews;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 公告/站内通知 Mapper 接口
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysNewsMapper extends BaseMapper<SysNews> {

}
