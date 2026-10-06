package com.carlos.job.convert;

import com.carlos.job.pojo.ao.JobRegisterAO;
import com.carlos.job.pojo.dto.JobRegisterDTO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import com.carlos.job.pojo.param.JobCreateParam;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * <p>
 * 自动注册任务 转换器
 * </p>
 * <p>
 * {@link ApiJobRegisterParam} 与 {@link JobCreateParam} 的字段名刻意保持一致，
 * 由 MapStruct 逐字段映射；字段名漂移会导致静默丢字段，故有逐字段断言的单元测试兜底。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Mapper
public interface JobRegisterConvert {

    JobRegisterConvert INSTANCE = Mappers.getMapper(JobRegisterConvert.class);

    /**
     * 注册参数转新增参数（复用既有的双写编排）
     *
     * @param param 自动注册参数
     * @return 任务新增参数
     */
    JobCreateParam toCreateParam(ApiJobRegisterParam param);

    /**
     * 注册结果转 API 对象
     *
     * @param dto 注册结果
     * @return API 对象
     */
    JobRegisterAO toAO(JobRegisterDTO dto);

}
