package com.carlos.devkit.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.devkit.engine.model.ConnectionTestResult;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.engine.support.JdbcMetaReader;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.manager.DataSourceManager;
import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.enums.DbTypeEnum;
import com.carlos.devkit.pojo.enums.DevkitStatusEnum;
import com.carlos.devkit.pojo.param.DataSourcePageParam;
import com.carlos.devkit.service.DataSourceService;
import com.carlos.devkit.support.CredentialCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 受管数据源 业务接口实现类
 * </p>
 * <p>
 * 明文口令只在 {@link #addDataSource} / {@link #updateDataSource} / {@link #resolveTarget} /
 * {@link #testConnection} 四个方法内出现，且不落日志、不出本类。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceManager dataSourceManager;

    private final CredentialCipher credentialCipher;

    private final JdbcMetaReader jdbcMetaReader;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDataSource(final DataSourceDTO dto) {
        if (dataSourceManager.countByName(dto.getDsName(), null) > 0) {
            throw DevkitErrorCode.DATASOURCE_CONNECT_FAILED.exception("数据源名称已存在");
        }
        DataSourceDTO save = new DataSourceDTO()
            .setDsName(dto.getDsName())
            .setDsCode(dto.getDsCode())
            .setDbType(dto.getDbType())
            .setHost(dto.getHost())
            .setPort(dto.getPort())
            .setDatabaseName(dto.getDatabaseName())
            .setSchemaName(dto.getSchemaName())
            .setUsername(dto.getUsername())
            .setPasswordCipher(credentialCipher.encrypt(dto.getPassword()))
            .setJdbcParams(dto.getJdbcParams())
            .setReadOnly(dto.getReadOnly() == null ? Boolean.TRUE : dto.getReadOnly())
            .setStatus(DevkitStatusEnum.ENABLED.getCode())
            .setRemark(dto.getRemark());
        dataSourceManager.add(save);
        log.info("新增数据源, id:{}, dsName:{}, dbType:{}", save.getId(), save.getDsName(), save.getDbType());
        return save.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDataSource(final DataSourceDTO dto) {
        DataSourceDTO old = requireDataSource(dto.getId());
        if (dataSourceManager.countByName(dto.getDsName(), dto.getId()) > 0) {
            throw DevkitErrorCode.DATASOURCE_CONNECT_FAILED.exception("数据源名称已存在");
        }
        DataSourceDTO update = new DataSourceDTO()
            .setId(old.getId())
            .setDsName(dto.getDsName())
            .setDsCode(dto.getDsCode())
            .setDbType(dto.getDbType())
            .setHost(dto.getHost())
            .setPort(dto.getPort())
            .setDatabaseName(dto.getDatabaseName())
            .setSchemaName(dto.getSchemaName())
            .setUsername(dto.getUsername())
            .setJdbcParams(dto.getJdbcParams())
            .setReadOnly(dto.getReadOnly())
            .setStatus(dto.getStatus())
            .setRemark(dto.getRemark());
        // 密码留空表示保持原值（绝不接受前端回填的密文）
        if (StrUtil.isNotBlank(dto.getPassword())) {
            update.setPasswordCipher(credentialCipher.encrypt(dto.getPassword()));
        }
        return dataSourceManager.modify(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDataSources(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw DevkitErrorCode.DATASOURCE_NOT_FOUND.exception("请选择要删除的数据源");
        }
        return dataSourceManager.delete(new ArrayList<>(ids));
    }

    @Override
    public DataSourceDTO getDetail(final Serializable id) {
        DataSourceDTO dto = requireDataSource(id);
        dto.setHasPassword(StrUtil.isNotBlank(dto.getPasswordCipher()));
        return dto;
    }

    @Override
    public Paging<DataSourceDTO> getPage(final DataSourcePageParam param) {
        Paging<DataSourceDTO> paging = dataSourceManager.getPage(param);
        fillHasPassword(paging.getRecords());
        return paging;
    }

    @Override
    public List<DataSourceDTO> listEnabled() {
        List<DataSourceDTO> list = dataSourceManager.listEnabled();
        fillHasPassword(list);
        return list;
    }

    /**
     * 批量回填「是否已配置密码」
     *
     * <p>列表查询故意不取 {@code password_cipher}，所以只能另做一次只取主键的轻查询。
     * 早先直接由 {@code passwordCipher} 推导，导致列表里 hasPassword 恒为 false。</p>
     *
     * @param records 数据源列表
     */
    private void fillHasPassword(final List<DataSourceDTO> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        List<Long> ids = records.stream().map(DataSourceDTO::getId).toList();
        Set<Long> withPassword = dataSourceManager.listIdsWithPassword(ids);
        records.forEach(item -> item.setHasPassword(withPassword.contains(item.getId())));
    }

    @Override
    public ConnectionTestResult testConnection(final DataSourceDTO dto) {
        DataSourceDTO effective = dto;
        // 编辑已有数据源且未重填密码时，用库中已存密码测试
        if (dto.getId() != null && StrUtil.isBlank(dto.getPassword())) {
            DataSourceDTO stored = requireDataSource(dto.getId());
            effective = new DataSourceDTO()
                .setId(stored.getId())
                .setDbType(dto.getDbType() != null ? dto.getDbType() : stored.getDbType())
                .setHost(StrUtil.blankToDefault(dto.getHost(), stored.getHost()))
                .setPort(dto.getPort() != null ? dto.getPort() : stored.getPort())
                .setDatabaseName(StrUtil.blankToDefault(dto.getDatabaseName(), stored.getDatabaseName()))
                .setSchemaName(StrUtil.blankToDefault(dto.getSchemaName(), stored.getSchemaName()))
                .setUsername(StrUtil.blankToDefault(dto.getUsername(), stored.getUsername()))
                .setJdbcParams(StrUtil.blankToDefault(dto.getJdbcParams(), stored.getJdbcParams()))
                .setPassword(credentialCipher.decrypt(stored.getPasswordCipher()));
        }
        return jdbcMetaReader.testConnection(toTargetDatabase(effective));
    }

    @Override
    public TargetDatabase resolveTarget(final Serializable dsId, final String schema) {
        DataSourceDTO dto = requireDataSource(dsId);
        if (!DevkitStatusEnum.ENABLED.getCode().equals(dto.getStatus())) {
            throw DevkitErrorCode.DATASOURCE_DISABLED.exception(dto.getDsName());
        }
        TargetDatabase target = toTargetDatabase(dto);
        target.setPassword(credentialCipher.decrypt(dto.getPasswordCipher()));
        if (StrUtil.isNotBlank(schema)) {
            // MySQL 的「schema」是库名；PostgreSQL 的「schema」是模式名
            if (DbTypeEnum.MYSQL == dto.getDbType()) {
                target.setDatabaseName(schema);
            } else {
                target.setSchemaName(schema);
            }
        }
        return target;
    }

    /**
     * 校验数据源存在并返回
     *
     * @param id 数据源ID
     * @return 数据源 DTO
     */
    private DataSourceDTO requireDataSource(final Serializable id) {
        DataSourceDTO dto = dataSourceManager.getDtoById(id);
        if (dto == null) {
            throw DevkitErrorCode.DATASOURCE_NOT_FOUND.exception();
        }
        return dto;
    }

    /**
     * DTO → 引擎目标数据库
     *
     * <p>⚠️ 必须把明文口令一并带过去：{@code testConnection} 依赖它建连。
     * 早先漏了这一行，导致走「用库里已存密码测试连接」分支时必然报
     * {@code The server requested SCRAM-based authentication, but no password was provided}
     * —— 而 {@code resolveTarget} 因为是显式 {@code setPassword(decrypt(...))} 才没暴露。</p>
     *
     * @param dto 数据源 DTO
     * @return 目标数据库
     */
    private TargetDatabase toTargetDatabase(final DataSourceDTO dto) {
        if (dto.getDbType() == null) {
            throw DevkitErrorCode.DATASOURCE_DB_TYPE_UNSUPPORTED.exception("数据库类型不能为空");
        }
        return new TargetDatabase()
            .setDbType(dto.getDbType())
            .setHost(dto.getHost())
            .setPort(String.valueOf(dto.getPort()))
            .setDatabaseName(dto.getDatabaseName())
            .setSchemaName(dto.getSchemaName())
            .setUsername(dto.getUsername())
            .setPassword(dto.getPassword())
            .setJdbcParams(dto.getJdbcParams());
    }
}
