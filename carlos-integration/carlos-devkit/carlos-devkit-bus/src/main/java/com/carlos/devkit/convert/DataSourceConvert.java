package com.carlos.devkit.convert;

import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.entity.DevDataSource;
import com.carlos.devkit.pojo.param.DataSourceCreateParam;
import com.carlos.devkit.pojo.param.DataSourceTestParam;
import com.carlos.devkit.pojo.param.DataSourceUpdateParam;
import com.carlos.devkit.pojo.vo.DataSourceVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 数据源 转换器
 * </p>
 * <p>
 * {@code passwordCipher} 只存在于实体与 DTO，<b>任何 toVO 都不会带出密码</b>。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Mapper
public interface DataSourceConvert {

    DataSourceConvert INSTANCE = Mappers.getMapper(DataSourceConvert.class);

    /**
     * 新增参数转 DTO
     *
     * @param param 新增参数
     * @return DTO
     */
    DataSourceDTO toDTO(DataSourceCreateParam param);

    /**
     * 编辑参数转 DTO
     *
     * @param param 编辑参数
     * @return DTO
     */
    DataSourceDTO toDTO(DataSourceUpdateParam param);

    /**
     * 连接测试参数转 DTO
     *
     * @param param 测试参数
     * @return DTO
     */
    DataSourceDTO toDTO(DataSourceTestParam param);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return DTO
     */
    DataSourceDTO toDTO(DevDataSource entity);

    /**
     * 实体列表转 DTO 列表
     *
     * @param entities 实体列表
     * @return DTO 列表
     */
    List<DataSourceDTO> toDTO(List<DevDataSource> entities);

    /**
     * DTO 转实体
     *
     * @param dto DTO
     * @return 实体
     */
    DevDataSource toDO(DataSourceDTO dto);

    /**
     * DTO 转 VO（不含密码，hasPassword 由 Service 回填）
     *
     * @param dto DTO
     * @return VO
     */
    DataSourceVO toVO(DataSourceDTO dto);

    /**
     * DTO 列表转 VO 列表
     *
     * @param dtos DTO 列表
     * @return VO 列表
     */
    List<DataSourceVO> toVO(List<DataSourceDTO> dtos);
}
