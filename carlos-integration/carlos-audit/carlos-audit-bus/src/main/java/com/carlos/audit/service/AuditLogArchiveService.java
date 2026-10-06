package com.carlos.audit.service;

/**
 * 审计日志冷数据归档服务
 *
 * <p>将超过冷数据天数的 ClickHouse 分区导出并上传至对象存储，随后清理源数据</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
public interface AuditLogArchiveService {

    /**
     * 归档冷数据
     *
     * <p>扫描 ClickHouse 中超过 {@code carlos.audit.archive.cold-days} 天的分区，
     * 导出为 CSV 后上传至对象存储，并在 MySQL 记录归档元数据</p>
     */
    void archiveColdData();

    /**
     * 从对象存储恢复归档数据
     *
     * <p>根据 archiveId 查询归档记录，从 OSS 下载 CSV，解析后重新写入 ClickHouse，
     * 并将归档记录状态更新为 RESTORED</p>
     *
     * @param archiveId 归档批次 ID（UUID）
     */
    void restoreArchive(String archiveId);
}
