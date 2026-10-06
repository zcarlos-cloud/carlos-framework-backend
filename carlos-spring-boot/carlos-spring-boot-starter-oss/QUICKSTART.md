# carlos-spring-boot-starter-oss 快速开始指南

## 1. 添加依赖

在项目的 `pom.xml` 中添加依赖：

```xml
<!-- OSS Starter -->
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-oss</artifactId>
    <version>3.0.0-SNAPSHOT</version>
</dependency>

<!-- 根据使用的云平台添加对应的 SDK 依赖 -->
<!-- 示例：使用 MinIO -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

## 2. 配置文件

在 `application.yml` 中添加配置：

```yaml
carlos:
  oss:
    enabled: true
    type: minio  # 可选值: minio, aliyun, tencent, qiniu, huawei, aws, local
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: default
    auto-create-bucket: true
```

## 3. 使用示例

### 3.1 注入 OssTemplate

```java
import com.carlos.oss.core.OssTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    @Autowired
    private OssTemplate ossTemplate;

    // 使用 ossTemplate 进行文件操作
}
```

### 3.2 上传文件

```java
import com.carlos.oss.model.OssFile;
import org.springframework.web.multipart.MultipartFile;

public OssFile uploadFile(MultipartFile file) throws Exception {
    String objectName = "uploads/" + file.getOriginalFilename();

    OssFile ossFile = ossTemplate.putObject(
        objectName,
        file.getInputStream(),
        file.getContentType()
    );

    System.out.println("文件上传成功，访问URL: " + ossFile.getUrl());
    return ossFile;
}
```

### 3.3 下载文件

```java
import java.io.InputStream;

public InputStream downloadFile(String objectName) {
    return ossTemplate.getObject(objectName);
}
```

### 3.4 删除文件

```java
public void deleteFile(String objectName) {
    ossTemplate.deleteObject(objectName);
    System.out.println("文件删除成功");
}
```

### 3.5 获取文件URL

```java
public String getFileUrl(String objectName) {
    return ossTemplate.getObjectUrl(objectName);
}
```

### 3.6 获取临时访问链接

```java
// 生成1小时有效的临时访问链接
public String getTemporaryUrl(String objectName) {
    return ossTemplate.getPresignedUrl("default", objectName, 3600);
}
```

## 4. 完整示例

### 4.1 文件上传控制器

```java
import com.carlos.core.response.Result;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.model.OssFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private OssTemplate ossTemplate;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<OssFile> upload(@RequestParam("file") MultipartFile file) {
        try {
            String objectName = "uploads/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
            OssFile ossFile = ossTemplate.putObject(objectName, file.getInputStream(), file.getContentType());
            return Result.success(ossFile);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件URL
     */
    @GetMapping("/url")
    public Result<String> getUrl(@RequestParam String objectName) {
        String url = ossTemplate.getObjectUrl(objectName);
        return Result.success(url);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam String objectName) {
        ossTemplate.deleteObject(objectName);
        return Result.success();
    }
}
```

### 4.2 文件服务类

```java
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.model.OssFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private OssTemplate ossTemplate;

    /**
     * 上传文件并返回访问URL
     */
    public String uploadAndGetUrl(MultipartFile file) throws Exception {
        // 生成唯一文件名
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String objectName = "uploads/" + UUID.randomUUID() + extension;

        // 上传文件
        OssFile ossFile = ossTemplate.putObject(objectName, file.getInputStream(), file.getContentType());

        // 返回访问URL
        return ossFile.getUrl();
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String objectName) {
        return ossTemplate.getObject(objectName);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        ossTemplate.deleteObject(objectName);
    }

    /**
     * 批量删除文件
     */
    public void deleteFiles(List<String> objectNames) {
        ossTemplate.deleteObjects("default", objectNames);
    }

    /**
     * 列出指定前缀的所有文件
     */
    public List<OssFile> listFiles(String prefix) {
        return ossTemplate.listObjects(prefix);
    }

    /**
     * 判断文件是否存在
     */
    public boolean fileExists(String objectName) {
        return ossTemplate.objectExists("default", objectName);
    }
}
```

## 5. 不同云平台配置

### 5.1 MinIO

```yaml
carlos:
  oss:
    type: minio
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: default
```

### 5.2 阿里云 OSS

```yaml
carlos:
  oss:
    type: aliyun
    aliyun:
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key-id: your-access-key-id
      access-key-secret: your-access-key-secret
      bucket-name: your-bucket
```

### 5.3 腾讯云 COS

```yaml
carlos:
  oss:
    type: tencent
    tencent:
      region: ap-guangzhou
      secret-id: your-secret-id
      secret-key: your-secret-key
      bucket-name: your-bucket-1234567890
```

### 5.4 七牛云 Kodo

```yaml
carlos:
  oss:
    type: qiniu
    qiniu:
      access-key: your-access-key
      secret-key: your-secret-key
      bucket-name: your-bucket
      domain: your-domain.com  # 必填
      region: z0
```

## 6. 常见问题

### Q1: 如何切换不同的云平台？

只需修改配置文件中的 `carlos.oss.type` 属性，并添加对应的 SDK 依赖即可。

### Q2: 如何配置公网访问地址？

使用 `public-endpoint` 配置项：

```yaml
carlos:
  oss:
    endpoint: http://192.168.1.100:9000  # 内网地址
    public-endpoint: http://oss.example.com  # 公网地址
```

### Q3: 如何禁用自动创建桶？

```yaml
carlos:
  oss:
    auto-create-bucket: false
```

### Q4: 如何上传到指定的桶？

```java
ossTemplate.putObject("my-bucket", "file.jpg", inputStream, "image/jpeg");
```

### Q5: 如何生成临时访问链接？

```java
// 生成1小时（3600秒）有效的临时链接
String url = ossTemplate.getPresignedUrl("default", "file.jpg", 3600);
```

## 7. 注意事项

1. **密钥安全**: 不要在代码中硬编码密钥，使用配置文件或环境变量
2. **依赖管理**: 只引入需要使用的云平台 SDK 依赖
3. **桶命名**: 遵循各云平台的桶命名规范（小写字母、数字、连字符）
4. **七牛云特殊说明**: 必须配置 `domain` 属性
5. **异常处理**: 建议使用 try-catch 捕获 `OssException`

## 8. 更多示例

查看完整示例代码：

- `com.carlos.oss.example.OssExampleController` - REST API 示例
- `com.carlos.oss.example.OssExampleService` - 服务层示例

查看详细文档：

- `README.md` - 完整使用文档
- `SUMMARY.md` - 模块创建总结

## 9. 技术支持

如有问题，请联系框架维护团队。
