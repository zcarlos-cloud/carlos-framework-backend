server:
port: 8080
spring:
  profiles:
    active: ${r'${PROFILE:local}'}
  application:
name: ${project.artifactId}
config:
import:
# 1. 公共配置（所有环境共享）
#      - optional:nacos:${spring.application.name}.yaml?group=DEFAULT_GROUP&refreshEnabled=true
# 2. 环境专属配置（覆盖公共配置）
#      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yaml?group=DEFAULT_GROUP&refreshEnabled=true
# 3. 额外共享配置（如 Redis、MySQL 配置）
- optional:nacos:common.yml?DEFAULT_GROUP&refreshEnabled=true
- optional:nacos:redis.yml?DEFAULT_GROUP&refreshEnabled=true
- optional:nacos:db.yml?DEFAULT_GROUP&refreshEnabled=true
  cloud:
    nacos:
      discovery:
        enabled: true
      config:
        enabled: true
        file-extension: yml
        refresh-enabled: true

# 本地调试环境
---
spring:
  config:
    activate:
      on-profile: local
  cloud:
    nacos:
      username: ${r'${NACOS_USERNAME:@nacos.username@}'}
      password: ${r'${NACOS_PASSWORD:@nacos.password@}'}
      discovery:
        server-addr: ${r'${NACOS_ADDR:@nacos.addr@}'}
        namespace: ${r'${NACOS_NAMESPACE:@nacos.namespace@}'}
        group: ${r'${NACOS_GROUP:@nacos.group@}'}
      config:
        server-addr: ${r'${NACOS_ADDR:@nacos.addr@}'}
        namespace: ${r'${NACOS_NAMESPACE:@nacos.namespace@}'}
        group: ${r'${NACOS_GROUP:@nacos.group@}'}


# 部署采用配置文件
---
spring:
  config:
    activate:
      on-profile: deploy
  cloud:
    nacos:
      username: ${r'${NACOS_USERNAME}'}
      password: ${r'${NACOS_PASSWORD}'}
      discovery:
        server-addr: ${r'${NACOS_ADDR:127.0.0.1:8848}'}
        namespace: ${r'${NACOS_NAMESPACE}'}
        group: ${r'${NACOS_GROUP}'}
      config:
        namespace: ${r'${NACOS_NAMESPACE}'}
        server-addr: ${r'${NACOS_ADDR}'}
        group: ${r'${NACOS_GROUP}'}
