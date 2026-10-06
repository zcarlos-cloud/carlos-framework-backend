# carlos-utils

## 模块简介

`carlos-utils` 是 Carlos 框架的基础工具模块，提供了框架所需的各种通用工具类和辅助函数。该模块包含 19
个工具类，涵盖树形结构、数据脱敏、文件操作、地理计算、邮件发送等多个领域。

## 主要功能

### 1. 树形结构工具 (TreeUtils)

将扁平列表数据转换为树形结构：

```java
// 基础用法（默认字段名：id, parentId, children）
List<Menu> menuList = menuService.list();
List<Menu> menuTree = TreeUtils.buildTree(menuList);

// 自定义配置
List<Dept> deptTree = TreeUtils.buildTree(
    deptList,
    true,           // 移除孤立节点
    true            // 移除空子节点集合
);

// 完全自定义
List<Node> tree = TreeUtils.buildTree(
    nodeList,
    "nodeId",       // ID 字段名
    "parentNodeId", // 父 ID 字段名
    "subNodes",     // 子节点字段名
    0L,             // 顶级节点 ID
    true,           // 移除孤立节点
    false           // 保留空子节点集合
);

// 获取所有叶子节点
List<Menu> leafNodes = TreeUtils.getLeafNodes(menuTree);
List<Menu> leafNodes = TreeUtils.getLeafNodes(menuTree, "children");
```

**特性**：

- 支持自定义字段名
- 可配置顶级节点 ID
- 可选移除孤立节点（无法追溯到根节点的节点）
- 可选移除空子节点集合
- 使用反射动态访问对象字段
- 递归树构建算法

### 2. ID 生成工具 (IdUtils)

生成带时间戳的唯一标识符：

```java
// 生成 32 位 ID（时间戳 yyyyMMddHHmm + 雪花算法 ID）
String id = IdUtils.date32Id();
// 示例: 202601251430123456789012345678

// 生成 24 位编码（时间戳 yyyyMMddHHmm + 7 位随机数）
String code = IdUtils.code24();
// 示例: 2026012514301234567
```

**特性**：

- 基于时间戳，便于排序和追溯
- 集成雪花算法，保证分布式唯一性
- 支持随机数生成

### 3. HTTP SSL 工具 (OkHttpUtil)

为 OkHttp 客户端提供 SSL/TLS 证书处理：

```java
// 创建忽略证书验证的 OkHttpClient
OkHttpClient.Builder builder = new OkHttpClient.Builder();
OkHttpUtil.disableCertCheck(builder.build());

// 获取忽略证书的 SSLContext
SSLContext sslContext = OkHttpUtil.getIgnoreInitedSslContext();

// 获取接受任何主机名的 HostnameVerifier
HostnameVerifier verifier = OkHttpUtil.getIgnoreSslHostnameVerifier();
```

**特性**：

- X509TrustManager 实现，绕过证书验证
- 适用于开发/测试环境的自签名证书
- 同时配置 SSL socket factory 和 hostname verifier

**注意**：仅用于开发/测试环境，生产环境应使用正规证书。

### 4. 数据脱敏工具 (DesensitizationUtil)

对敏感个人信息进行掩码处理：

```java
// 中文姓名（显示首字符）
String name = DesensitizationUtil.chineseName("李明");
// 结果: 李*

// 身份证号（仅显示后 4 位）
String idCard = DesensitizationUtil.idCardNum("110101199001011234");
// 结果: **************1234

// 手机号（仅显示后 4 位）
String phone = DesensitizationUtil.phoneNo("13812345678");
// 结果: *******5678

// 手机号（显示前 3 位和后 4 位）
String mobile = DesensitizationUtil.mobileNo("13812345678");
// 结果: 138****5678

// 地址（隐藏详细地址）
String addr = DesensitizationUtil.address("北京市朝阳区某某街道123号", 6);
// 结果: 北京市朝阳区******

// 邮箱（显示首字母和域名）
String email = DesensitizationUtil.email("example@163.com");
// 结果: e******@163.com

// 银行卡号（显示前 6 位和后 4 位）
String card = DesensitizationUtil.bankCard("6222600123456781234");
// 结果: 622260***********1234

// 联行号（显示前 2 位）
String cnaps = DesensitizationUtil.cnapsCode("123456789012");
// 结果: 12**********

// 通用脱敏方法
String masked = DesensitizationUtil.sensitive("敏感信息", 2, 2);
// 结果: 敏感****信息

// 自适应脱敏（根据字符串长度自动调整）
String adaptive = DesensitizationUtil.adaptive("这是一段敏感信息");
```

**特性**：

- 多种脱敏策略适配不同数据类型
- 可自定义掩码字符（默认：*）
- 灵活的填充大小控制
- 支持 null 输入处理

### 5. Bean 操作工具 (BeanUtils)

扩展 Spring 的 BeanUtils，提供额外的 Bean 反射工具：

```java
// 复制 Bean 属性
User source = new User();
User dest = new User();
BeanUtils.copyBeanProp(dest, source);

// 获取所有 setter 方法
List<Method> setters = BeanUtils.getSetterMethods(user);

// 获取所有 getter 方法
List<Method> getters = BeanUtils.getGetterMethods(user);

// 检查两个方法名是否表示同一属性
boolean isSame = BeanUtils.isMethodPropEquals("setName", "getName");
// 结果: true
```

**特性**：

- 扩展 Spring 的 BeanUtils
- 基于正则表达式的方法名模式匹配
- 基于反射的方法发现
- 工具类模式（私有构造函数）

### 6. 文件操作工具 (FileUtils)

文件处理和下载工具：

```java
// 将文件写入输出流
FileUtils.writeBytes("/path/to/file.pdf", outputStream);

// 删除文件
FileUtils.deleteFile("/path/to/file.txt");

// 验证文件名
boolean isValid = FileUtils.isValidFilename("document.pdf");

// 检查文件是否允许下载
boolean canDownload = FileUtils.checkAllowDownload("/uploads/file.pdf");

// 从路径提取文件名
String fileName = FileUtils.getName("/path/to/file.txt");
// 结果: file.txt

// 获取文件扩展名
String suffix = FileUtils.getSuffix("/path/to/file.pdf");
// 结果: pdf

// 设置文件下载响应头
FileUtils.setAttachmentResponseHeader(response, "报告.pdf");

// URL 百分号编码
String encoded = FileUtils.percentEncode("文件名.pdf");
```

**特性**：

- 跨平台路径处理（Windows \ 和 Unix /）
- 浏览器特定的文件名编码（IE、Firefox、Chrome）
- 文件验证和安全检查
- 防止目录遍历攻击（.. 检测）
- 支持允许的文件扩展名白名单

### 7. 地理计算工具 (GeoUtils)

计算地理坐标之间的距离：

```java
// 计算两点之间的距离（单位：米）
double distance = GeoUtils.getDistance(
    116.404,  // 经度1
    39.915,   // 纬度1
    121.472,  // 经度2
    31.231    // 纬度2
);
// 结果: 约 1067545 米（北京到上海的距离）
```

**特性**：

- 使用 Haversine 公式进行精确距离计算
- 地球半径常量：6378137 米
- 将角度转换为弧度进行计算
- 返回距离单位为米

### 8. 邮件发送工具 (EmailBeanUtils)

流式 API 用于编写和发送邮件：

```java
@Autowired
private EmailBeanUtils emailBeanUtils;

// 发送简单文本邮件
emailBeanUtils
    .from("sender@example.com")
    .toEmail("recipient@example.com")
    .subject("测试邮件")
    .text("这是邮件内容")
    .sendMail();

// 发送 HTML 邮件
emailBeanUtils
    .from("sender@example.com")
    .toEmail("recipient@example.com")
    .subject("HTML 邮件")
    .text("<h1>标题</h1><p>内容</p>", true)
    .sendMail();

// 发送带附件的邮件
emailBeanUtils
    .from("sender@example.com")
    .toEmail("recipient@example.com")
    .subject("带附件的邮件")
    .text("请查收附件")
    .addAttachment("报告.pdf", new File("/path/to/report.pdf"))
    .sendMail();

// 发送带抄送和密送的邮件
emailBeanUtils
    .from("sender@example.com")
    .toEmail("recipient@example.com")
    .cc("cc@example.com")
    .bcc("bcc@example.com")
    .subject("邮件主题")
    .text("邮件内容")
    .sendMail();

// 添加内联图片
emailBeanUtils
    .from("sender@example.com")
    .toEmail("recipient@example.com")
    .subject("带图片的邮件")
    .text("<img src='cid:logo'>", true)
    .addInline("logo", new File("/path/to/logo.png"))
    .sendMail();
```

**特性**：

- 流式构建器模式
- Spring 组件，原型作用域
- 支持 HTML 邮件
- 附件和内联图片支持
- 集成 Spring 的 JavaMailSender

### 9. 数字转换工具 (ArabicNumeralsUtils)

阿拉伯数字与中文数字互转：

```java
// 中文数字转整数
int num1 = ArabicNumeralsUtils.chineseTransIntNumber("一千二百三十四");
// 结果: 1234

int num2 = ArabicNumeralsUtils.chineseTransIntNumber("三万五千");
// 结果: 35000

// 整数转中文数字
String chinese1 = ArabicNumeralsUtils.intTransIntChineseNumber(1234);
// 结果: 一千二百三十四

String chinese2 = ArabicNumeralsUtils.intTransIntChineseNumber(10000);
// 结果: 一万

// BigDecimal 转中文数字
BigDecimal decimal = new BigDecimal("123.45");
String chinese3 = ArabicNumeralsUtils.bigDecimalTransIntChineseNumber(decimal);
// 结果: 一百二十三点四五
```

**特性**：

- 支持中文数字单位：零一二三四五六七八九（0-9）
- 支持单位：十百千万亿（10, 100, 1000, 10000, 100000000）
- 处理负数（负前缀）
- 处理小数（点分隔符）
- 支持最大整数值
- 基于正则表达式清理冗余零

### 10. 图片处理工具 (ImageUtils)

从 URL 加载和获取图片文件：

```java
// 获取图片字节数组
byte[] imageBytes = ImageUtils.getImage("http://example.com/image.jpg");

// 获取图片输入流
InputStream imageStream = ImageUtils.getFile("http://example.com/image.jpg");

// 从 URL 读取文件（带超时）
byte[] fileBytes = ImageUtils.readFile("http://example.com/file.pdf");
```

**特性**：

- 支持本地和远程 URL
- 可配置连接超时（30 秒）
- 可配置读取超时（60 秒）
- 返回字节数组或输入流
- 使用 SLF4J 记录错误日志

### 11. 自定义异常 (YJBaseException)

框架的基础异常类：

```java
// 基本用法
throw new YJBaseException("操作失败");

// 带模块和错误码
throw new YJBaseException("user-service", "USER_NOT_FOUND",
    new Object[]{userId}, "用户不存在");

// 带参数
throw new YJBaseException("USER_NOT_FOUND", new Object[]{userId});

// 包装其他异常
try {
    // 操作
} catch (Exception e) {
    throw new YJBaseException(e);
}
```

**特性**：

- 模块信息跟踪
- 错误码支持
- 错误消息参数化
- 默认消息回退
- 多种构造函数重载

### 12. 常量类

#### BaseConstants - 基础常量

```java
// 节点 ID
Long topId = BaseConstants.TOP_ID;        // 0L - 顶级节点
Long commonId = BaseConstants.COMMON_ID;  // 0L - 通用节点
Long noneId = BaseConstants.NONE_ID;      // -2L - 空节点

// 操作类型枚举
BaseConstants.Operate.ADD     // 新增
BaseConstants.Operate.EDIT    // 编辑
BaseConstants.Operate.DELETE  // 删除

// 状态枚举
BaseConstants.Status.NORMAL    // 正常
BaseConstants.Status.DISABLE   // 禁用
BaseConstants.Status.EXCEPTION // 异常

// 是否枚举
BaseConstants.Whether.YES  // 是
BaseConstants.Whether.NO   // 否

// 实体字段映射
BaseConstants.Entity.ID        // id
BaseConstants.Entity.PARENT_ID // parentId
BaseConstants.Entity.SORT      // sort
BaseConstants.Entity.CHILDREN  // children
```

#### DictConstants - 字典常量

```java
// 字典类型
DictConstants.DictType.SYS_NORMAL_DISABLE  // 系统开关
DictConstants.DictType.SYS_SHOW_HIDE       // 显示隐藏
DictConstants.DictType.SYS_YES_NO          // 是否
DictConstants.DictType.SYS_USER_SEX        // 用户性别

// 是否枚举
DictConstants.DicYesNo.Y  // 是
DictConstants.DicYesNo.N  // 否

// 显示隐藏枚举
DictConstants.DicShowHide.SHOW  // 显示
DictConstants.DicShowHide.HIDE  // 隐藏

// 状态枚举
DictConstants.DicStatus.NORMAL  // 正常
DictConstants.DicStatus.FAIL    // 失败
```

#### TenantConstants - 租户常量

```java
// 租户字段名
String tenantField = TenantConstants.TENANT_ID;  // "tenant_id"
String commonField = TenantConstants.COMMON_ID;  // "is_common"

// 数据源标识
String master = TenantConstants.MASTER;    // "#master"
String slave = TenantConstants.ISOLATE;    // "#isolute"
String source = TenantConstants.SOURCE;    // "#sourceName"

// 需要租户隔离的表
String[] tables = TenantConstants.SLAVE_TABLE;  // 28 个表

// 数据源类型枚举
TenantConstants.Source.MASTER    // 主数据源
TenantConstants.Source.SLAVE     // 从数据源
TenantConstants.Source.REGISTER  // 注册数据源

// 同步类型枚举
TenantConstants.SyncType.UNCHANGED  // 未改变
TenantConstants.SyncType.REFRESH    // 刷新
TenantConstants.SyncType.ADD        // 新增
TenantConstants.SyncType.DELETE     // 删除
```

#### TokenConstants - Token 常量

```java
// HTTP 头名称
String header = TokenConstants.AUTHENTICATION;  // "Authorization"

// Token 前缀
String prefix = TokenConstants.PREFIX;  // "Bearer "

// Token 密钥
String secret = TokenConstants.SECRET;  // "abcdefghijklmnopqrstuvwxyz"
```

#### SqlConstants - SQL 常量

```java
// 批量查询限制
int batchSize = SqlConstants.DEFAULT_BATCH_SIZE;  // 1000

// 单条结果限制
String limitOne = SqlConstants.LIMIT_ONE;  // "limit 1"

// 祖先查找 SQL 函数
String ancestorsFind = SqlConstants.ANCESTORS_FIND;
// "find_in_set({0}, ancestors)"

// 实体字段枚举
SqlConstants.Entity.ID         // id
SqlConstants.Entity.PARENT_ID  // parentId
SqlConstants.Entity.STATUS     // status
SqlConstants.Entity.SORT       // sort
SqlConstants.Entity.ANCESTORS  // ancestors
```

#### MimeTypeUtils - MIME 类型常量

```java
// MIME 类型
String png = MimeTypeUtils.IMAGE_PNG;    // "image/png"
String jpg = MimeTypeUtils.IMAGE_JPG;    // "image/jpg"
String jpeg = MimeTypeUtils.IMAGE_JPEG;  // "image/jpeg"

// 文件扩展名数组
String[] imageExts = MimeTypeUtils.IMAGE_EXTENSION;
// ["bmp", "gif", "jpg", "jpeg", "png"]

String[] mediaExts = MimeTypeUtils.MEDIA_EXTENSION;
// ["mp3", "wav", "avi", "rmvb", "mp4", ...]

String[] allowedExts = MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION;
// 18 种允许的文件类型

// 获取扩展名
String ext = MimeTypeUtils.getExtension("image/png");
// 结果: "png"
```

#### CharConstants - 字符常量

```java
// 字母
char upperA = CharConstants.UPPER_A;  // 'A'
char lowerA = CharConstants.LOWER_A;  // 'a'

// 符号
char dot = CharConstants.DOT;          // '.'
char comma = CharConstants.COMMA;      // ','
char colon = CharConstants.COLON;      // ':'
char slash = CharConstants.SLASH;      // '/'
char dash = CharConstants.DASH;        // '-'
char underscore = CharConstants.UNDERSCORE;  // '_'

// 括号
char leftBrace = CharConstants.LEFT_BRACE;    // '{'
char rightBrace = CharConstants.RIGHT_BRACE;  // '}'

// 特殊字符
char space = CharConstants.SPACE;      // ' '
char newline = CharConstants.NEWLINE;  // '\n'
char tab = CharConstants.TAB;          // '\t'
```

### 13. 函数式接口 (CheckedConsumer)

支持受检异常的函数式接口：

```java
// 在 lambda 表达式中处理受检异常
CheckedConsumer<String> consumer = (str) -> {
    // 可以抛出受检异常
    Files.readString(Path.of(str));
};

// 使用
try {
    consumer.accept("/path/to/file.txt");
} catch (Throwable e) {
    // 处理异常
}
```

**特性**：

- 泛型类型参数 T
- 抛出 Throwable 用于受检异常处理
- 可序列化，适用于分布式系统
- 支持可空参数和返回值

## 依赖引入

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-utils</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

## 依赖项

- **Jakarta Servlet API**: Servlet 支持
- **Apache POI (poi-ooxml)**: Excel 文件处理
- **Spring Web**: Spring Web 框架
- **Apache Commons Lang3 (3.18.0)**: 通用工具
- **Hutool (hutool-all)**: 综合工具库
- **Spring Boot Mail Starter**: 邮件功能
- **OkHttp3**: HTTP 客户端
- **Commons IO (2.7)**: 文件 I/O 工具
- **Transmittable ThreadLocal**: 分布式系统的 ThreadLocal 工具

## 设计模式

- **工具类模式**: 静态方法，私有构造函数
- **建造者模式**: EmailBeanUtils 流式 API
- **枚举模式**: 常量组织为带元数据的枚举
- **反射模式**: TreeUtils 和 BeanUtils 中的动态字段访问
- **函数式接口**: CheckedConsumer 支持 lambda
- **扩展模式**: BeanUtils 和 FileUtils 扩展 Apache Commons 类

## 使用场景

1. **数据组织**: TreeUtils 用于菜单/部门层次结构
2. **安全**: DesensitizationUtil 用于日志/报告中的 PII 保护
3. **文件管理**: FileUtils 用于安全文件下载
4. **邮件通知**: EmailBeanUtils 用于系统通知
5. **地理功能**: GeoUtils 用于基于位置的服务
6. **多租户系统**: TenantConstants 用于数据隔离
7. **API 开发**: 常量用于标准化 API 响应
8. **开发/测试**: OkHttpUtil 用于在开发环境中绕过 SSL

## 注意事项

1. **SSL 证书**: `OkHttpUtil` 仅用于开发/测试环境，生产环境应使用正规证书
2. **文件安全**: `FileUtils` 包含路径遍历防护，但仍需配置白名单扩展名
3. **数据脱敏**: 脱敏后的数据不可逆，确保在正确的场景使用
4. **邮件发送**: `EmailBeanUtils` 需要配置 Spring Mail 属性
5. **树形结构**: `TreeUtils` 使用反射，大数据量时注意性能
6. **地理计算**: `GeoUtils` 使用 Haversine 公式，适用于球面距离计算
7. **ID 生成**: `IdUtils` 依赖系统时间，确保服务器时间同步

## 版本要求

- JDK 17+
- Spring Boot 4.0.6+
- Maven 3.8+

## 相关模块

- `carlos-spring-boot-core`: 核心基础模块
- `carlos-spring-boot-starter-web`: Spring Boot 自动配置
- `carlos-spring-boot-starter-encrypt`: 加密工具
