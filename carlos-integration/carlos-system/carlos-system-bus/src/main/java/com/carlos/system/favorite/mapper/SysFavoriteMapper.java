package com.carlos.system.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.favorite.pojo.entity.SysFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 通用用户收藏 Mapper 接口
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysFavoriteMapper extends BaseMapper<SysFavorite> {

}
