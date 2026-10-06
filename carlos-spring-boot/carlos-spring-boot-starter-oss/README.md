# carlos-spring-boot-starter-oss

统一对象存储服务（OSS）Spring Boot Starter，基于 **AWS S3 协议**，提供跨云平台的统一文件存储操作接口。

## 核心设计理念

**使用 AWS S3 协议统一兼容多种云存储平台**，而不是为每个云平台创建单独的适配器。这种设计具有以下优势：

- ✅ **代码简洁** - 只需一个 S3OssTemplate 实现，代码量减少 70%+
- ✅ **易于维护** - 统一的实现逻辑，降低维护成本
- ✅ **广泛兼容** - 主流云平台都支持 S3 协议
- ✅ **标准化** - S3 已成为对象存储的事实标准
- ✅ **灵活切换** - 只需修改配置即可切换云平台

## 功能特性

- 🌐 **多云平台支持**：基于 S3 协议，支持所有兼容 S3 的云存储服务
- 🔌 **统一接口**：提供统一的 `OssTemplate` 接口，屏蔽不同云平台的差异
- 🚀 **开箱即用**：基于 Spring Boot 自动配置，简化集成流程
- 🔧 **灵活配置**：通过配置不同的 endpoint 访问不同云平台
- 📦 **轻量级**：只依赖 AWS S3 SDK，无需引入多个云平台 SDK

## 支持的云平台

所有兼容 S3 协议的对象存储服务都可以使用，包括但不限于：

| 云平台      | 类型标识      | S3 兼容性  | 默认 Endpoint                              |
|----------|-----------|---------|------------------------------------------|
| AWS S3   | `aws`     | ✅ 原生支持  | https://s3.amazonaws.com                 |
| MinIO    | `minio`   | ✅ 完全兼容  | http://localhost:9000                    |
| 阿里云 OSS  | `aliyun`  | ✅ 兼容 S3 | https://oss-cn-hangzhou.aliyuncs.com     |
| 腾讯云 COS  | `tencent` | ✅ 兼容 S3 | https://cos.ap-guangzhou.myqcloud.com    |
| 华为云 OBS  | `huawei`  | ✅ 兼容 S3 | https://obs.cn-north-4.myhuaweicloud.com |
| 七牛云 Kodo | `qiniu`   | ✅ 兼容 S3 | https://s3-cn-east-1.qiniucs.com         |
| 京东云 OSS  | `jd`      | ✅ 兼容 S3 | https://s3.cn-north-1.jdcloud-oss.com    |
| 其他       | `custom`  | ✅ 兼容 S3 | 自定义                                      |

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-oss</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

**注意**：无需添加其他云平台 SDK 依赖，只需要 AWS S3 SDK（已包含在 starter 中）。

### 2. 配置文件

#### MinIO 配置示例

```yaml
carlos:
  oss:
    enabled: true
    type: minio
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    region: us-east-1
    bucket-name: default
    path-style-access: true  # MinIO 使用路径样式访问
```

#### AWS S3 配置示例

```yaml
carlos:
  oss:
    enabled: true
    type: aws
    access-key: your-access-key-id
    secret-key: your-secret-access-key
    region: us-east-1
    bucket-name: your-bucket
    path-style-access: false  # AWS S3 推荐虚拟主机样式
```

#### 阿里云 OSS 配置示例

```yaml
carlos:
  oss:
    enabled: true
    type: aliyun
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key: your-access-key-id
    secret-key: your-access-key-secret
    region: cn-hangzhou
    bucket-name: your-bucket
    path-style-access: true
```

#### 腾讯云 COS 配置示例

```yaml
carlos:
  oss:
    enabled: true
    type: tencent
    endpoint: https://cos.ap-guangzhou.myqcloud.com
    access-key: your-secret-id
    secret-key: your-secret-key
    region: ap-guangzhou
    bucket-name: your-bucket
    path-style-access: true
```

### 3. 使用示例

```java
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.model.OssFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Autowired
    private OssTemplate ossTemplate;

    /**
     * 上传文件
     */
    public OssFile uploadFile(MultipartFile file) throws Exception {
        String objectName = "uploads/" + file.getOriginalFilename();
        return ossTemplate.putObject(
            objectName,
            file.getInputStream(),
            file.getContentType()
        );
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String objectName) {
        return ossTemplate.getObject(objectName);
    }

    /**
     * 获取文件URL
     */
    public String getFileUrl(String objectName) {
        return ossTemplate.getObjectUrl(objectName);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        ossTemplate.deleteObject(objectName);
    }

    /**
     * 列出文件
     */
    public List<OssFile> listFiles(String prefix) {
        return ossTemplate.listObjects(prefix);
    }
}
```

## 核心接口

### OssTemplate

统一的对象存储操作接口，提供以下方法：

#### 存储桶操作

- `void createBucket(String bucketName)` - 创建存储桶
- `void deleteBucket(String bucketName)` - 删除存储桶
- `boolean bucketExists(String bucketName)` - 判断存储桶是否存在
- `List<String> listBuckets()` - 列出所有存储桶

#### 文件上传

- `OssFile putObject(String bucketName, String objectName, InputStream inputStream, String contentType)` - 上传文件
- `OssFile putObject(String objectName, InputStream inputStream, String contentType)` - 上传文件（使用默认桶）
- `OssFile putObject(String bucketName, String objectName, byte[] bytes, String contentType)` - 上传文件（字节数组）

#### 文件下载

- `InputStream getObject(String bucketName, String objectName)` - 获取文件流
- `InputStream getObject(String objectName)` - 获取文件流（使用默认桶）
- `OssFile getObjectInfo(String bucketName, String objectName)` - 获取文件信息

#### 文件删除

- `void deleteObject(String bucketName, String objectName)` - 删除文件
- `void deleteObject(String objectName)` - 删除文件（使用默认桶）
- `void deleteObjects(String bucketName, List<String> objectNames)` - 批量删除文件

#### 文件操作

- `void copyObject(String sourceBucket, String sourceObject, String targetBucket, String targetObject)` - 复制文件
- `List<OssFile> listObjects(String bucketName, String prefix)` - 列出文件
- `List<OssFile> listObjects(String prefix)` - 列出文件（使用默认桶）

#### URL生成

- `String getObjectUrl(String bucketName, String objectName)` - 获取文件访问URL
- `String getObjectUrl(String objectName)` - 获取文件访问URL（使用默认桶）
- `String getPresignedUrl(String bucketName, String objectName, int expires)` - 获取预签名URL

#### 文件检查

- `boolean objectExists(String bucketName, String objectName)` - 判断文件是否存在

## 配置说明

### 核心配置项

```yaml
carlos:
  oss:
    enabled: true                    # 是否启用，默认 true
    type: minio                      # 云平台类型（用于选择默认 endpoint）
    endpoint: http://localhost:9000  # S3 访问端点（必填）
    public-endpoint: http://...      # 公网访问端点（可选）
    access-key: your-access-key      # Access Key ID
    secret-key: your-secret-key      # Secret Access Key
    region: us-east-1                # 区域
    bucket-name: default             # 默认存储桶名称
    path-style-access: true          # 是否使用路径样式访问
    auto-create-bucket: true         # 是否自动创建桶
    presigned-url-expires: 3600      # 预签名URL过期时间（秒）
    connection-timeout: 10000        # 连接超时时间（毫秒）
    socket-timeout: 60000            # 读取超时时间（毫秒）
    max-connections: 50              # 最大连接数
    use-https: false                 # 是否使用 HTTPS
```

### 路径样式访问说明

- **路径样式（Path Style）**: `http://endpoint/bucket/key`
    - MinIO、阿里云、腾讯云等通常使用此方式
    - 配置：`path-style-access: true`

- **虚拟主机样式（Virtual Hosted Style）**: `http://bucket.endpoint/key`
    - AWS S3 推荐使用此方式
    - 配置：`path-style-access: false`

## 架构设计

### 设计模式

1. **统一协议** - 使用 AWS S3 协议作为统一标准
2. **模板方法** - `AbstractOssTemplate` 提供通用实现
3. **策略模式** - 通过配置选择不同的云平台
4. **工厂模式** - `OssAutoConfiguration` 自动创建实例

### 技术栈

- **AWS S3 SDK** - 2.20.26
- **Spring Boot** - 3.5.9
- **Hutool** - 5.8.40
- **Lombok** - 编译时注解处理

### 模块结构

```
carlos-spring-boot-starter-oss/
├── adapter/
│   └── S3OssTemplate.java          # S3 协议统一实现
├── config/
│   ├── OssAutoConfiguration.java   # 自动配置
│   └── OssProperties.java          # 配置属性
├── core/
│   ├── AbstractOssTemplate.java    # 抽象模板
│   └── OssTemplate.java            # 统一接口
├── enums/
│   └── OssType.java                # 云平台类型
├── exception/
│   └── OssException.java           # OSS 异常
├── model/
│   └── OssFile.java                # 文件信息
└── example/
    ├── OssExampleController.java   # 示例控制器
    └── OssExampleService.java      # 示例服务
```

## 云平台配置指南

### MinIO

```yaml
carlos:
  oss:
    type: minio
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    region: us-east-1
    path-style-access: true
```

### AWS S3

```yaml
carlos:
  oss:
    type: aws
    # endpoint 可以不配置，使用默认值
    access-key: your-access-key-id
    secret-key: your-secret-access-key
    region: us-east-1
    path-style-access: false
```

### 阿里云 OSS

```yaml
carlos:
  oss:
    type: aliyun
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key: your-access-key-id
    secret-key: your-access-key-secret
    region: cn-hangzhou
    path-style-access: true
```

### 腾讯云 COS

```yaml
carlos:
  oss:
    type: tencent
    endpoint: https://cos.ap-guangzhou.myqcloud.com
    access-key: your-secret-id
    secret-key: your-secret-key
    region: ap-guangzhou
    path-style-access: true
```

### 华为云 OBS

```yaml
carlos:
  oss:
    type: huawei
    endpoint: https://obs.cn-north-4.myhuaweicloud.com
    access-key: your-access-key-id
    secret-key: your-secret-access-key
    region: cn-north-4
    path-style-access: true
```

## 常见问题

### Q1: 为什么使用 S3 协议而不是各云平台原生 SDK？

**A**: 使用 S3 协议有以下优势：

- 代码更简洁，只需一个实现
- 易于维护和扩展
- S3 已成为对象存储的事实标准
- 主流云平台都支持 S3 协议
- 降低依赖冲突风险

### Q2: 如何切换不同的云平台？

**A**: 只需修改配置文件中的 `type` 和 `endpoint` 即可：

```yaml
# 从 MinIO 切换到阿里云 OSS
carlos:
  oss:
    type: aliyun  # 修改类型
    endpoint: https://oss-cn-hangzhou.aliyuncs.com  # 修改 endpoint
    access-key: your-aliyun-key
    secret-key: your-aliyun-secret
```

### Q3: 是否支持所有 S3 功能？

**A**: 本模块实现了常用的 S3 功能，包括：

- 存储桶管理
- 文件上传/下载
- 文件删除/复制
- 文件列表
- 预签名 URL

如需使用高级功能，可以直接注入 `S3Client` 使用原生 API。

### Q4: 如何配置公网访问地址？

**A**: 使用 `public-endpoint` 配置项：

```yaml
carlos:
  oss:
    endpoint: http://192.168.1.100:9000  # 内网地址
    public-endpoint: http://oss.example.com  # 公网地址
```

### Q5: 某些云平台的特殊功能如何使用？

**A**: 可以直接注入 `S3Client` 使用原生 API：

```java
@Autowired
private S3Client s3Client;

// 使用原生 API
s3Client.putObject(...);
```

## 注意事项

1. **密钥安全**: 不要在代码中硬编码密钥，使用配置文件或环境变量
2. **Region 配置**: 确保 region 配置正确，否则可能无法访问
3. **路径样式**: 不同云平台对路径样式的支持不同，需要正确配置
4. **Endpoint 格式**: 确保 endpoint 包含协议（http:// 或 https://）
5. **兼容性**: 虽然都支持 S3 协议，但某些高级功能可能存在差异

## 版本要求

- JDK 17+
- Spring Boot 3.5.9+
- carlos-spring-boot-core 3.0.0+

## 许可证

内部使用

## 作者

carlos - 2026-02-01

## 更新日志

### v3.0.0-SNAPSHOT (2026-02-01)

- ✅ 重构为基于 S3 协议的统一实现
- ✅ 移除各云平台单独的适配器
- ✅ 简化配置方式
- ✅ 减少依赖，只需 AWS S3 SDK
- ✅ 支持 8+ 个云平台
- ✅ 完善的文档和示例
