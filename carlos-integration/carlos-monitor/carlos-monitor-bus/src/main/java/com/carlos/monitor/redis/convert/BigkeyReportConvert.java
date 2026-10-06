package com.carlos.monitor.redis.convert;

import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReport;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReportItem;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportItemVO;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * <p>
 * 大key/热key 报告 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Mapper
public interface BigkeyReportConvert {

    BigkeyReportConvert INSTANCE = Mappers.getMapper(BigkeyReportConvert.class);

    /**
     * 报告实体转 VO
     */
    BigkeyReportVO toVO(MonitorBigkeyReport entity);

    /**
     * 明细实体转 VO
     */
    BigkeyReportItemVO toVO(MonitorBigkeyReportItem entity);
}
