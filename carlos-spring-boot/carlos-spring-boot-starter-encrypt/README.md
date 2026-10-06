# carlos-spring-boot-starter-encrypt

## 模块简介

`carlos-spring-boot-starter-encrypt` 是 Carlos
框架的加密工具模块，提供了全面的加解密功能支持。该模块支持中国国密算法（SM2/SM3/SM4）和国际标准算法（AES/RSA/DES/3DES/MD5/SHA/Base64），基于
BouncyCastle 1.70 和 Hutool 5.8.40 实现。

## 主要功能

### 支持的算法

| 算法类型        | 算法名称          | 说明                     |
|-------------|---------------|------------------------|
| **国密对称加密**  | SM4           | 中国国家密码管理局发布的对称加密算法     |
| **国密非对称加密** | SM2           | 中国国家密码管理局发布的椭圆曲线公钥密码算法 |
| **国密哈希**    | SM3           | 中国国家密码管理局发布的密码学哈希函数    |
| **国际对称加密**  | AES           | 高级加密标准，广泛使用            |
| **国际非对称加密** | RSA           | 非对称加密算法，支持数字签名         |
| **传统对称加密**  | DES           | 数据加密标准（已不建议使用）         |
| **传统对称加密**  | 3DES          | 三重 DES（DES 的改进版）       |
| **哈希算法**    | MD5           | 消息摘要算法（已不建议用于安全场景）     |
| **哈希算法**    | SHA-1/256/512 | 安全哈希算法系列               |
| **编码**      | Base64        | Base64 编码（非加密）         |

---

## 快速开始

### 1. SM4 对称加密（国密）

**SM4** 是中国国家密码管理局发布的对称加密算法，密钥长度为 128 位。

#### 基础用法

```java
// 使用配置的默认模式加密（BASE64 或 HEX）
String encrypted = EncryptUtil.encrypt("敏感数据");

// 解密
String decrypted = EncryptUtil.decrypt(encrypted);
```

#### 指定输出格式

```java
// HEX 格式加密
String hexEncrypted = EncryptUtil.sm4EncryptHex("敏感数据");
// 结果: 3a5f8c2d...（十六进制字符串）

// BASE64 格式加密
String base64Encrypted = EncryptUtil.sm4EncryptBase64("敏感数据");
// 结果: Ol+MLd...（Base64 字符串）

// 解密（自动识别格式）
String decrypted = EncryptUtil.decrypt(hexEncrypted);
```

#### 配置示例

```yaml
carlos:
  encrypt:
    sm4:
      enabled: true
      encrypt-mode: cbc
      key: "your-secret-key-16bytes"
      iv: "your-iv-16bytes"
      store-type: base64
```

---

### 2. SM2 非对称加密（国密）

**SM2** 是中国国家密码管理局发布的椭圆曲线公钥密码算法，基于 256 位椭圆曲线。

#### 生成密钥对

```java
// 生成 SM2 密钥对
Sm2KeyPair keyPair = EncryptUtil.generateSm2KeyPair();

// 公钥（130 位十六进制字符串，以 "04" 开头）
String publicKey = keyPair.getPublicKey();
// 示例: 0403c7a7250306531238d49c2e0146e48217f2d65b28ed1d61565bd40472c7516bc78574de0a89f9067328558d656ecdc2504f49bb2bd843a9cfa9b404463ed453

// 私钥（64 位十六进制字符串）
String privateKey = keyPair.getPrivateKey();
// 示例: 3134665e1e009bab370738fd5ff1dabc7f390b683885c321f1523f4f725e8aa1
```

#### 加密解密

```java
// 使用公钥加密
String encrypted = EncryptUtil.sm2Encrypt("敏感数据");

// 使用私钥解密
String decrypted = EncryptUtil.sm2Decrypt(encrypted);
```

#### 指定输出格式

```java
// HEX 格式加密
String hexEncrypted = EncryptUtil.sm2EncryptHex("敏感数据");

// BASE64 格式加密
String base64Encrypted = EncryptUtil.sm2EncryptBase64("敏感数据");

// 解密（自动识别格式）
String decrypted = EncryptUtil.sm2Decrypt(hexEncrypted);
```

---

### 3. SM3 哈希（国密）

**SM3** 是中国国家密码管理局发布的密码学哈希函数，产生 256 位哈希值。

```java
// SM3 哈希
String hash = EncryptUtil.sm3("待哈希数据");
// 结果: 66c7f0f462eeedd9d1f2d46bdcb6b9e5e0a5d6e0e2e6e8e...

// 加盐 SM3
String saltedHash = EncryptUtil.sm3WithSalt("待哈希数据", "salt123");

// 验证哈希
boolean valid = EncryptUtil.sm3Verify("待哈希数据", hash);
```

---

### 4. AES 对称加密

**AES** (Advanced Encryption Standard) 是国际标准对称加密算法，支持 128/192/256 位密钥。

```java
import com.carlos.encrypt.utils.AesUtil;
import com.carlos.encrypt.enums.AesKeySize;

// 生成 AES 密钥
String key = EncryptUtil.generateAesKey(AesKeySize.BITS_256);
// 或使用 128/192 位
// String key = EncryptUtil.generateAesKey(AesKeySize.BITS_128);

// AES 加密（Hex 格式）
String encrypted = EncryptUtil.aesEncrypt("敏感数据", key);

// AES 加密（Base64 格式）
String encryptedBase64 = EncryptUtil.aesEncryptBase64("敏感数据", key);

// AES 解密
String decrypted = EncryptUtil.aesDecrypt(encrypted, key);
```

#### 高级用法

```java
// 使用 AesUtil 直接调用（更多参数控制）
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;

// 指定模式和填充方式
String encrypted = AesUtil.encrypt(data, key, iv, Mode.CBC, Padding.PKCS5Padding);

// 生成 IV
String iv = AesUtil.generateIv();
```

---

### 5. RSA 非对称加密

**RSA** 是国际标准非对称加密算法，支持数字签名。

```java
import com.carlos.encrypt.utils.RsaUtil;
import com.carlos.encrypt.enums.RsaKeySize;
import com.carlos.encrypt.key.RsaKeyPair;

// 生成 RSA 密钥对
RsaKeyPair keyPair = EncryptUtil.generateRsaKeyPair(RsaKeySize.BITS_2048);
String publicKey = keyPair.getPublicKey();
String privateKey = keyPair.getPrivateKey();

// RSA 加密
String encrypted = EncryptUtil.rsaEncrypt("敏感数据", publicKey);

// RSA 解密
String decrypted = EncryptUtil.rsaDecrypt(encrypted, privateKey);

// RSA 数字签名
String sign = EncryptUtil.rsaSign("待签名数据", privateKey);

// RSA 验签
boolean valid = EncryptUtil.rsaVerify("待签名数据", sign, publicKey);
```

#### 高级用法

```java
// 使用 RsaUtil 直接调用
// 签名（Base64 格式）
String signBase64 = RsaUtil.signBase64(data, privateKey);

// 验签（Base64 格式签名）
boolean valid = RsaUtil.verifyBase64(data, signBase64, publicKey);

// 加密（Base64 格式）
String encryptedBase64 = RsaUtil.encryptBase64(data, publicKey);
```

---

### 6. 3DES 对称加密

**3DES** (Triple DES) 是 DES 的改进版本，使用三个密钥进行三次加密。

```java
import com.carlos.encrypt.utils.Des3Util;

// 生成 3DES 密钥
String key = Des3Util.generateKey();

// 3DES 加密
String encrypted = EncryptUtil.des3Encrypt("敏感数据", key);

// 3DES 解密
String decrypted = EncryptUtil.des3Decrypt(encrypted, key);
```

---

### 7. MD5 哈希

**MD5** 是一种广泛使用的哈希算法，但已被证明不安全，不建议用于安全敏感场景。

```java
import com.carlos.encrypt.utils.Md5Util;

// MD5 哈希（32 位）
String hash = EncryptUtil.md5("待哈希数据");

// MD5 哈希（16 位）
String hash16 = EncryptUtil.md5_16("待哈希数据");

// 加盐 MD5
String saltedHash = EncryptUtil.md5WithSalt("待哈希数据", "salt123");

// 验证 MD5
boolean valid = EncryptUtil.md5Verify("待哈希数据", hash);

// 文件 MD5
String fileMd5 = Md5Util.md5(new File("/path/to/file"));

// HMAC-MD5
String hmac = Md5Util.hmacMd5("待哈希数据", "secretKey");
```

---

### 8. SHA 哈希

**SHA** (Secure Hash Algorithm) 是安全哈希算法系列。

```java
import com.carlos.encrypt.utils.ShaUtil;

// SHA-1
String sha1 = EncryptUtil.sha1("待哈希数据");

// SHA-256（推荐）
String sha256 = EncryptUtil.sha256("待哈希数据");

// SHA-512
String sha512 = EncryptUtil.sha512("待哈希数据");

// 加盐 SHA-256
String saltedSha256 = EncryptUtil.sha256WithSalt("待哈希数据", "salt123");

// 验证 SHA-256
boolean valid = EncryptUtil.sha256Verify("待哈希数据", sha256);

// 文件 SHA-256
String fileSha256 = ShaUtil.sha256(new File("/path/to/file"));

// HMAC-SHA256
String hmac = ShaUtil.hmacSha256("待哈希数据", "secretKey");
```

---

### 9. Base64 编码

**Base64** 是一种编码方式，不是加密算法。

```java
import com.carlos.encrypt.utils.Base64Util;

// Base64 编码
String encoded = EncryptUtil.base64Encode("待编码数据");

// Base64 解码
String decoded = EncryptUtil.base64Decode(encoded);

// 字节数组编码
byte[] data = "待编码数据".getBytes();
String encoded = EncryptUtil.base64Encode(data);

// 解码为字节数组
byte[] decoded = EncryptUtil.base64DecodeToBytes(encoded);

// URL 安全编码
String urlSafe = Base64Util.encodeUrlSafe("待编码数据");

// 文件编码
String fileBase64 = Base64Util.encodeFile(new File("/path/to/file"));
```

---

## 实际应用场景

### 场景 1：敏感数据存储

```java
@Service
public class UserService {

    public void saveUser(User user) {
        // 加密身份证号（使用 SM4）
        String encryptedIdCard = EncryptUtil.encrypt(user.getIdCard());
        user.setIdCard(encryptedIdCard);

        // 加密手机号
        String encryptedPhone = EncryptUtil.encrypt(user.getPhone());
        user.setPhone(encryptedPhone);

        userMapper.insert(user);
    }

    public User getUser(Long id) {
        User user = userMapper.selectById(id);

        // 解密身份证号
        String idCard = EncryptUtil.decrypt(user.getIdCard());
        user.setIdCard(idCard);

        // 解密手机号
        String phone = EncryptUtil.decrypt(user.getPhone());
        user.setPhone(phone);

        return user;
    }
}
```

### 场景 2：密码存储（使用 SM3）

```java
@Service
public class AuthService {

    @Value("${user.password.salt}")
    private String passwordSalt;

    public void register(String username, String password) {
        // 使用 SM3 加盐哈希存储密码
        String hashedPassword = EncryptUtil.sm3WithSalt(password, passwordSalt);
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        userMapper.insert(user);
    }

    public boolean login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 验证密码
        String hashedPassword = EncryptUtil.sm3WithSalt(password, passwordSalt);
        return hashedPassword.equals(user.getPassword());
    }
}
```

### 场景 3：API 数据加密传输（使用 RSA）

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    // 接收加密数据
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody EncryptedRequest request) {
        // 解密请求数据
        String decryptedData = EncryptUtil.rsaDecrypt(request.getData(), privateKey);

        // 处理业务逻辑
        processData(decryptedData);

        return Result.success();
    }

    // 返回加密数据
    @GetMapping("/data")
    public Result<EncryptedResponse> getData() {
        String sensitiveData = getSensitiveData();

        // 加密响应数据
        String encryptedData = EncryptUtil.rsaEncrypt(sensitiveData, clientPublicKey);

        EncryptedResponse response = new EncryptedResponse();
        response.setData(encryptedData);

        return Result.success(response);
    }
}
```

### 场景 4：配置文件敏感信息加密（使用 AES）

```java
@Component
public class ConfigLoader {

    @Value("${database.password}")
    private String encryptedPassword;
    
    @Value("${encrypt.aes.key}")
    private String aesKey;

    @PostConstruct
    public void init() {
        // 解密数据库密码
        String password = EncryptUtil.aesDecrypt(encryptedPassword, aesKey);

        // 使用解密后的密码
        dataSource.setPassword(password);
    }
}
```

### 场景 5：日志脱敏

```java
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(com.carlos.core.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        // 加密敏感参数
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SensitiveData) {
                SensitiveData data = (SensitiveData) args[i];
                data.setIdCard(EncryptUtil.encrypt(data.getIdCard()));
                data.setPhone(EncryptUtil.encrypt(data.getPhone()));
            }
        }

        // 记录日志
        log.info("Method: {}, Args: {}", joinPoint.getSignature(), args);

        return joinPoint.proceed();
    }
}
```

### 场景 6：MyBatis 字段加密

**自定义 TypeHandler**：

```java
@MappedTypes(String.class)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 加密后存储
        String encrypted = EncryptUtil.encrypt(parameter);
        ps.setString(i, encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        // 解密后返回
        return EncryptUtil.decrypt(encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return EncryptUtil.decrypt(encrypted);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return EncryptUtil.decrypt(encrypted);
    }
}
```

**使用 TypeHandler**：

```java
@Data
@TableName("sys_user")
public class User {
    @TableId
    private Long id;

    private String name;

    // 自动加密解密
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String idCard;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String phone;
}
```

---

## 配置说明

### 完整配置示例

```yaml
carlos:
  encrypt:
    # SM4 国密对称加密配置
    sm4:
      enabled: true                      # 启用 SM4 加密
      encrypt-mode: cbc                  # 加密模式：cbc 或 ecb
      key: "asdfghjklmnbvcx"            # 密钥（16 字节，不足会自动 MD5 处理）
      iv: "mnbvcxzpoiuytre1"            # 初始化向量（CBC 模式必需，16 字节）
      store-type: base64                 # 存储格式：base64 或 hex

    # SM2 国密非对称加密配置
    sm2:
      enabled: true                      # 启用 SM2 加密
      public-key: "0403c7a7250306531238d49c2e0146e48217f2d65b28ed1d61565bd40472c7516bc78574de0a89f9067328558d656ecdc2504f49bb2bd843a9cfa9b404463ed453"
      private-key: "3134665e1e009bab370738fd5ff1dabc7f390b683885c321f1523f4f725e8aa1"
      store-type: base64                 # 存储格式：base64 或 hex

    # AES 对称加密配置
    aes:
      enabled: false                     # 启用 AES 加密
      key-size: 256                      # 密钥长度：128/192/256
      encrypt-mode: cbc                  # 加密模式
      key:                               # 密钥
      iv:                                # 初始化向量

    # RSA 非对称加密配置
    rsa:
      enabled: false                     # 启用 RSA 加密
      key-size: 2048                     # 密钥长度
      public-key:                        # 公钥
      private-key:                       # 私钥

    # 哈希算法配置
    hash:
      default-algorithm: sm3             # 默认哈希算法
      salt-enabled: true                 # 是否启用加盐
      salt:                              # 全局盐值
```

### 配置项说明

#### SM4 配置

| 配置项            | 类型      | 默认值    | 说明                   |
|----------------|---------|--------|----------------------|
| `enabled`      | Boolean | true   | 是否启用 SM4 加密          |
| `encrypt-mode` | String  | cbc    | 加密模式（cbc/ecb）        |
| `key`          | String  | -      | 密钥（16 字节，不足会 MD5 处理） |
| `iv`           | String  | -      | 初始化向量（CBC 模式必需）      |
| `store-type`   | String  | base64 | 存储格式（base64/hex）     |

#### SM2 配置

| 配置项           | 类型      | 默认值    | 说明               |
|---------------|---------|--------|------------------|
| `enabled`     | Boolean | false  | 是否启用 SM2 加密      |
| `public-key`  | String  | -      | 公钥（130 位十六进制）    |
| `private-key` | String  | -      | 私钥（64 位十六进制）     |
| `store-type`  | String  | base64 | 存储格式（base64/hex） |

#### AES 配置

| 配置项            | 类型      | 默认值   | 说明                |
|----------------|---------|-------|-------------------|
| `enabled`      | Boolean | false | 是否启用 AES 加密       |
| `key-size`     | Integer | 256   | 密钥长度（128/192/256） |
| `encrypt-mode` | String  | cbc   | 加密模式              |
| `key`          | String  | -     | 密钥                |
| `iv`           | String  | -     | 初始化向量             |

#### RSA 配置

| 配置项           | 类型      | 默认值   | 说明                   |
|---------------|---------|-------|----------------------|
| `enabled`     | Boolean | false | 是否启用 RSA 加密          |
| `key-size`    | Integer | 2048  | 密钥长度（1024/2048/4096） |
| `public-key`  | String  | -     | 公钥（Base64 格式）        |
| `private-key` | String  | -     | 私钥（Base64 格式）        |

### 加密模式对比

| 特性    | CBC 模式 | ECB 模式 |
|-------|--------|--------|
| 安全性   | 高      | 低      |
| 需要 IV | 是      | 否      |
| 并行加密  | 否      | 是      |
| 并行解密  | 是      | 是      |
| 推荐使用  | ✓      | ✗      |

**推荐使用 CBC 模式**，因为 ECB 模式对相同的明文块会产生相同的密文块，存在安全隐患。

### 存储格式对比

| 格式     | 长度 | 可读性 | 存储空间 |
|--------|----|-----|------|
| HEX    | 较长 | 较好  | 较大   |
| BASE64 | 较短 | 一般  | 较小   |

**推荐使用 BASE64 格式**，存储空间更小。

---

## 依赖引入

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-encrypt</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

## 依赖项

- **BouncyCastle**: 1.70（密码学提供者）
- **Hutool**: 5.8.40（加密工具封装）
- **carlos-spring-boot-core**: 核心基础模块
- **Spring Boot**: 3.5.8+（自动配置）

---

## 算法特性

### 国密算法

#### SM4 算法

- **标准**: GB/T 32907-2016
- **密钥长度**: 128 位（16 字节）
- **分组长度**: 128 位（16 字节）
- **轮数**: 32 轮
- **模式**: CBC、ECB、CTR、OFB、CFB
- **填充**: PKCS5Padding、PKCS7Padding
- **性能**: 优于 AES-128

#### SM2 算法

- **标准**: GB/T 32918-2016
- **曲线**: SM2P256V1（256 位椭圆曲线）
- **密钥长度**: 256 位
- **公钥长度**: 130 字符（未压缩点编码）
- **私钥长度**: 64 字符（32 字节）
- **安全性**: 等同于 RSA-3072
- **性能**: 优于 RSA

#### SM3 算法

- **标准**: GB/T 32905-2016
- **输出长度**: 256 位（32 字节）
- **结构**: Merkle-Damgård
- **安全性**: 等同于 SHA-256

### 国际算法

#### AES 算法

- **标准**: FIPS-197
- **密钥长度**: 128/192/256 位
- **分组长度**: 128 位
- **安全性**: 高，广泛使用

#### RSA 算法

- **密钥长度**: 1024/2048/3072/4096 位
- **推荐长度**: 2048 位或更高
- **用途**: 加密、数字签名
- **性能**: 较慢，适合小数据量

---

## 算法选择建议

| 场景       | 推荐算法                | 说明       |
|----------|---------------------|----------|
| 大量数据加密   | SM4 / AES           | 对称加密，性能高 |
| 密钥交换     | SM2 / RSA           | 非对称加密，安全 |
| 数字签名     | SM2 / RSA           | 支持签名和验签  |
| 密码存储     | SM3 / SHA-256       | 单向哈希，加盐  |
| 数据完整性    | SM3 / SHA-256       | 哈希校验     |
| 合规要求（中国） | SM2/SM3/SM4         | 国密算法     |
| 国际兼容     | AES / RSA / SHA-256 | 国际标准     |

---

## 注意事项

1. **密钥管理**: 密钥应妥善保管，不要硬编码在代码中
2. **密钥长度**:
    - SM4 密钥必须为 16 字节
    - AES 密钥建议 256 位
    - RSA 密钥建议 2048 位或更高
3. **IV 向量**: CBC 模式必须配置 IV，且 IV 应随机生成
4. **SM2 公私钥**: 公钥以 "04" 开头，长度 130 字符；私钥长度 64 字符
5. **RSA 密钥**: 公钥和私钥均为 Base64 格式
6. **加密模式**: 推荐使用 CBC 或 GCM 模式，ECB 模式不安全
7. **存储格式**: 推荐使用 BASE64 格式，节省存储空间
8. **性能考虑**:
    - 对称加密（SM4/AES）适合大量数据
    - 非对称加密（SM2/RSA）适合小数据量和密钥交换
9. **哈希算法**:
    - MD5 和 SHA-1 已不安全，不建议用于安全场景
    - 推荐使用 SM3 或 SHA-256
10. **异常处理**: 加密失败会抛出 `EncryptException`，需要捕获处理
11. **配置验证**: 启动时会验证密钥配置，缺失会抛出异常
12. **国密合规**: 符合中国国家密码管理局要求

---

## 性能优化

1. **批量加密**: 使用 SM4 或 AES 对称加密，性能更好
2. **密钥缓存**: 密钥在启动时加载，避免重复解析
3. **线程安全**: 工具类方法是线程安全的
4. **对象复用**: BouncyCastle 对象会被复用，减少创建开销
5. **混合加密**: 使用非对称加密传输对称密钥，对称加密传输数据

---

## 安全建议

1. **密钥轮换**: 定期更换密钥，降低泄露风险
2. **密钥分离**: 不同环境使用不同密钥
3. **密钥加密**: 配置文件中的密钥应加密存储
4. **传输安全**: 使用 HTTPS 传输加密数据
5. **日志脱敏**: 日志中不要输出明文敏感数据
6. **权限控制**: 限制密钥文件的访问权限
7. **审计日志**: 记录加密解密操作的审计日志
8. **盐值随机**: 哈希加盐时，盐值应随机生成并存储

---

## 常见问题

### 1. 密钥长度不正确

**问题**：

```
java.security.InvalidKeyException: Wrong key size
```

**解决**：

- SM4 密钥必须为 16 字节
- AES 密钥必须为 16/24/32 字节
- 配置的密钥会自动处理为合适长度

### 2. 解密失败

**问题**：

```
EncryptException: Decryption failed
```

**原因**：

- 密钥不匹配
- 加密模式不匹配
- 数据已损坏

**解决**：

- 检查密钥配置
- 确认加密解密使用相同模式
- 验证数据完整性

### 3. SM2 公钥格式错误

**问题**：

```
IllegalArgumentException: Invalid public key format
```

**解决**：

- 公钥必须以 "04" 开头
- 公钥长度必须为 130 字符
- 使用 `generateSm2KeyPair()` 生成标准密钥对

### 4. RSA 密钥格式错误

**问题**：

```
IllegalArgumentException: RSA key format not supported
```

**解决**：

- 确保使用 Base64 格式的密钥
- 使用 `generateRsaKeyPair()` 生成标准密钥对

### 5. 性能问题

**问题**：非对称加密大数据量时性能差

**解决**：

- 使用对称加密（SM4/AES）加密大数据
- 使用非对称加密传输对称密钥
- 使用流式处理大文件

---

## 版本要求

- JDK 17+
- Spring Boot 3.5.8+
- BouncyCastle 1.70+
- Hutool 5.8.40+
- Maven 3.8+

---

## 相关模块

- `carlos-spring-boot-core`: 核心基础模块
- `carlos-tools`: 加密工具 GUI
- `carlos-test`: 加密功能测试

---

## 更新日志

### v3.0.0

- ✨ 新增 AES 对称加密支持
- ✨ 新增 RSA 非对称加密支持
- ✨ 新增 DES/3DES 对称加密支持
- ✨ 新增 MD5 哈希支持
- ✨ 新增 SHA-1/256/512 哈希支持
- ✨ 新增 SM3 国密哈希支持
- ✨ 新增 Base64 编码支持
- ✨ 统一 EncryptUtil 入口
- ✨ 完善配置支持
- 📝 完善文档说明
