package com.carlos.audit.recovery;

import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.config.AuditProperties;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link BackupRecoveryService} 单元测试
 *
 * @author Carlos
 * @date 2026-06-13
 */
@ExtendWith(MockitoExtension.class)
class BackupRecoveryServiceTest {

    @Mock
    private ClickHouseBatchWriter batchWriter;

    @Mock
    private AuditProperties auditProperties;

    @InjectMocks
    private BackupRecoveryService recoveryService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("recoverFile：应正确解析备份 CSV 并写入 batchWriter")
    void should_parse_backup_csv_and_write_to_batch_writer() throws Exception {
        // given
        Path csvFile = tempDir.resolve("audit-backup-2026-06-13.csv");
        String csvContent = "backup_time,id,server_time,log_type,principal_id,operation,reason\n"
            + "2026-06-13 10:00:00,1001,2026-06-13 09:30:00,USER_LOGIN,user123,用户登录,CLICKHOUSE_WRITE_FAILED\n"
            + "2026-06-13 10:00:00,1002,2026-06-13 09:31:00,ORDER_CREATE,user456,创建订单,CLICKHOUSE_WRITE_FAILED\n";
        Files.writeString(csvFile, csvContent);

        // when
        int count = recoveryService.recoverFile(csvFile);

        // then
        assertEquals(2, count);
        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(2)).add(captor.capture());
        assertEquals(1001L, captor.getAllValues().get(0).getId());
        assertEquals("USER_LOGIN", captor.getAllValues().get(0).getLogType());
        assertEquals("user123", captor.getAllValues().get(0).getPrincipalId());
        assertEquals("用户登录", captor.getAllValues().get(0).getOperation());
        assertEquals(1002L, captor.getAllValues().get(1).getId());
        assertEquals("ORDER_CREATE", captor.getAllValues().get(1).getLogType());
    }

    @Test
    @DisplayName("recoverFile：空文件应返回 0")
    void should_return_zero_for_empty_file() throws Exception {
        // given
        Path csvFile = tempDir.resolve("empty.csv");
        Files.writeString(csvFile, "backup_time,id,server_time,log_type,principal_id,operation,reason\n");

        // when
        int count = recoveryService.recoverFile(csvFile);

        // then
        assertEquals(0, count);
        verify(batchWriter, never()).add(any());
    }

    @Test
    @DisplayName("recoverFile：应跳过字段不足的行")
    void should_skip_incomplete_lines() throws Exception {
        // given
        Path csvFile = tempDir.resolve("bad.csv");
        String csvContent = "backup_time,id,server_time,log_type,principal_id,operation,reason\n"
            + "bad-line\n"
            + "2026-06-13 10:00:00,2001,2026-06-13 09:30:00,USER_LOGIN,user123,用户登录,CLICKHOUSE_WRITE_FAILED\n";
        Files.writeString(csvFile, csvContent);

        // when
        int count = recoveryService.recoverFile(csvFile);

        // then
        assertEquals(1, count);
        verify(batchWriter, times(1)).add(any());
    }

    @Test
    @DisplayName("recoverFile：应正确处理带引号的 CSV 字段")
    void should_handle_quoted_csv_fields() throws Exception {
        // given
        Path csvFile = tempDir.resolve("quoted.csv");
        String csvContent = "backup_time,id,server_time,log_type,principal_id,operation,reason\n"
            + "2026-06-13 10:00:00,3001,2026-06-13 09:30:00,\"LOG,TYPE\",\"user,123\",\"操作,包含逗号\",CLICKHOUSE_WRITE_FAILED\n";
        Files.writeString(csvFile, csvContent);

        // when
        int count = recoveryService.recoverFile(csvFile);

        // then
        assertEquals(1, count);
        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        assertEquals("LOG,TYPE", captor.getValue().getLogType());
        assertEquals("user,123", captor.getValue().getPrincipalId());
        assertEquals("操作,包含逗号", captor.getValue().getOperation());
    }
}
