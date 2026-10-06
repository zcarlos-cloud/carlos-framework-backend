package com.carlos.system.upload.convert;


import com.carlos.system.pojo.ao.FileInfoAO;
import com.carlos.system.pojo.ao.UploadResultAO;
import com.carlos.system.pojo.param.ApiFileUploadParam.UploadFile;
import com.carlos.system.upload.pojo.dto.UploadFileDTO;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.dto.UploadResultDTO;
import com.carlos.system.upload.pojo.vo.FileInfoVO;
import com.carlos.system.upload.pojo.vo.UploadResultVO;
import org.mapstruct.Mapper;
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
public interface UploadFileConvert {

    UploadFileConvert INSTANCE = Mappers.getMapper(UploadFileConvert.class);

    /**
     * 三方接扣参数转DTO
     *
     * @param files 参数0
     * @return java.util.List<com.carlos.system.upload.pojo.dto.UploadFileDTO>
     * @author Carlos
     * @date 2023/7/6 14:38
     */
    List<UploadFileDTO> toDTO(List<UploadFile> files);

    /**
     * 转换成接口对象
     *
     * @param result 参数0
     * @return com.carlos.system.pojo.ao.UploadResultAO
     * @author Carlos
     * @date 2023/7/6 14:37
     */
    UploadResultAO toAO(UploadResultDTO result);

    FileInfoVO toFileVO(UploadFileDTO record);


    UploadResultVO toResultVO(UploadResultDTO dto);

    FileInfoAO toAO(UploadFileDTO fileInfo);

    UploadFileDTO toDTO(UploadRecordDTO i);
}
