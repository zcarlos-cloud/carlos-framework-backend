package com.carlos.system.favorite.convert;

import com.carlos.system.favorite.pojo.entity.SysFavorite;
import com.carlos.system.favorite.pojo.vo.SysFavoriteVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 通用用户收藏 对象转换器
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysFavoriteConvert {

    SysFavoriteConvert INSTANCE = Mappers.getMapper(SysFavoriteConvert.class);

    /**
     * 实体转 VO
     *
     * @param entity 收藏实体
     * @return 视图对象
     */
    SysFavoriteVO toVO(SysFavorite entity);

    /**
     * 实体列表转 VO 列表
     *
     * @param entities 收藏实体列表
     * @return 视图对象列表
     */
    List<SysFavoriteVO> toVOList(List<SysFavorite> entities);

}
