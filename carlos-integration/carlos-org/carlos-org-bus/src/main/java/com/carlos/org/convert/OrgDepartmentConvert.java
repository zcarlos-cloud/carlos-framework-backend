package com.carlos.org.convert;


import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.pojo.dto.OrgDepartmentDTO;
import com.carlos.org.pojo.dto.OrgDepartmentUserDTO;
import com.carlos.org.pojo.entity.OrgDepartment;
import com.carlos.org.pojo.param.OrgDepartmentCreateParam;
import com.carlos.org.pojo.param.OrgDepartmentUpdateParam;
import com.carlos.org.pojo.vo.OrgDepartmentTreeVO;
import com.carlos.org.pojo.vo.OrgDepartmentUserVO;
import com.carlos.org.pojo.vo.OrgDepartmentVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;


/**
 * <p>
 * 部门 mapstruct
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Mapper(componentModel = "spring")
public interface OrgDepartmentConvert {

    OrgDepartmentConvert INSTANCE = Mappers.getMapper(OrgDepartmentConvert.class);

    // ==================== Entity ↔ DTO ====================

    /**
     * 转 do
     *
     * @param dto 对象
     * @return com.carlos.org.pojo.entity.OrgDepartment
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartment toDO(OrgDepartmentDTO dto);

    /**
     * 转 DTO
     *
     * @param entity 对象
     * @return com.carlos.org.pojo.dto.OrgDepartmentDTO
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentDTO toDTO(OrgDepartment entity);

    // ==================== Param ↔ DTO ====================

    /**
     * Param to DTO
     */
    OrgDepartmentDTO toDTO(OrgDepartmentCreateParam param);

    /**
     * UpdateParam to DTO
     */
    OrgDepartmentDTO toDTO(OrgDepartmentUpdateParam param);

    // ==================== DTO ↔ VO ====================

    /**
     * DTO to VO
     */
    OrgDepartmentVO toVO(OrgDepartmentDTO dto);

    /**
     * DTO List to VO List
     */
    List<OrgDepartmentVO> toVOList(List<OrgDepartmentDTO> dtoList);

    /**
     * DTO to TreeVO
     */
    OrgDepartmentTreeVO toTreeVO(OrgDepartmentDTO dto);

    /**
     * DTO List to TreeVO List (构建树形结构)
     */
    default List<OrgDepartmentTreeVO> buildTreeVOList(List<OrgDepartmentDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 先转换为 TreeVO
        List<OrgDepartmentTreeVO> treeList = dtoList.stream()
            .map(this::toTreeVO)
            .collect(java.util.stream.Collectors.toList());

        // 构建父子关系
        java.util.Map<Long, OrgDepartmentTreeVO> map = treeList.stream()
            .collect(java.util.stream.Collectors.toMap(OrgDepartmentTreeVO::getId, v -> v));

        java.util.List<OrgDepartmentTreeVO> result = new java.util.ArrayList<>();
        for (OrgDepartmentTreeVO vo : treeList) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                result.add(vo);
            } else {
                OrgDepartmentTreeVO parent = map.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new java.util.ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }
        // 按排序排序
        result.sort(java.util.Comparator.comparing(OrgDepartmentTreeVO::getOrderNum, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return result;
    }

    /**
     * User DTO to User VO
     */
    OrgDepartmentUserVO toUserVO(OrgDepartmentUserDTO dto);

    /**
     * User DTO List to User VO List
     */
    List<OrgDepartmentUserVO> toUserVOList(List<OrgDepartmentUserDTO> dtoList);

    // ==================== DTO ↔ AO ====================

    /**
     * DTO to AO
     */
    @Mapping(target = "deptStatus", expression = "java(dto.getDeptStatus() != null ? dto.getDeptStatus().getCode() : null)")
    OrgDepartmentAO toAO(OrgDepartmentDTO dto);

    /**
     * DTO List to AO List
     */
    List<OrgDepartmentAO> toAOList(List<OrgDepartmentDTO> dtoList);

}
