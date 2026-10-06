package com.carlos.system.upload.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.upload.manager.UploadRecordManager;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.entity.UploadRecord;
import com.carlos.system.upload.pojo.param.UploadRecordPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 文件上传记录 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadRecordService {

    private final UploadRecordManager recordManager;


    public void addUploadRecord(final UploadRecordDTO dto) {
        final boolean success = recordManager.add(dto);
        if (!success) {
            return;
        }
        final Serializable id = dto.getId();
    }


    public void deleteUploadRecord(final Set<Long> ids) {
        final boolean success = recordManager.deleteBatch(ids);
        if (!success) {
            log.warn("Batch delete upload record failed, ids: {}", ids);
        }
    }


    public void updateUploadRecord(final UploadRecordDTO dto) {
        final boolean success = recordManager.modify(dto);
        if (!success) {
            return;
        }
    }


    public UploadRecordDTO getInfoById(final Long id) {
        final UploadRecordDTO dto = recordManager.getDtoById(id);
        if (dto == null) {
            throw SystemErrorCode.SYS_FILE_NOT_FOUND.exception("文件不存在！id=" + id);
        }
        return dto;
    }


    public List<UploadRecordDTO> getInfoByGroupId(final String groupId) {
        return recordManager.getDtoByGroupId(groupId);
    }


    public List<UploadRecordDTO> getInfoByIds(final Set<Long> ids) {
        return recordManager.getDtoByIds(ids);
    }

    public IPage<UploadRecordDTO> page(final UploadRecordPageParam param) {
        return recordManager.pageDto(param);
    }

    public long count() {
        return recordManager.count();
    }

    public long totalSize() {
        List<UploadRecord> records = recordManager.lambdaQuery().list();
        return records.stream()
            .mapToLong(r -> r.getFileSize() != null ? r.getFileSize() : 0L)
            .sum();
    }

    public long todayUploadCount() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        return recordManager.lambdaQuery()
            .between(UploadRecord::getCreateTime, startOfDay, endOfDay)
            .count();
    }

    public Map<String, Long> typeDistribution() {
        List<UploadRecord> records = recordManager.lambdaQuery().list();
        return records.stream()
            .filter(r -> r.getFileExtension() != null)
            .collect(Collectors.groupingBy(UploadRecord::getFileExtension, Collectors.counting()));
    }
}
