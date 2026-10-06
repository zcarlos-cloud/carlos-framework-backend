package com.carlos.test.convert;

import com.carlos.test.pojo.dto.OrgUserDTO;
import com.carlos.test.pojo.entity.OrgUser;
import com.carlos.test.pojo.param.OrgUserCreateParam;
import com.carlos.test.pojo.param.OrgUserUpdateParam;
import com.carlos.test.pojo.vo.OrgUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 系统用户 转换
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgUserConvert {

    OrgUserConvert INSTANCE = Mappers.getMapper(OrgUserConvert.class);

    /**
     * 接口新增参数对象转数据传输对
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUserDTO toDTO(OrgUserCreateParam param);

    /**
     * 接口修改参数对象转数据传输对
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUserDTO toDTO(OrgUserUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    List<OrgUserDTO> toDTO(List<OrgUser> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUserDTO toDTO(OrgUser entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUser toDO(OrgUserDTO dto);

    /**
     * 数据传输对象转数据显示对
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUserVO toVO(OrgUserDTO dto);

    /**
     * 数据持久对象转数据显示对
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    List<OrgUserVO> toVO(List<OrgUser> dos);

    /**
     * 数据持久对象转数据显示对
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUserVO toVO(OrgUser entity);
}
