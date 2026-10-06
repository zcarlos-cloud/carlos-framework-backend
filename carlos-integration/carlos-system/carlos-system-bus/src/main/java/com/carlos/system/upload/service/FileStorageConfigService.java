package com.carlos.system.upload.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.encrypt.EncryptUtil;
import com.carlos.oss.adapter.LocalOssTemplate;
import com.carlos.oss.adapter.S3OssTemplate;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.enums.OssType;
import com.carlos.oss.utils.OssUtil;
import com.carlos.system.upload.convert.FileStorageConfigConvert;
import com.carlos.system.upload.manager.FileStorageConfigManager;
import com.carlos.system.upload.pojo.dto.FileStorageConfigDTO;
import com.carlos.system.upload.pojo.enums.StorageTypeEnum;
import com.carlos.system.upload.pojo.param.FileStorageConfigCreateParam;
import com.carlos.system.upload.pojo.param.FileStorageConfigPageParam;
import com.carlos.system.upload.pojo.param.FileStorageConfigUpdateParam;
import com.carlos.system.upload.pojo.vo.FileStorageConfigVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/**
 * 文件存储配置 Service
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageConfigService {

    private final FileStorageConfigManager configManager;

    /**
     * 应用启动时，加载数据库中的默认配置并初始化 OssUtil
     */
    @PostConstruct
    public void initDefaultStorage() {
        try {
            FileStorageConfigDTO defaultConfig = configManager.getDefaultConfig();
            if (defaultConfig != null) {
                log.info("Loading default storage config from database: {}", defaultConfig.getConfigName());
                reloadOssTemplate(defaultConfig);
            } else {
                log.info("No default storage config found in database, using application.yml configuration");
            }
        } catch (Exception e) {
            log.warn("Failed to load default storage config from database: {}", e.getMessage());
        }
    }

    @Transactional
    public void create(FileStorageConfigCreateParam param) {
        FileStorageConfigDTO dto = FileStorageConfigConvert.INSTANCE.toDTO(param);
        // 加密敏感字段
        encryptSensitiveFields(dto);
        configManager.add(dto);
    }

    @Transactional
    public void update(FileStorageConfigUpdateParam param) {
        FileStorageConfigDTO existing = configManager.getDtoById(param.getId());
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        FileStorageConfigDTO dto = FileStorageConfigConvert.INSTANCE.toDTO(param);
        // 加密敏感字段
        encryptSensitiveFields(dto);
        configManager.modify(dto);
    }

    @Transactional
    public void delete(Set<Long> ids) {
        configManager.deleteBatch(ids);
    }

    public IPage<FileStorageConfigVO> page(FileStorageConfigPageParam param) {
        IPage<FileStorageConfigDTO> dtoPage = configManager.pageDto(param);
        return dtoPage.convert(this::toVOWithDesensitization);
    }

    public FileStorageConfigVO getById(Long id) {
        FileStorageConfigDTO dto = configManager.getDtoById(id);
        if (dto == null) {
            return null;
        }
        // 解密敏感字段
        decryptSensitiveFields(dto);
        return toVOWithDesensitization(dto);
    }

    public FileStorageConfigDTO getDefault() {
        FileStorageConfigDTO dto = configManager.getDefaultConfig();
        if (dto != null) {
            decryptSensitiveFields(dto);
        }
        return dto;
    }

    @Transactional
    public void setDefault(Long id) {
        FileStorageConfigDTO config = configManager.getDtoById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        if (config.getConfigStatus() != null && config.getConfigStatus() == 0) {
            throw new IllegalArgumentException("禁用的配置不能设为默认");
        }
        // 取消所有默认
        configManager.clearDefault();
        // 设置当前为默认
        configManager.setDefault(id);
        // 重新加载 Template
        decryptSensitiveFields(config);
        reloadOssTemplate(config);
        log.info("Switched default storage config to: {}", config.getConfigName());
    }

    /**
     * 测试存储配置连通性
     */
    public boolean testConnection(Long id) {
        FileStorageConfigDTO config = configManager.getDtoById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        decryptSensitiveFields(config);

        try {
            OssTemplate template = buildTemplate(config);
            String bucketName = StrUtil.isNotBlank(config.getBucketName()) ? config.getBucketName() : "test-bucket";
            template.bucketExists(bucketName);
            return true;
        } catch (Exception e) {
            log.warn("Storage config connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 重新初始化 OssUtil 的 Template
     */
    private synchronized void reloadOssTemplate(FileStorageConfigDTO config) {
        OssTemplate template = buildTemplate(config);
        OssProperties properties = buildOssProperties(config);
        OssUtil.init(template, properties);
        log.info("OssUtil reinitialized with config: {}, type: {}", config.getConfigName(), config.getStorageType());
    }

    /**
     * 根据配置构建 OssTemplate 实例（仅用于测试连通性）
     */
    private OssTemplate buildTemplate(FileStorageConfigDTO config) {
        if (StorageTypeEnum.LOCAL.getCode().equals(config.getStorageType())) {
            return new LocalOssTemplate(buildOssProperties(config));
        }
        // S3 类型
        OssProperties properties = buildOssProperties(config);
        String endpoint = properties.getActualEndpoint();
        String accessKey = properties.getAccessKey();
        String secretKey = properties.getSecretKey();
        String region = properties.getRegion();

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build();

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .region(Region.of(region))
                .overrideConfiguration(cfg -> cfg
                        .apiCallTimeout(Duration.ofMillis(properties.getSocketTimeout()))
                        .apiCallAttemptTimeout(Duration.ofMillis(properties.getConnectionTimeout())));
        if (StrUtil.isNotBlank(endpoint)) {
            s3ClientBuilder.endpointOverride(URI.create(endpoint));
        }
        S3Client s3Client = s3ClientBuilder.build();

        S3Presigner.Builder s3PresignerBuilder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region));
        if (StrUtil.isNotBlank(endpoint)) {
            s3PresignerBuilder.endpointOverride(URI.create(endpoint));
        }
        S3Presigner s3Presigner = s3PresignerBuilder.build();

        return new S3OssTemplate(properties, s3Client, s3Presigner);
    }

    /**
     * 根据配置构建 OssProperties
     */
    private OssProperties buildOssProperties(FileStorageConfigDTO config) {
        OssProperties properties = new OssProperties();
        properties.setBucketName(config.getBucketName());
        properties.setPublicEndpoint(config.getPublicEndpoint());
        properties.setAutoCreateBucket(true);

        if (StorageTypeEnum.LOCAL.getCode().equals(config.getStorageType())) {
            properties.setType(OssType.LOCAL);
            properties.setLocalBasePath(config.getLocalBasePath());
        } else {
            // S3 类型，根据 endpoint 推断具体类型，默认 CUSTOM
            properties.setType(OssType.CUSTOM);
            properties.setEndpoint(config.getEndpoint());
            properties.setAccessKey(config.getAccessKey());
            properties.setSecretKey(config.getSecretKey());
            properties.setRegion(StrUtil.isNotBlank(config.getRegion()) ? config.getRegion() : "us-east-1");
            properties.setPathStyleAccess(config.getPathStyleAccess() != null ? config.getPathStyleAccess() : true);
        }
        return properties;
    }

    /**
     * 加密敏感字段
     */
    private void encryptSensitiveFields(FileStorageConfigDTO dto) {
        if (StrUtil.isNotBlank(dto.getAccessKey())) {
            dto.setAccessKey(EncryptUtil.encrypt(dto.getAccessKey()));
        }
        if (StrUtil.isNotBlank(dto.getSecretKey())) {
            dto.setSecretKey(EncryptUtil.encrypt(dto.getSecretKey()));
        }
    }

    /**
     * 解密敏感字段
     */
    private void decryptSensitiveFields(FileStorageConfigDTO dto) {
        if (dto == null) {
            return;
        }
        if (StrUtil.isNotBlank(dto.getAccessKey())) {
            try {
                dto.setAccessKey(EncryptUtil.decrypt(dto.getAccessKey()));
            } catch (Exception e) {
                log.warn("Failed to decrypt accessKey for config id:{}", dto.getId());
            }
        }
        if (StrUtil.isNotBlank(dto.getSecretKey())) {
            try {
                dto.setSecretKey(EncryptUtil.decrypt(dto.getSecretKey()));
            } catch (Exception e) {
                log.warn("Failed to decrypt secretKey for config id:{}", dto.getId());
            }
        }
    }

    /**
     * 转换为 VO 并对敏感字段脱敏
     */
    private FileStorageConfigVO toVOWithDesensitization(FileStorageConfigDTO dto) {
        FileStorageConfigVO vo = FileStorageConfigConvert.INSTANCE.toVO(dto);
        if (vo != null && StrUtil.isNotBlank(vo.getSecretKey())) {
            vo.setSecretKey(desensitizeKey(vo.getSecretKey()));
        }
        return vo;
    }

    /**
     * 密钥脱敏：显示前4位和后4位，中间用 **** 替代
     */
    private String desensitizeKey(String key) {
        if (StrUtil.isBlank(key) || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
