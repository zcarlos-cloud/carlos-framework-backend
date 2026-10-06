package com.carlos.system.upload.convert;


import com.carlos.system.pojo.ao.FileInfoAO;
import com.carlos.system.upload.pojo.dto.UploadFileDTO;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.entity.UploadRecord;
import com.carlos.system.upload.pojo.vo.FileInfoVO;
import com.carlos.system.upload.pojo.vo.UploadRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 文件上传记录 转换器
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Mapper(uses = {CommonConvert.class})
public interface UploadRecordConvert {

    UploadRecordConvert INSTANCE = Mappers.getMapper(UploadRecordConvert.class);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    List<UploadRecordDTO> toDTO(List<UploadRecord> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    UploadRecordDTO toDTO(UploadRecord entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    UploadRecord toDO(UploadRecordDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    UploadRecordVO toVO(UploadRecordDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    List<UploadRecordVO> toVO(List<UploadRecord> dos);

    @Mapping(target = "name", source = "originalName")
    FileInfoAO toAO(UploadRecordDTO dto);

    List<FileInfoAO> toAO(List<UploadRecordDTO> dtos);

    FileInfoVO toFileVO(UploadRecordDTO dto);

    List<FileInfoAO> uploadFile2AO(List<UploadFileDTO> files);
}
