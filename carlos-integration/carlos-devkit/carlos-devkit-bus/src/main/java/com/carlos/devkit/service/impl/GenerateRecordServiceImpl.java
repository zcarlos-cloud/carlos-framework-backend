package com.carlos.devkit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.manager.GenerateRecordManager;
import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.param.GenerateRecordPageParam;
import com.carlos.devkit.service.GenerateRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * 生成记录 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateRecordServiceImpl implements GenerateRecordService {

    private final GenerateRecordManager generateRecordManager;

    @Override
    public Paging<GenerateRecordDTO> getPage(final GenerateRecordPageParam param) {
        return generateRecordManager.getPage(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRecords(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw DevkitErrorCode.GENERATE_PARAM_INVALID.exception("请选择要删除的生成记录");
        }
        return generateRecordManager.delete(new ArrayList<>(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void record(final GenerateRecordDTO dto) {
        try {
            dto.setErrorMsg(StrUtil.maxLength(dto.getErrorMsg(), 1000));
            generateRecordManager.add(dto);
        } catch (Exception e) {
            // 记录生成历史不能反过来打断生成主流程
            log.warn("写入生成记录失败，忽略: {}", e.getMessage());
        }
    }
}
