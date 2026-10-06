package com.carlos.auth.app.convert;

import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import com.carlos.auth.app.pojo.entity.AppCategory;
import com.carlos.auth.app.pojo.param.AppCategoryCreateParam;
import com.carlos.auth.app.pojo.param.AppCategoryUpdateParam;
import com.carlos.auth.app.pojo.vo.AppCategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 应用分类 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Mapper(componentModel = "spring")
public interface AppCategoryConvert {

    AppCategoryConvert INSTANCE = Mappers.getMapper(AppCategoryConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     */
    AppCategoryDTO toDTO(AppCategoryCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     */
    AppCategoryDTO toDTO(AppCategoryUpdateParam param);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     */
    AppCategory toDO(AppCategoryDTO dto);

    /**
     * 数据持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象
     */
    AppCategoryDTO toDTO(AppCategory entity);

    /**
     * 数据持久化对象列表转数据传输对象列表
     *
     * @param entities 数据持久化对象列表
     * @return 数据传输对象列表
     */
    List<AppCategoryDTO> toDTO(List<AppCategory> entities);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     */
    AppCategoryVO toVO(AppCategoryDTO dto);

    /**
     * 数据传输对象列表转数据显示对象列表
     *
     * @param dtoList 数据传输对象列表
     * @return 数据显示对象列表
     */
    List<AppCategoryVO> toVO(List<AppCategoryDTO> dtoList);
}
