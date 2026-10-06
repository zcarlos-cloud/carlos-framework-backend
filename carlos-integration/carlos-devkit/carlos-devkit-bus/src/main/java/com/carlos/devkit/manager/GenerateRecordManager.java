package com.carlos.devkit.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.entity.DevGenerateRecord;
import com.carlos.devkit.pojo.param.GenerateRecordPageParam;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 * 生成记录 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface GenerateRecordManager extends BaseService<DevGenerateRecord> {

    /**
     * 追加生成记录
     *
     * @param dto 记录数据
     * @return 是否成功
     */
    boolean add(GenerateRecordDTO dto);

    /**
     * 分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<GenerateRecordDTO> getPage(GenerateRecordPageParam param);

    /**
     * 逻辑删除生成记录
     *
     * @param ids 记录ID集合
     * @return 是否成功
     */
    boolean delete(Collection<Serializable> ids);
}
