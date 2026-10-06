package com.carlos.auth.app.convert;

import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import com.carlos.auth.app.pojo.dto.AppPortalAppDTO;
import com.carlos.auth.app.pojo.entity.AppClient;
import com.carlos.auth.app.pojo.vo.AppPortalAppVO;
import com.carlos.auth.app.pojo.vo.AppPortalCategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 应用门户 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Mapper
public interface AppPortalConvert {

    AppPortalConvert INSTANCE = Mappers.getMapper(AppPortalConvert.class);

    /**
     * 应用客户端实体转门户应用数据传输对象
     *
     * @param entity 应用客户端实体
     * @return 门户应用数据传输对象
     */
    @Mapping(source = "appCategoryId", target = "categoryId")
    AppPortalAppDTO toAppDTO(AppClient entity);

    /**
     * 应用客户端实体列表转门户应用数据传输对象列表
     *
     * @param entities 应用客户端实体列表
     * @return 门户应用数据传输对象列表
     */
    List<AppPortalAppDTO> toAppDTOList(List<AppClient> entities);

    /**
     * 门户应用数据传输对象转门户应用展示对象
     *
     * @param dto 门户应用数据传输对象
     * @return 门户应用展示对象
     */
    AppPortalAppVO toAppVO(AppPortalAppDTO dto);

    /**
     * 门户应用数据传输对象列表转门户应用展示对象列表
     *
     * @param dtoList 门户应用数据传输对象列表
     * @return 门户应用展示对象列表
     */
    List<AppPortalAppVO> toAppVOList(List<AppPortalAppDTO> dtoList);

    /**
     * 应用分类数据传输对象转门户分类展示对象
     *
     * @param dto 应用分类数据传输对象
     * @return 门户分类展示对象
     */
    @Mapping(source = "categoryCode", target = "code")
    @Mapping(source = "categoryName", target = "name")
    @Mapping(source = "categoryIcon", target = "icon")
    AppPortalCategoryVO toCategoryVO(AppCategoryDTO dto);

    /**
     * 应用分类数据传输对象列表转门户分类展示对象列表
     *
     * @param dtoList 应用分类数据传输对象列表
     * @return 门户分类展示对象列表
     */
    List<AppPortalCategoryVO> toCategoryVOList(List<AppCategoryDTO> dtoList);
}
