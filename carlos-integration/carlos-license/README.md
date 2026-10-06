# Carlos License 许可证管理组件

基于 TrueLicense 的软件许可证管理解决方案，支持硬件指纹验证和时间约束，适用于内网部署的商业软件授权管理。

## 目录

- [概述](#概述)
- [模块结构](#模块结构)
- [快速开始](#快速开始)
- [证书生成](#证书生成)
- [证书验证](#证书验证)
- [API 接口](#api-接口)
- [配置说明](#配置说明)
- [安全注意事项](#安全注意事项)
- [常见问题](#常见问题)

## 概述

License（版权许可证）用于收费软件给付费用户提供的访问许可证明。当应用部署在客户的内网环境时，由于无法保证服务器可以访问外网，通常使用服务器许可文件的方式，在应用启动时加载证书并验证其有效性。

**核心特性：**

- 基于 TrueLicense 1.33 实现
- 支持硬件指纹验证（IP 地址、MAC 地址、CPU 序列号、主板序列号）
- 支持时间约束（生效时间、过期时间）
- 支持 Linux 和 Windows 操作系统
- 自动证书安装和卸载
- 提供 REST API 接口用于证书生成和服务器信息获取

**适用场景：**

- 内网部署的商业软件授权管理
- 需要硬件绑定的软件许可控制
- 基于时间的软件试用期管理

## 模块结构

```
carlos-integration/carlos-license/
├── carlos-license-core                              # 核心模块
│   ├── CustomLicenseManager.java                   # 自定义 License 管理器
│   ├── CustomKeyStoreParam.java                    # 自定义密钥存储参数
│   ├── LicenseCheckModel.java                      # 服务器硬件信息模型
│   └── service/
│       ├── AbstractSystemInfoDao.java              # 系统信息获取抽象类
│       ├── LinuxSystemInfoDao.java                 # Linux 系统信息获取
│       └── WindowsSystemInfoDao.java               # Windows 系统信息获取
├── carlos-spring-boot-starter-license-generate     # 证书生成模块（仅开发环境）
│   ├── LicenseCreatorService.java                  # 证书生成服务
│   ├── LicenseCreatorController.java               # 证书生成 API
│   └── LicenseGenerateProperties.java              # 生成模块配置
└── carlos-spring-boot-starter-license-verify       # 证书验证模块（生产环境）
    ├── LicenseVerify.java                          # 证书验证服务
    ├── LicenseVerifyConfig.java                    # 验证模块配置
    └── LicenseVerifyProperties.java                # 验证模块配置属性
```

**模块说明：**

- **carlos-license-core**: 核心功能模块，包含 TrueLicense 的自定义实现和硬件信息获取逻辑
- **carlos-spring-boot-starter-license-generate**: 证书生成模块，提供证书创建和服务器信息获取功能，**仅用于开发环境，不应包含在生产部署中**
- **carlos-spring-boot-starter-license-verify**: 证书验证模块，提供证书安装、验证和卸载功能，**应包含在生产部署中**

## 快速开始

### 1. 生成环境（证书生成服务）

用于生成和管理客户许可证，应部署在安全的内部环境。

**添加依赖：**

```xml
<dependency>
    <groupId>com.carlos.license</groupId>
    <artifactId>carlos-spring-boot-starter-license-generate</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

**配置文件（application.yml）：**

```yaml
carlos:
  license:
    generate:
      enabled: true
      file-path: /data/license  # 证书文件存储路径
```

### 2. 验证环境（客户部署）

用于验证许可证的有效性，应包含在交付给客户的应用中。

**添加依赖：**

```xml
<dependency>
    <groupId>com.carlos.license</groupId>
    <artifactId>carlos-spring-boot-starter-license-verify</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

**配置文件（application.yml）：**

```yaml
carlos:
  license:
    verify:
      enabled: true
      subject: your-app-name                          # 证书主题（应用名称）
      public-alias: publicCert                        # 公钥别名
      store-pass: pubwd123456                         # 公钥库密码
      license-path: /data/license/license.lic         # 证书文件路径
      public-keys-store-path: /data/license/publicCerts.keystore  # 公钥库路径
      validate-contents:                              # 验证内容（可选）
        - IP                                          # 验证 IP 地址
        - MAC                                         # 验证 MAC 地址
        - CPU                                         # 验证 CPU 序列号
        - MAIN_BOARD                                  # 验证主板序列号
```

## 证书生成

### 步骤 1: 生成公私钥证书库

使用 JDK 自带的 `keytool` 工具生成公私钥对。

```bash
# 1. 生成私钥库
# validity: 私钥的有效期（天）
# alias: 私钥别称
# keystore: 私钥库文件名称
# storepass: 私钥库密码（密钥库口令）
# keypass: 别名条目的密码（密钥口令）
keytool -genkeypair \
  -keysize 1024 \
  -validity 3650 \
  -alias "privateKey" \
  -keystore "privateKeys.keystore" \
  -storepass "pubwd123456" \
  -keypass "priwd123456" \
  -dname "CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN"

# 2. 导出公钥证书
keytool -exportcert \
  -alias "privateKey" \
  -keystore "privateKeys.keystore" \
  -storepass "pubwd123456" \
  -file "certfile.cer"

# 3. 导入公钥到公钥库
keytool -import \
  -alias "publicCert" \
  -file "certfile.cer" \
  -keystore "publicCerts.keystore" \
  -storepass "pubwd123456"

# 4. 删除临时证书文件
rm certfile.cer
```

**生成的文件：**

- `privateKeys.keystore`: 私钥库（用于证书生成，需妥善保管）
- `publicCerts.keystore`: 公钥库（用于证书验证，交付给客户）

### 步骤 2: 获取客户服务器硬件信息

调用 API 获取客户服务器的硬件指纹信息。

**请求示例：**

```bash
curl -X GET "http://localhost:8080/license/getServerInfos?osType=LINUX"
```

**响应示例：**

```json
{
  "ipAddress": [
    "10.42.1.0",
    "10.101.1.4",
    "10.42.1.1",
    "172.17.0.1"
  ],
  "macAddress": [
    "DE-9B-A2-3D-34-C0",
    "7C-C3-85-5F-A7-E1",
    "0A-58-0A-2A-01-01",
    "02-42-3A-E2-79-21"
  ],
  "cpuSerial": "54 06 05 00 FF FB EB BF",
  "mainBoardSerial": "2102311TUVCNJC000055"
}
```

### 步骤 3: 生成许可证文件

使用获取的硬件信息生成许可证文件。

**请求示例：**

```bash
curl -X POST "http://localhost:8080/license/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "your-app-name",
    "privateAlias": "privateKey",
    "publicAlias": "publicCert",
    "keyPass": "priwd123456",
    "storePass": "pubwd123456",
    "validity": "3650",
    "issuedTime": "2024-01-01 00:00:00",
    "expiryTime": "2025-12-31 23:59:59",
    "consumerType": "User",
    "consumerAmount": 1,
    "description": "License for Customer A",
    "identifier": {
      "appName": "YourApp",
      "orgUnit": "IT Department",
      "org": "Your Company",
      "city": "Shanghai",
      "province": "Shanghai",
      "country": "CN"
    },
    "licenseCheckModel": {
      "ipAddress": ["10.42.1.0", "10.101.1.4"],
      "macAddress": ["DE-9B-A2-3D-34-C0", "7C-C3-85-5F-A7-E1"],
      "cpuSerial": "54 06 05 00 FF FB EB BF",
      "mainBoardSerial": "2102311TUVCNJC000055"
    }
  }'
```

**响应示例：**

```json
"a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

### 步骤 4: 下载许可证文件

使用返回的 ID 下载生成的许可证压缩包。

```bash
curl -X GET "http://localhost:8080/license/download?id=a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -o license.zip
```

**压缩包内容：**

- `license.lic`: 许可证文件
- `publicCerts.keystore`: 公钥库文件

### 步骤 5: 部署到客户环境

1. 将 `license.lic` 和 `publicCerts.keystore` 复制到客户服务器
2. 配置应用的 `license-path` 和 `public-keys-store-path`
3. 启动应用，证书将自动安装和验证

## 证书验证

### 自动验证

证书验证模块会在应用启动时自动安装和验证证书。

**验证流程：**

1. 应用启动时，`LicenseVerify` Bean 初始化
2. 调用 `installLicense()` 方法安装证书
3. 验证证书的时间有效性
4. 验证服务器硬件信息（IP、MAC、CPU、主板序列号）
5. 验证通过则应用正常启动，否则抛出异常

**日志示例：**

```
INFO  c.c.l.v.LicenseVerify - 证书安装成功
INFO  c.c.l.v.LicenseVerify - 证书验证通过
```

### 手动验证

在代码中手动验证证书有效性。

```java
@Service
public class YourService {

    @Autowired
    private LicenseVerify licenseVerify;

    public void criticalOperation() {
        // 在关键操作前验证证书
        if (!licenseVerify.verify()) {
            throw new ServiceException("许可证无效，操作被拒绝");
        }

        // 执行业务逻辑
        // ...
    }
}
```

### 验证失败场景

证书验证会在以下情况失败：

1. **证书文件不存在**: 未找到 `license.lic` 文件
2. **公钥库不存在**: 未找到 `publicCerts.keystore` 文件
3. **证书已过期**: 当前时间超过 `expiryTime`
4. **证书未生效**: 当前时间早于 `issuedTime`
5. **硬件信息不匹配**: 服务器的 IP、MAC、CPU 或主板序列号与证书中的不一致
6. **证书被篡改**: 证书签名验证失败

## API 接口

### 1. 获取服务器硬件信息

**接口：** `GET /license/getServerInfos`

**参数：**

| 参数名    | 类型     | 必填 | 说明                            |
|--------|--------|----|-------------------------------|
| osType | String | 否  | 操作系统类型（LINUX/WINDOWS），不传则自动检测 |

**响应：**

```json
{
  "ipAddress": ["10.42.1.0", "10.101.1.4"],
  "macAddress": ["DE-9B-A2-3D-34-C0", "7C-C3-85-5F-A7-E1"],
  "cpuSerial": "54 06 05 00 FF FB EB BF",
  "mainBoardSerial": "2102311TUVCNJC000055"
}
```

### 2. 生成许可证

**接口：** `POST /license/generate`

**请求体：**

```json
{
  "subject": "your-app-name",
  "privateAlias": "privateKey",
  "publicAlias": "publicCert",
  "keyPass": "priwd123456",
  "storePass": "pubwd123456",
  "validity": "3650",
  "issuedTime": "2024-01-01 00:00:00",
  "expiryTime": "2025-12-31 23:59:59",
  "consumerType": "User",
  "consumerAmount": 1,
  "description": "License description",
  "identifier": {
    "appName": "YourApp",
    "orgUnit": "IT",
    "org": "Company",
    "city": "Shanghai",
    "province": "Shanghai",
    "country": "CN"
  },
  "licenseCheckModel": {
    "ipAddress": ["10.42.1.0"],
    "macAddress": ["DE-9B-A2-3D-34-C0"],
    "cpuSerial": "54 06 05 00 FF FB EB BF",
    "mainBoardSerial": "2102311TUVCNJC000055"
  }
}
```

**响应：** 返回文件 ID（用于下载）

```json
"a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

### 3. 下载许可证文件

**接口：** `GET /license/download`

**参数：**

| 参数名 | 类型     | 必填 | 说明            |
|-----|--------|----|---------------|
| id  | String | 是  | 生成证书时返回的文件 ID |

**响应：** ZIP 压缩文件（包含 `license.lic` 和 `publicCerts.keystore`）

## 配置说明

### 证书生成模块配置

```yaml
carlos:
  license:
    generate:
      enabled: true                    # 是否启用证书生成功能，默认 false
      file-path: /data/license         # 证书文件存储路径（必填）
```

### 证书验证模块配置

```yaml
carlos:
  license:
    verify:
      enabled: true                    # 是否启用证书验证，默认 true
      subject: your-app-name           # 证书主题（应用名称，必填）
      public-alias: publicCert         # 公钥别名（必填）
      store-pass: pubwd123456          # 公钥库密码（必填）
      license-path: /data/license/license.lic                    # 证书文件路径（必填）
      public-keys-store-path: /data/license/publicCerts.keystore # 公钥库路径（必填）
      validate-contents:               # 验证内容（可选，默认全部验证）
        - IP                           # 验证 IP 地址
        - MAC                          # 验证 MAC 地址
        - CPU                          # 验证 CPU 序列号
        - MAIN_BOARD                   # 验证主板序列号
```

**验证内容说明：**

- `IP`: 验证服务器 IP 地址是否在许可证允许的列表中
- `MAC`: 验证服务器 MAC 地址是否在许可证允许的列表中
- `CPU`: 验证服务器 CPU 序列号是否匹配
- `MAIN_BOARD`: 验证服务器主板序列号是否匹配

如果不配置 `validate-contents`，默认验证所有硬件信息。

## 安全注意事项

### 1. 模块隔离

**严格遵守以下原则：**

- ✅ **生产环境**: 只包含 `carlos-spring-boot-starter-license-verify` 模块
- ❌ **生产环境**: 绝不包含 `carlos-spring-boot-starter-license-generate` 模块
- ✅ **开发环境**: 可以包含证书生成模块用于测试

**原因：** 如果客户获得证书生成模块，可能自行签发证书，导致授权失效。

### 2. 私钥保护

- 私钥库文件（`privateKeys.keystore`）应妥善保管，不得泄露
- 建议使用硬件安全模块（HSM）或密钥管理服务（KMS）存储私钥
- 定期更换私钥和公钥对
- 限制证书生成服务的访问权限

### 3. 证书管理

- 为每个客户生成独立的许可证文件
- 记录每个许可证的生成时间、客户信息和硬件指纹
- 定期审计许可证使用情况
- 建立许可证撤销机制

### 4. 硬件指纹验证

- 根据实际需求选择验证内容（IP、MAC、CPU、主板）
- IP 地址可能变化，建议配置多个允许的 IP
- MAC 地址在虚拟化环境中可能不稳定
- CPU 和主板序列号相对稳定，推荐使用

### 5. 时间约束

- 合理设置证书的生效时间和过期时间
- 考虑时区差异，建议使用 UTC 时间
- 提前通知客户证书即将过期

### 6. 代码混淆

- 建议对生产环境的 JAR 包进行代码混淆
- 使用 ProGuard 或其他混淆工具保护验证逻辑
- 在关键业务逻辑中多处埋点验证证书有效性

## 常见问题

### Q1: 证书安装失败，提示找不到文件

**原因：** 配置的 `license-path` 或 `public-keys-store-path` 路径不正确。

**解决方案：**

1. 检查文件路径是否存在
2. 检查应用是否有读取权限
3. 使用绝对路径而非相对路径
4. Linux 环境注意路径大小写

### Q2: 证书验证失败，提示硬件信息不匹配

**原因：** 服务器的硬件信息与证书中的不一致。

**解决方案：**

1. 重新获取服务器硬件信息
2. 使用最新的硬件信息重新生成证书
3. 如果 IP 地址经常变化，配置多个允许的 IP
4. 考虑减少验证内容（如只验证 CPU 和主板）

### Q3: 虚拟机环境下 MAC 地址不稳定

**原因：** 虚拟机重启后 MAC 地址可能变化。

**解决方案：**

1. 配置虚拟机使用固定 MAC 地址
2. 或者不验证 MAC 地址，只验证 CPU 和主板序列号
3. 在配置中移除 `MAC` 验证项

### Q4: 如何支持多台服务器使用同一个证书

**方案 1：** 在证书中配置多台服务器的硬件信息（推荐）

```json
{
  "licenseCheckModel": {
    "ipAddress": ["192.168.1.10", "192.168.1.11", "192.168.1.12"],
    "macAddress": ["AA-BB-CC-DD-EE-01", "AA-BB-CC-DD-EE-02"],
    "cpuSerial": "CPU1|CPU2|CPU3",
    "mainBoardSerial": "MB1|MB2|MB3"
  }
}
```

**方案 2：** 为每台服务器生成独立证书

### Q5: 证书过期后如何续期

**解决方案：**

1. 使用相同的硬件信息生成新证书
2. 更新 `expiryTime` 为新的过期时间
3. 将新证书文件替换旧证书文件
4. 重启应用使新证书生效

### Q6: 如何在代码中多处验证证书

**建议：** 在关键业务逻辑中埋点验证

```java
@Aspect
@Component
public class LicenseAspect {

    @Autowired
    private LicenseVerify licenseVerify;

    @Around("@annotation(com.carlos.license.RequireLicense)")
    public Object checkLicense(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!licenseVerify.verify()) {
            throw new ServiceException("许可证无效");
        }
        return joinPoint.proceed();
    }
}
```

### Q7: 如何获取 Windows 服务器的硬件信息

**方法 1：** 使用提供的 API 接口

```bash
curl -X GET "http://localhost:8080/license/getServerInfos?osType=WINDOWS"
```

**方法 2：** 使用 Windows 命令

```cmd
# 获取 IP 地址
ipconfig

# 获取 MAC 地址
getmac

# 获取 CPU 序列号
wmic cpu get processorid

# 获取主板序列号
wmic baseboard get serialnumber
```

### Q8: 如何获取 Linux 服务器的硬件信息

**方法 1：** 使用提供的 API 接口

```bash
curl -X GET "http://localhost:8080/license/getServerInfos?osType=LINUX"
```

**方法 2：** 使用 Linux 命令

```bash
# 获取 IP 地址
ip addr show

# 获取 MAC 地址
ip link show

# 获取 CPU 序列号
dmidecode -t processor | grep ID

# 获取主板序列号
dmidecode -t baseboard | grep Serial
```

## 技术架构

### 核心依赖

- **TrueLicense 1.33**: 开源的 Java 许可证管理框架
- **Spring Boot 3.5.9**: 自动配置和依赖注入
- **Hutool**: 系统信息获取和工具类

### 工作原理

1. **证书生成**:
    - 使用 RSA 非对称加密算法生成公私钥对
    - 使用私钥对许可证内容进行签名
    - 将硬件信息和时间约束封装到许可证中

2. **证书验证**:
    - 使用公钥验证许可证签名
    - 检查证书的时间有效性
    - 获取当前服务器的硬件信息并与证书中的进行比对
    - 所有验证通过则返回 true

### 扩展性

如果需要自定义验证逻辑，可以继承 `CustomLicenseManager` 类并重写验证方法：

```java
public class MyLicenseManager extends CustomLicenseManager {

    public MyLicenseManager(LicenseParam param) {
        super(param);
    }

    @Override
    protected synchronized void validate(LicenseContent content)
            throws LicenseContentException {
        super.validate(content);

        // 添加自定义验证逻辑
        // ...
    }
}
```

## 参考资料

- [TrueLicense 官方文档](http://truelicense.java.net/)
- [JDK Keytool 文档](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html)
- [Carlos Framework 文档](../../../CLAUDE.md)

## 许可证

本组件遵循 Carlos Framework 的许可协议，仅供内部使用。

---

**维护者**: Carlos Team
**最后更新**: 2026-02-01
