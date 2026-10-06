package com.carlos.system.menu.convert;

import com.carlos.system.config.CommonConvert;
import com.carlos.system.menu.pojo.dto.SysMenuButtonDTO;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.entity.SysMenu;
import com.carlos.system.menu.pojo.param.SysMenuButtonParam;
import com.carlos.system.menu.pojo.param.SysMenuCreateParam;
import com.carlos.system.menu.pojo.param.SysMenuUpdateParam;
import com.carlos.system.menu.pojo.vo.SysMenuButtonVO;
import com.carlos.system.menu.pojo.vo.SysMenuRecursionVO;
import com.carlos.system.menu.pojo.vo.SysMenuTreeVO;
import com.carlos.system.menu.pojo.vo.SysMenuVO;
import com.carlos.system.pojo.ao.SysMenuAO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 系统菜单 转换器
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Mapper(uses = CommonConvert.class)
public interface SysMenuConvert {

    SysMenuConvert INSTANCE = Mappers.getMapper(SysMenuConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author carlos
     * @date 2021-12-28 15:26:57
     */

    @Mapping(target = "parentId", source = "parentId", defaultValue = "0L")
    SysMenuDTO toDTO(SysMenuCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    SysMenuDTO toDTO(SysMenuUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    List<SysMenuDTO> toDTO(List<SysMenu> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    SysMenuDTO toDTO(SysMenu entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    SysMenu toDO(SysMenuDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    @Mapping(target = "haveChildren", source = "id", qualifiedByName = "haveChildrenMenu")
    SysMenuVO toVO(SysMenuDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    List<SysMenuVO> toVO(List<SysMenu> dos);

    /**
     * 简单转换
     *
     * @param dtos 数据传输对象
     * @return java.util.List<com.carlos.user.pojo.vo.SysMenuTreeVO>
     * @author carlos
     * @date 2021/12/28 18:23
     */
    List<SysMenuVO> toListVO(List<SysMenuDTO> dtos);

    /**
     * 简单转换
     *
     * @param dtos 数据传输对象
     * @return java.util.List<com.carlos.user.pojo.vo.SysMenuTreeVO>
     * @author carlos
     * @date 2021/12/28 18:23
     */
    List<SysMenuTreeVO> toTreeListVO(List<SysMenuDTO> dtos);

    /**
     * 简单转换
     *
     * @param dtos 数据传输对象
     * @return java.util.List<com.carlos.user.pojo.vo.SysMenuTreeVO>
     * @author carlos
     * @date 2021/12/28 18:23
     */
    List<SysMenuRecursionVO> toRecursionListVO(List<SysMenuDTO> dtos);

    SysMenuAO toAO(SysMenuDTO dto);

    List<SysMenuAO> toAOList(List<SysMenuDTO> menus);

    /**
     * 菜单按钮参数转按钮DTO
     *
     * @param param 按钮参数
     * @return 按钮DTO
     */
    SysMenuButtonDTO toButtonDTO(SysMenuButtonParam param);

    /**
     * 菜单按钮参数列表转按钮DTO列表
     *
     * @param params 按钮参数列表
     * @return 按钮DTO列表
     */
    List<SysMenuButtonDTO> toButtonDTOList(List<SysMenuButtonParam> params);

    /**
     * 菜单按钮DTO转按钮VO
     *
     * @param dto 按钮DTO
     * @return 按钮VO
     */
    SysMenuButtonVO toButtonVO(SysMenuButtonDTO dto);

    /**
     * 菜单按钮DTO列表转按钮VO列表
     *
     * @param dtos 按钮DTO列表
     * @return 按钮VO列表
     */
    List<SysMenuButtonVO> toButtonVOList(List<SysMenuButtonDTO> dtos);
}
