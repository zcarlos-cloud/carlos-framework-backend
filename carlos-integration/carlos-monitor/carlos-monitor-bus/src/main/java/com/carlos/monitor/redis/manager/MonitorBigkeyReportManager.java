package com.carlos.monitor.redis.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReport;
import com.carlos.monitor.redis.pojo.param.BigkeyReportPageParam;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportVO;

import java.io.Serializable;

/**
 * <p>
 * Redis 大key/热key 报告 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public interface MonitorBigkeyReportManager extends BaseService<MonitorBigkeyReport> {

    /**
     * 创建运行中报告
     *
     * @param report 报告实体
     * @return 是否成功（id 回填）
     */
    boolean createReport(MonitorBigkeyReport report);

    /**
     * 报告分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<BigkeyReportVO> getPage(BigkeyReportPageParam param);

    /**
     * 报告详情
     *
     * @param id 报告ID
     * @return 报告实体
     */
    MonitorBigkeyReport getReportById(Serializable id);

    /**
     * 标记报告完成
     *
     * @param id          报告ID
     * @param scannedKeys 已扫描 key 数量
     */
    void markSuccess(Serializable id, long scannedKeys);

    /**
     * 标记报告失败
     *
     * @param id       报告ID
     * @param errorMsg 失败原因
     */
    void markFailed(Serializable id, String errorMsg);
}
