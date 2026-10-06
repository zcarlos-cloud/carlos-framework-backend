package com.carlos.devkit.service.impl;

import com.carlos.devkit.engine.model.ConnectionTestResult;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.engine.support.JdbcMetaReader;
import com.carlos.devkit.manager.DataSourceManager;
import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.enums.DbTypeEnum;
import com.carlos.devkit.support.CredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据源连接的密码透传回归测试。
 *
 * <p>「编辑已有数据源且未重填密码」分支必须把**库中已存密码解密后**传给 JDBC 层。
 * 早先 {@code toTargetDatabase} 漏掉 password，导致该分支必然报
 * {@code SCRAM-based authentication, but no password was provided}
 * —— 而 {@code resolveTarget} 因为另有显式 setPassword 才没暴露，所以这里专门钉死。</p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@ExtendWith(MockitoExtension.class)
class DataSourceServiceImplPasswordPassThroughTest {

    @Mock
    private DataSourceManager dataSourceManager;

    @Mock
    private CredentialCipher credentialCipher;

    @Mock
    private JdbcMetaReader jdbcMetaReader;

    private DataSourceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataSourceServiceImpl(dataSourceManager, credentialCipher, jdbcMetaReader);
    }

    @Test
    @DisplayName("传 id 且密码留空时，应用库中已存密码（解密后）建连")
    void should_use_stored_password_when_blank() {
        DataSourceDTO stored = new DataSourceDTO()
            .setId(1L)
            .setDsName("demo")
            .setDbType(DbTypeEnum.POSTGRESQL)
            .setHost("10.0.0.1")
            .setPort(5432)
            .setDatabaseName("demo_db")
            .setUsername("demo")
            .setPasswordCipher("CIPHER")
            .setStatus(1);
        when(dataSourceManager.getDtoById(1L)).thenReturn(stored);
        when(credentialCipher.decrypt("CIPHER")).thenReturn("plain-secret");
        when(jdbcMetaReader.testConnection(any())).thenReturn(ConnectionTestResult.success("PG", "16", 1L));

        // 表单只带 id 与连接信息，password 留空
        DataSourceDTO param = new DataSourceDTO()
            .setId(1L)
            .setDbType(DbTypeEnum.POSTGRESQL)
            .setHost("10.0.0.1")
            .setPort(5432)
            .setDatabaseName("demo_db")
            .setUsername("demo");

        service.testConnection(param);

        ArgumentCaptor<TargetDatabase> captor = ArgumentCaptor.forClass(TargetDatabase.class);
        verify(jdbcMetaReader).testConnection(captor.capture());
        assertEquals("plain-secret", captor.getValue().getPassword(),
            "库中已存密码应解密后透传给 JDBC 层");
    }

    @Test
    @DisplayName("表单重填了密码时，用表单密码而不是库中已存密码")
    void should_prefer_form_password() {
        DataSourceDTO param = new DataSourceDTO()
            .setId(1L)
            .setDbType(DbTypeEnum.POSTGRESQL)
            .setHost("10.0.0.1")
            .setPort(5432)
            .setDatabaseName("demo_db")
            .setUsername("demo")
            .setPassword("form-secret");
        when(jdbcMetaReader.testConnection(any())).thenReturn(ConnectionTestResult.success("PG", "16", 1L));

        service.testConnection(param);

        ArgumentCaptor<TargetDatabase> captor = ArgumentCaptor.forClass(TargetDatabase.class);
        verify(jdbcMetaReader).testConnection(captor.capture());
        assertEquals("form-secret", captor.getValue().getPassword());
    }
}
