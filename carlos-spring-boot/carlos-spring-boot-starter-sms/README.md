# carlos-spring-boot-starter-sms

短信发送组件，提供统一的短信发送接口，支持多个短信服务商。

## 功能特性

- **多厂商支持**: 支持阿里云、华为云、创蓝等多个短信服务商
- **负载均衡**: 支持多厂商负载均衡和故障转移
- **模板管理**: 支持短信模板配置和管理
- **发送限流**: 支持单手机号发送频率限制
- **异步发送**: 支持异步发送，提高响应速度
- **发送记录**: 自动记录短信发送历史

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-sms</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 配置示例

```yaml
carlos:
  sms:
    # 配置类型: yaml 或 database
    config-type: yaml
    # 是否开启短信拦截
    restricted: true
    # 单手机号每日最大发送量
    account-max: 20
    # 单手机号每分钟最大发送量
    minute-max: 5
    # 线程池配置
    core-pool-size: 5
    max-pool-size: 16
    queue-capacity: 50
    # 短信模板配置
    templates:
      verify-code:
        code: verify-code
        template-id: SMS_123456
        content: '您的验证码是：{code}，请在10分钟内输入。'
        signature: '【Carlos】'
    # 短信厂商配置
    blends:
      aliyun:
        supplier: aliyun
        access-key-id: ${ALIYUN_SMS_ACCESS_KEY}
        access-key-secret: ${ALIYUN_SMS_ACCESS_SECRET}
        weight: 1
        retry-interval: 5
        max-retries: 3
      huawei:
        supplier: huawei
        app-key: ${HUAWEI_SMS_APP_KEY}
        app-secret: ${HUAWEI_SMS_APP_SECRET}
        weight: 1
```

## 使用示例

### 发送验证码短信

```java
@Autowired
private SmsTemplate smsTemplate;

public void sendVerifyCode(String phone) {
    String code = RandomUtil.randomNumbers(6);

    Map<String, String> params = new HashMap<>();
    params.put("code", code);

    SmsResult result = smsTemplate.send(
        phone,
        "verify-code",
        params
    );

    if (result.isSuccess()) {
        System.out.println("短信发送成功");
    }
}
```

### 批量发送短信

```java
public void sendBatch() {
    List<String> phones = Arrays.asList(
        "13800138000",
        "13800138001",
        "13800138002"
    );

    Map<String, String> params = new HashMap<>();
    params.put("eventId", "E20260127001");

    List<SmsResult> results = smsTemplate.sendBatch(
        phones,
        "event-notify",
        params
    );
}
```

### 自定义短信内容

```java
public void sendCustom(String phone, String content) {
    SmsResult result = smsTemplate.sendCustom(
        phone,
        content,
        "【Carlos】"
    );
}
```

## 支持的短信厂商

| 厂商  | 标识        | 状态    |
|-----|-----------|-------|
| 阿里云 | aliyun    | ✅ 已支持 |
| 华为云 | huawei    | ✅ 已支持 |
| 创蓝  | chuanglan | ✅ 已支持 |
| 沃云  | wocloud   | ✅ 已支持 |

## 负载均衡策略

组件支持基于权重的负载均衡：

```yaml
blends:
  aliyun:
    weight: 2  # 权重2
  huawei:
    weight: 1  # 权重1
```

上述配置表示阿里云和华为云按2:1的比例分配流量。

## 发送限流

为防止短信轰炸，组件内置限流功能：

- **每日限制**: 单手机号每日最大发送量
- **每分钟限制**: 单手机号每分钟最大发送量

超过限制将拒绝发送并返回错误。

## 依赖模块

- **carlos-spring-boot-core**: 核心基础功能
- **carlos-spring-boot-starter-redis**: Redis缓存支持（用于限流）

## 注意事项

- **生产环境必须配置真实的access-key和secret**
- 短信签名必须在服务商平台备案
- 短信模板需要提前在服务商平台审核通过
- 注意短信费用，避免恶意刷短信
- 建议启用发送限流，防止短信轰炸
- 敏感操作建议增加图形验证码

## 版本要求

- JDK 17+
- Spring Boot 3.x
