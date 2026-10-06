package com.carlos.job.convert;

import com.carlos.job.pojo.ao.JobRegisterAO;
import com.carlos.job.pojo.dto.JobRegisterDTO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import com.carlos.job.pojo.param.JobCreateParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>
 * 自动注册任务转换器单元测试
 * </p>
 * <p>
 * 逐字段断言：MapStruct 对缺失的目标字段不会报错，只会静默丢字段，必须靠用例兜底。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class JobRegisterConvertTest {

    @Test
    @DisplayName("应该逐字段映射注册参数到新增参数")
    void should_map_every_field_from_register_param_to_create_param() {
        ApiJobRegisterParam param = new ApiJobRegisterParam()
            .setJobName("orderSyncJob")
            .setJobGroup("ORDER")
            .setServiceName("carlos-order")
            .setHandlerName("orderSyncHandler")
            .setScheduleType(2)
            .setScheduleConf("30000")
            .setCronExpression("0 0 2 * * ?")
            .setParams("{\"a\":1}")
            .setDescription("订单同步")
            .setMisfireStrategy(2)
            .setConcurrentMode(2)
            .setTimeoutSeconds(60)
            .setFailRetryCount(3)
            .setAuthor("carlos")
            .setAlarmReceivers("a,b")
            .setStatus("PAUSED");

        JobCreateParam createParam = JobRegisterConvert.INSTANCE.toCreateParam(param);

        assertNotNull(createParam);
        assertEquals("orderSyncJob", createParam.getJobName());
        assertEquals("ORDER", createParam.getJobGroup());
        assertEquals("carlos-order", createParam.getServiceName());
        assertEquals("orderSyncHandler", createParam.getHandlerName());
        assertEquals(2, createParam.getScheduleType().intValue());
        assertEquals("30000", createParam.getScheduleConf());
        assertEquals("0 0 2 * * ?", createParam.getCronExpression());
        assertEquals("{\"a\":1}", createParam.getParams());
        assertEquals("订单同步", createParam.getDescription());
        assertEquals(2, createParam.getMisfireStrategy().intValue());
        assertEquals(2, createParam.getConcurrentMode().intValue());
        assertEquals(60, createParam.getTimeoutSeconds().intValue());
        assertEquals(3, createParam.getFailRetryCount().intValue());
        assertEquals("carlos", createParam.getAuthor());
        assertEquals("a,b", createParam.getAlarmReceivers());
        // scheduleConf 优先于兼容字段 cronExpression
        assertEquals("30000", createParam.resolveScheduleConf());
    }

    @Test
    @DisplayName("应该保持未声明字段为null当注册参数缺省时")
    void should_keep_null_when_register_param_omitted() {
        JobCreateParam createParam = JobRegisterConvert.INSTANCE.toCreateParam(
            new ApiJobRegisterParam().setJobName("plainJob").setServiceName("carlos-job").setHandlerName("plainHandler"));

        assertNull(createParam.getJobGroup());
        assertNull(createParam.getScheduleConf());
        assertNull(createParam.getMisfireStrategy());
        assertNull(createParam.getConcurrentMode());
        assertNull(createParam.getTimeoutSeconds());
        assertNull(createParam.getFailRetryCount());
    }

    @Test
    @DisplayName("应该逐字段映射注册结果到API对象")
    void should_map_register_result_to_ao() {
        JobRegisterAO ao = JobRegisterConvert.INSTANCE.toAO(new JobRegisterDTO()
            .setCreated(Boolean.TRUE)
            .setExisted(Boolean.FALSE)
            .setDrifted(Boolean.FALSE)
            .setStatus("NORMAL")
            .setMessage("任务已创建"));

        assertNotNull(ao);
        assertEquals(Boolean.TRUE, ao.getCreated());
        assertEquals(Boolean.FALSE, ao.getExisted());
        assertEquals(Boolean.FALSE, ao.getDrifted());
        assertEquals("NORMAL", ao.getStatus());
        assertEquals("任务已创建", ao.getMessage());
    }

}
