package com.carlos.system.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.upload.pojo.entity.UploadRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文件上传记录 查询接口
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Mapper
public interface UploadRecordMapper extends BaseMapper<UploadRecord> {


}
