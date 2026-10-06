package com.carlos.org.convert;

import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserCandidateAO;
import com.carlos.org.pojo.dto.OrgUserCandidateDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.dto.OrgUserDeptDTO;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import com.carlos.org.pojo.entity.OrgUser;
import com.carlos.org.pojo.param.OrgUserCreateParam;
import com.carlos.org.pojo.param.OrgUserUpdateParam;
import com.carlos.org.pojo.vo.OrgUserDeptVO;
import com.carlos.org.pojo.vo.OrgUserRoleVO;
import com.carlos.org.pojo.vo.OrgUserVO;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.vo.OrgUserPositionVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 系统用户 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgUserConvert {

    OrgUserConvert INSTANCE = Mappers.getMapper(OrgUserConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDTO toDTO(OrgUserCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDTO toDTO(OrgUserUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserDTO> toDTO(List<OrgUser> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDTO toDTO(OrgUser entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUser toDO(OrgUserDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserVO toVO(OrgUserDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserVO> toVO(List<OrgUser> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserVO toVO(OrgUser entity);

    /**
     * 用户部门DTO转VO
     */
    OrgUserDeptVO toDeptVO(OrgUserDeptDTO dto);

    /**
     * 用户部门DTO列表转VO列表
     */
    List<OrgUserDeptVO> toDeptVOList(List<OrgUserDeptDTO> dtoList);

    /**
     * 用户角色DTO转VO
     */
    OrgUserRoleVO toRoleVO(OrgUserRoleDTO dto);

    /**
     * 用户角色DTO列表转VO列表
     */
    List<OrgUserRoleVO> toRoleVOList(List<OrgUserRoleDTO> dtoList);

    /**
     * 用户岗位DTO转VO
     */
    OrgUserPositionVO toPositionVO(OrgUserPositionDTO dto);

    /**
     * 用户岗位DTO列表转VO列表
     */
    List<OrgUserPositionVO> toPositionVOList(List<OrgUserPositionDTO> dtoList);

    /**
     * DTO to AO
     */
    @Mapping(target = "userStatus", expression = "java(dto.getUserStatus() != null ? dto.getUserStatus().getCode() : null)")
    OrgUserAO toAO(OrgUserDTO dto);

    /**
     * DTO List to AO List
     */
    List<OrgUserAO> toAOList(List<OrgUserDTO> dtoList);

    /**
     * 候选用户 DTO 转 AO（字段同名，直接映射）
     */
    OrgUserCandidateAO toCandidateAO(OrgUserCandidateDTO dto);

    /**
     * 候选用户 DTO 列表转 AO 列表
     */
    List<OrgUserCandidateAO> toCandidateAOList(List<OrgUserCandidateDTO> dtoList);

    /**
     * VO to AO（分页查询接口返回的是 VO，接口契约要 AO）
     *
     * <p>由 VO 而不是 DTO 映射：VO 不含 {@code pwd}，密码哈希不会随接口响应外泄；
     * {@code version} 只在 DTO/entity 上有，AO 上留空。</p>
     *
     * <p>方法名刻意不叫 {@code toAO}：与 {@code toAO(OrgUserDTO)} 重载时
     * {@code toAOList} 的两个重载会因泛型擦除而签名相同（都是 {@code List}），编译不过。</p>
     */
    @Mapping(target = "pwd", ignore = true)
    @Mapping(target = "version", ignore = true)
    OrgUserAO toAOFromVo(OrgUserVO vo);

    /**
     * VO List to AO List
     */
    List<OrgUserAO> toAOListFromVo(List<OrgUserVO> voList);

    // ======================== MapStruct 高级特性示例 ========================

    /**
     * @AfterMapping 示例：映射完成后对目标对象进行后处理
     * <p>
     * 应用场景：
     * <ul>
     *   <li>设置派生字段（如 displayName = nickname ?? account）</li>
     *   <li>补充外部数据（如调用字典服务获取状态文本）</li>
     *   <li>加密敏感字段（如密码 BCrypt 加密）</li>
     *   <li>日志记录映射结果</li>
     * </ul>
     * </p>
     * <p>
     * 触发时机：MapStruct 完成所有 &#064;Mapping 字段映射后自动调用此方法。
     * 本方法签名仅供参考，取消注释并补充 fields 后即可使用。
     * </p>
     *
     * @param source 映射源（如 OrgUser entity）
     * @param target 映射目标（如 OrgUserVO），由 &#064;MappingTarget 标注
     */
    // @AfterMapping
    // default void enrichVO(OrgUser source, @MappingTarget OrgUserVO target) {
    //     // 示例 1：昵称为空时用账号兜底
    //     if (target.getNickname() == null && source.getNickname() != null) {
    //         target.setNickname(source.getNickname());
    //     }
    //     // 示例 2：设置状态文本
    //     // target.setStatusText(statusDict.getText(source.getUserStatus()));
    // }

    /**
     * @Context 示例：传递辅助参数给映射方法
     * <p>
     * 应用场景：
     * <ul>
     *   <li>传递当前登录用户（用于自动填充 updatedBy/createBy）</li>
     *   <li>传递国际化 Locale（用于多语言字段映射）</li>
     *   <li>传递时区信息（用于时间字段转换）</li>
     *   <li>传递外部服务引用（如字典服务、文件服务）</li>
     * </ul>
     * </p>
     * <p>
     * 调用方式：
     * <pre>
     *   String currentUser = SecurityUtils.getCurrentUsername();
     *   OrgUserVO vo = OrgUserConvert.INSTANCE.toVO(entity, currentUser);
     * </pre>
     * &#064;Context 标注的参数不会作为映射源，仅作为辅助上下文在 &#064;Mapping.expression 和 &#064;AfterMapping 中使用。
     * </p>
     *
     * @param entity 映射源
     * @param currentUsername 上下文参数（由调用方注入）
     */
    // OrgUserVO toVO(OrgUser entity, @Context String currentUsername);
}
