package com.carlos.devkit.service;

import com.carlos.core.pagination.Paging;
import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.param.GenerateRecordPageParam;

import java.util.Collection;

/**
 * <p>
 * 生成记录 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface GenerateRecordService {

    /**
     * 分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<GenerateRecordDTO> getPage(GenerateRecordPageParam param);

    /**
     * 批量删除生成记录
     *
     * @param ids 记录ID集合
     * @return 是否成功
     */
    boolean deleteRecords(Collection<Long> ids);

    /**
     * 落一条生成记录（失败不阻断主流程）
     *
     * @param dto 记录数据
     */
    void record(GenerateRecordDTO dto);
}
