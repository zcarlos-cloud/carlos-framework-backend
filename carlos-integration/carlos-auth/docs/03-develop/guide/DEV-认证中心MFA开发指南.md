# Carlos Auth MFA（多因素认证）详细文档

> **文档类型**: DEV
> **状态**: 已确认
> **版本**: v1.0.0
> **作者**: Carlos Team
> **日期**: 2026-05-20
> **标签**: #auth #DEV

---

## 目录

1. [什么是 MFA](#1-什么是-mfa)
2. [使用场景](#2-使用场景)
3. [需求分析](#3-需求分
4. [工作原理](#4-工作原理)
    - 4.1 [TOTP 算法核心流程](#41-totp-算法核心流程)
   - 4.2 [系统架构与交互流程](#42-系统架构与交互流
    - 4.3 [登录流程中的 MFA 集成](#43-登录流程中的-mfa-集成)
5. [代码结构](#5-代码结构)
6. [使用方式](#6-使用方式)
    - 6.1 [用户侧：绑定与验证](#61-用户侧绑定与验证)
   - 6.2 [开发侧：集成到登录流程](#62-开发侧集成到登录流
   - 6.3 [开发侧：实UserProvider](#63-开发侧实现-userprovider)
7. [数据库表结构](#7-数据库表结构)
8. [API 接口清单](#8-api-接口清单)
9. [错误码](#9-错误
10. [注意事项与最佳实践](#10-注意事项与最佳实

## 1. 什么是 MFA

**MFA（Multi-Factor Authentication，多因素认证* 是一种安全机制，要求用户在登录时提供两种或以上的验证因素

- **第一因素**：你知道的（用户+ 密码
- **第二因素**：你拥有的（手机上的动态验证码

`carlos-auth` 中，MFA 采用 **TOTP（Time-based One-Time Password，基于时间的一次性密码）** 实现，兼**Google
Authenticator、Microsoft Authenticator、Authy** 等主流验证器应用

## 2. 使用场景

| 场景            | 说明                                |
|---------------|-----------------------------------|
| **管理员账号保*     | 后台管理系统的超级管理员必须开MFA，防止密码泄露后被暴力破    |
| **敏感操作二次确认**  | 涉及资金转账、权限变更、数据导出等操作时，要求输入 MFA 验证  |
| **企业SSO**     | 作为统一认证中心，为内部系统提供可强制的二次验证能         |
| **等保 / 合规要求** | 满足等级保护 2.0、ISO 27001 等对身份鉴权的多因素要求 |

## 3. 需求分

`carlos-auth` MFA 模块覆盖了从绑定、验证、管理到容灾恢复的完整生命周期：

### 功能需

1. **MFA 设置**：为当前登录用户生成唯一TOTP 密钥和二维码（QR Code
2. **MFA 启用**：用户输入验证器上的 6 位验证码，验证通过后正式启MFA
3. **MFA 禁用**：用户可主动关闭 MFA（通常需要二次确认或管理员权限）
4. **MFA 重置**：重新生成密钥，旧的验证器失效，需重新绑定
5. **MFA 状态查*：前端判断是否需要展建议开MFA"的提
6. **登录集成**：用户名密码验证通过后，判断是否需MFA 二次验证
7. **备用恢复*：生10 组一次性恢复码，防止用户丢失手机后无法登录
8. **可信设备**（预留）：记住常用设备，在可信设备上可跳MFA 验证

### 非功能需

- **兼容*：兼RFC 6238 标准，支持主TOTP 应用
- **安全*：密钥随机生成、验证码 30 秒失效、 时间窗口容错
- **可扩展*：通过 `UserProvider` 接口与外部用户系统解

## 4. 工作原理

### 4.1 TOTP 算法核心流程

`carlos-auth` `TotpGenerator` 基于 **RFC 6238** 实现，核心参数如下：

| 参数   |            | 说明                   |
|------|------------|----------------------|
| 算法   | `HmacSHA1` | HMAC 哈希算法            |
| 时间步长 | `30 秒`     | 30 秒生成一个新的验证码        |
| 验证码位 | `6 位`      | 000000 ~ 999999      |
| 时间窗口 | `±1`       | 允许前后1 个时间步（共 90 秒）的容 |

**生成验证码的步骤**

```
1. 生成随机密钥60 / 20 字节
2. 计算当前时间步：counter = currentTimeMillis / (30 * 1000)
3. 用密钥和计数器生HMAC-SHA1 哈希
4. 动态截断（Dynamic Truncation）取 31 位整
5. 10^6 取模，得6 位数字，不足补零
```

**验证逻辑**

```java
// 校验当前时间步及前后1 个时间步
for (int i = -1; i <= 1; i++) {
    long targetTime = timestamp + (i * 30;
    if (code.equals(getTotpCode(secret, targetTime))) {
        return true;
    }
}
```

### 4.2 系统架构与交互流

```
┌─────────────────    ┌─────────────────    ┌─────────────────
  用户浏览    │────▶│  MfaController  │────▶│   MfaService    
 (扫码/输验证码) │◀──── (/auth/mfa/*)  │◀────(业务逻辑    
└─────────────────    └─────────────────    └────────┬────────
                                                         
                              ┌──────────────────────────┼──────────
                                                                
                                                                
                    ┌─────────────────      ┌───────────────── ┌─────────────────
                     TotpGenerator        QrCodeGenerator  │MfaRecoveryCode  
                     (TOTP 核心)           (QR URI)     │Service(恢复  
                    └─────────────────      └───────────────── └─────────────────
                                                                        
                                                                        
                    ┌─────────────────      ┌───────────────── ┌─────────────────
                     UserProvider         Google Chart API   MfaManager    
                    (用户数据接口)          (生成二维码图)   (数据持久化层)  
                    └─────────────────      └───────────────── └─────────────────
```

### 4.3 登录流程中的 MFA 集成

当前 `carlos-auth` 的登录流程中，MFA **"建议/可选增** 模式工作

```
用户提交用户名密
        
        
┌─────────────────
 认证管理器校 
(Authentication)
└────────┬────────
         
         
┌─────────────────────────
 查询用户 MFA 启用状   
 user.getMfaEnabled()   
└────────┬────────────────
         
    ┌────┴────
            
已启     未启
            
            
mfaRequired  mfaRecommended
=true        =true
            
            
返回 Token  返回 Token
(前端引导    (前端可提
二次验证)    用户开
```

`mfaRequired` 标志引导完成，属**"后置增强"** 模式。若业务需**"未通过 MFA 不颁Token"** 的强管控模式，需要自行扩展登录接口，
`buildLoginResponse()` 前增MFA 校验拦截

## 5. 代码结构

MFA 模块位于 `carlos-auth-service/src/main/java/com/carlos/auth/mfa/`，严格遵Carlos Framework 的分层架构：

```
mfa/
├── controller/
  └── MfaController.java              # Web 入口auth/mfa/*
├── service/
  ├── MfaService.java                 # 业务逻辑：设置、验证、启用、禁
  ├── MfaRecoveryCodeService.java     # 备用恢复码生成与验证
  └── TotpGenerator.java              # TOTP 算法实现（RFC 6238
├── manager/
  ├── MfaManager.java                 # 数据访问接口
  └── impl/
      └── MfaManagerImpl.java         # MyBatis-Plus 实现
├── mapper/
  ├── MfaRecoveryCodeMapper.java      # 恢复Mapper
  └── TrustedDeviceMapper.java        # 可信设备 Mapper
├── convert/
  └── MfaConvert.java                 # MapStruct 对象转换
└── pojo/
    ├── dto/
      └── MfaSetupDTO.java            # Service 层内部传输对
    ├── vo/
      ├── MfaSetupVO.java             # 设置信息响应
      └── MfaStatusVO.java            # 状态查询响
    ├── param/
      ├── MfaSetupParam.java          # 设置请求参数
      └── MfaVerifyParam.java         # 验证请求参数
    └── entity/
        ├── MfaRecoveryCode.java        # 恢复码实
        └── TrustedDevice.java          # 可信设备实体
```

## 6. 使用方式

### 6.1 用户侧：绑定与验

#### 步骤 1：获MFA 设置信息

```http
POST /auth/mfa/setup
Authorization: Bearer {access_token}
```

**响应示例**

```json
{
  "success": true,
  "code": "00000",
  "msg": "MFA设置信息生成成功",
  "data": {
    "secret": "dGVzdC1zZWNyZXQtMTIzNA==",
    "qrCodeUrl": "https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl=otpauth%3A%2F%2Ftotp%2FCarlosAuth...",
    "formattedSecret": "dGVz dC1z ZWNy ZXQt MTIz NA==",
    "recoveryCodes": ["A1B2C3D4", "E5F6G7H8", "...0]
  }
}
```

> 用户使用手机上的 **Google Authenticator** 扫描二维码，或手动输`formattedSecret`

#### 步骤 2：验证并启用 MFA

```http
POST /auth/mfa/verify
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "secret": "dGVzdC1zZWNyZXQtMTIzNA==",
  "code": "123456"
}
```

#### 步骤 3：登录时二次验证（由前端引导

当前版本中，登录接口返回`LoginResponse` 包含两个关键字段

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "mfaRequired": true,
  "mfaRecommended": false
}
```

- **`mfaRequired = true`**：用户已开MFA，前端应引导用户输入 6 位验证码，调`/auth/mfa/verify` 或专门的 MFA 校验接口完成二次确认
- **`mfaRecommended = true`**：用户未开MFA，前端可弹出提示建议用户开启

### 6.2 开发侧：集成到登录流程

如果你需要让 MFA 在颁Token 前进*强制拦截**，可修改 `UserLoginService.login()`

```java
public LoginResponse login(LoginRequest loginRequest) {
    // 1. 执行用户名密码认
    Authentication authentication = authenticationManager.authenticate(...);
    UserInfo user = userProvider.loadUserByIdentifier(loginRequest.getUsername());

    // 2. 检MFA（强制模式示例）
    if (Boolean.TRUE.equals(user.getMfaEnabled())) {
        // 若请求中未携MFA 验证码，或验证失败，直接抛异
        if (!StringUtils.hasText(loginRequest.getMfaCode()) 
            || !mfaService.verifyMfaCode(user.getUsername(), loginRequest.getMfaCode())) {
            throw AuthErrorCode.AUTH_MFA_CODE_ERROR.exception();
        }
    }

    // 3. 颁发 Token
    return buildLoginResponse(user, authentication, clientId);
}
```

### 6.3 开发侧：实UserProvider

MFA 的状态最终需要持久化到用户表中。`UserProvider` 接口要求实现方提供以下方法：

```java
public interface UserProvider {
    /**
     * 更新 MFA 启用状
     */
    boolean updateMfaStatus(Long userId, boolean mfaEnabled, String mfaSecret);

    /**
     * 根据标识查询用户（需包含 mfaEnabled、mfaSecret 字段
     */
    UserInfo loadUserByIdentifier(String identifier);
}
```

**用户表示例字*

```sql
ALTER TABLE sys_user ADD COLUMN mfa_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用MFA';
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(255) DEFAULT NULL COMMENT 'MFA密钥(Base64)';
```

## 7. 数据库表结构

### 7.1 备用恢复码表

```sql
CREATE TABLE auth_mfa_recovery_code (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '恢复码ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    code        VARCHAR(16)  NOT NULL COMMENT '恢复,
    used        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已使,
    used_time   DATETIME              DEFAULT NULL COMMENT '使用时间',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT 'MFA备用恢复;
```

### 7.2 可信设备

```sql
CREATE TABLE auth_trusted_device (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设备ID',
    user_id            BIGINT       NOT NULL COMMENT '用户ID',
    device_fingerprint VARCHAR(64)  NOT NULL COMMENT '设备指纹(MD5)',
    device_name        VARCHAR(128)          DEFAULT NULL COMMENT '设备名称',
    ip_address         VARCHAR(64)           DEFAULT NULL COMMENT 'IP地址',
    location           VARCHAR(128)          DEFAULT NULL COMMENT '地理位置',
    user_agent         VARCHAR(512)          DEFAULT NULL COMMENT 'User-Agent',
    browser            VARCHAR(64)           DEFAULT NULL COMMENT '浏览器名,
    is_mobile          TINYINT(1)            DEFAULT 0 COMMENT '是否为移动设,
    trusted_time       DATETIME              DEFAULT NULL COMMENT '添加为可信时,
    last_used_time     DATETIME              DEFAULT NULL COMMENT '最后使用时,
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) COMMENT 'MFA可信设备';
```

## 8. API 接口清单

| 接口     | 方法   | 路径                  | 权限 | 说明                 |
|--------|------|---------------------|----|--------------------|
| 获取设置信息 | POST | `/auth/mfa/setup`   | 已登 | 生成密钥、QR码、恢复码       |
| 验证并启   | POST | `/auth/mfa/verify`  | 已登 | 验证 TOTP 验证码，启用 MFA |
| 禁用 MFA | POST | `/auth/mfa/disable` | 已登 | 关闭 MFA，清除密钥和恢复     |
| 重置密钥   | POST | `/auth/mfa/reset`   | 已登 | 重新生成密钥，需重新绑定       |
| 查询状    | GET  | `/auth/mfa/status`  | 已登 | 返回 MFA 是否启用        |

## 9. 错误

MFA 相关错误码定义在 `AuthErrorCode` 中：

| 错误      | 消息         | HTTP 状 | 触发场景            |
|---------|------------|--------|-----------------|
| `20311` | 多因素认证未启用   | 400    | 用户尝试验证 MFA 但未先开 |
| `20312` | 多因素认证验证码错误 | 400    | TOTP 验证码输入错     |
| `20313` | 多因素认证验证码已过 | 400    | 验证码超出时间窗口（90秒）  |
| `20314` | 多因素认证设置失   | 500    | 系统内部生成密钥/保存失败   |

## 10. 注意事项与最佳实

1. **必须实现 `UserProvider.updateMfaStatus()`**
    - 当前 `MfaService.enableMfa()` / `disableMfa()` 中更新用MFA 状态的代码被注释掉了，因为框架层面`UserInfo`
      DTO，`UserProvider` 是接口。接入方必须在自己的用户服务中实现该方法，将 `mfa_enabled` `mfa_secret` 持久化到数据库

2. **生产环境建议使用 Base32 编码**
    - 当前 `TotpGenerator.generateSecret()` 使用 `Base64` 编码，虽Google Authenticator 也支持，**RFC 6238 推荐
      Base32**。如果追求最大兼容性，建议`generateSecret()` `getTotpCode()` 改为 Base32 编解码（如使Apache Commons
      Codec `Base32`）

3. **恢复码必须仅展示一*
    - `/auth/mfa/setup` 返回`recoveryCodes` 应在用户完成 MFA 验证后由后端保存`auth_mfa_recovery_code`
      表。前端应在用户绑定成功后立即展示恢复码，并提请截图保存，关闭后无法再次查

4. **可信设备机制待完*
    - `TrustedDevice` 表和 Manager 层已预留，但 Controller Service 层面记住此设30 功能尚未实现。若需要，可扩
      `MfaController` 增加可信设备管理接口

5. **登录模式选择**
    - **当前默认**后置增强"模式（先Token，再引导 MFA）
    - **可选改*前置拦截"模式（MFA 不通过则不Token）。根据业务安全等级选择

6. **QR 码生成依赖外部服*
    - 当前使用 `https://chart.googleapis.com/chart` 生成二维码图片。若部署环境无法访问外网，建议引入本QR 码生成库（如
      `zxing`），替换 `QrCodeGenerator.generateQrCodeUrl()` 的实现

## 附录：快速验证脚

你可以用以下方式快速测TOTP 生成和验证：

```java
@Autowired
private TotpGenerator totpGenerator;

@Test
public void testTotp() {
    String secret = totpGenerator.generateSecret();
    String code = totpGenerator.getTotpCode(secret);
    boolean valid = totpGenerator.verifyTotpCode(secret, code);
    System.out.println("Secret: " + secret);
    System.out.println("Code: " + code);
    System.out.println("Valid: " + valid); // true
}
```
