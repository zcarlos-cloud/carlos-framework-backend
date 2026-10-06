server:
port: 8080
compression:
enabled: true
mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
min-response-size: 128KB
tomcat:
threads:
max: 32
servlet:
context-path: /
spring:
profiles:
active: dm,bootdm
servlet:
multipart:
enabled: true
max-file-size: 200MB # 单文件最大限制
max-request-size: 200MB # 一次上传所有文件的最大限制. 如果接口只支持单文件,则该值与上面相同即可
application:
name: carlos-test
# spring配置
datasource:
druid:
stat-view-servlet:
enabled: true
loginUsername: admin
loginPassword: 123456
dynamic:
primary: master
druid:
break-after-acquire-failure: false
fail-fast: true
connection-error-retry-attempts: 10
initial-size: 5
min-idle: 5
max-active: 600
max-wait: 60000
time-between-eviction-runs-millis: 60000
min-evictable-idle-time-millis: 300000
validation-query: SELECT 1
test-while-idle: true
test-on-borrow: false
test-on-return: false
pool-prepared-statements: true
max-pool-prepared-statement-per-connection-size: 20
#                filters: stat,slf4j
connection-properties:
druid.stat.mergeSql: false
druid.stat.slowSqlMillis: 5000
filter:
stat:
merge-sql: false
wall:
enabled: false

############################## application start ##############################
carlos:
boot:
cors:
enable: true
enums:
scan-package: com.carlos.test
enabled: true
# 自定义项目信息
info:
project-name: carlos-test
groupId: com.carlos
author: Carlos
description:
sourceEncoding: utf-8
version: 1.0.0-SNAPSHOT
domain: www.zcarlos.com

############################ application end ###############################

management:
endpoints:
web:
exposure:
include: "*"


##############################  日志配置 start  ###########################
logging:
config: classpath:config/logback.xml
level:
root: info
com.carlos: debug
file:
path: logs/carlos-test/
##############################  日志配置 end   ###########################


knife4j: # 是否开启Knife4j增强模式 默认 false
enable: true
# 是否是生产环境
production: false
# knife4j配置
cors: false
# 个性化配置 详情配置参照源码
setting: # 是否开启Debug调试
enableDebug: true
language: zh-CN
enableSwaggerModels: true
enableDocumentManage: true
swaggerModelName: 实体类列表
enableVersion: true
enableReloadCacheParameter: false
enableAfterScript: true
enableFilterMultipartApiMethodType: POST
enableFilterMultipartApis: false
enableRequestCache: true
enableHost: false
enableHostText: 192.168.0.193:8000
# 是否开启自定义主页内容
enableHomeCustom: false
# 主页内容Markdown文件路径
homeCustomLocation: classpath:markdown/home.md
# 开启搜索
enableSearch: true
# 显示页底信息
enableFooter: false
enableFooterCustom: true
footerCustomContent: 云津智慧科技有限公司
# 动态参数调试
enableDynamicParameter: true
# 显示OpenAPI规范
enableOpenApi: true
enableGroup: true
# 是否开启BasicHttp验证
basic:
enable: false
username: carlos
password: hadoop543216
#################################### 接口文档 Swagger end ###################################

