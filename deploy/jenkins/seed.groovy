#!/usr/bin/env groovy

// ============================================
// Carlos Framework Job DSL Seed Script
// 批量创建模块级构建流水线
// 使用方式: 在 Jenkins 中创建 Seed Job，引用此脚本
// ============================================

def gitUrl = 'http://192.168.3.10:28080/project/carlos-framework.git'
def credentialsId = 'gitlab-carlos'
def backendJenkinsfilePath = 'deploy/jenkins/Jenkinsfile.backend'
def frontendJenkinsfilePath = 'deploy/jenkins/Jenkinsfile.frontend'

// ============================================
// 模块定义
// ============================================

def foundationModules = [
    [name: 'carlos-dependencies', path: 'carlos-dependencies', desc: 'BOM依赖版本管理'],
    [name: 'carlos-parent',       path: 'carlos-parent',       desc: '统一父POM'],
    [name: 'carlos-commons',      path: 'carlos-commons',      desc: '公共工具模块'],
]

def springBootModules = [
    [name: 'carlos-spring-boot-core',              path: 'carlos-spring-boot/carlos-spring-boot-core',              desc: '核心抽象与自动配置'],
    [name: 'carlos-spring-boot-redis',             path: 'carlos-spring-boot/carlos-spring-boot-redis',             desc: 'Redis + Redisson + Caffeine'],
    [name: 'carlos-spring-boot-starter-ai',        path: 'carlos-spring-boot/carlos-spring-boot-starter-ai',        desc: 'AI 服务集成'],
    [name: 'carlos-spring-boot-starter-apm',       path: 'carlos-spring-boot/carlos-spring-boot-starter-apm',       desc: 'APM 集成 (SkyWalking)'],
    [name: 'carlos-spring-boot-starter-datacenter',path: 'carlos-spring-boot/carlos-spring-boot-starter-datacenter',desc: '数据中心集成'],
    [name: 'carlos-spring-boot-starter-datascope', path: 'carlos-spring-boot/carlos-spring-boot-starter-datascope', desc: '数据权限控制'],
    [name: 'carlos-spring-boot-starter-disruptor', path: 'carlos-spring-boot/carlos-spring-boot-starter-disruptor', desc: 'Disruptor 高性能队列'],
    [name: 'carlos-spring-boot-starter-encrypt',   path: 'carlos-spring-boot/carlos-spring-boot-starter-encrypt',   desc: '国密加密 (SM2/SM4)'],
    [name: 'carlos-spring-boot-starter-integration',path: 'carlos-spring-boot/carlos-spring-boot-starter-integration',desc: '第三方集成框架'],
    [name: 'carlos-spring-boot-starter-json',      path: 'carlos-spring-boot/carlos-spring-boot-starter-json',      desc: 'JSON 序列化 (Fastjson2)'],
    [name: 'carlos-spring-boot-starter-log',       path: 'carlos-spring-boot/carlos-spring-boot-starter-log',       desc: '日志增强'],
    [name: 'carlos-spring-boot-starter-migration', path: 'carlos-spring-boot/carlos-spring-boot-starter-migration', desc: '数据库迁移'],
    [name: 'carlos-spring-boot-starter-mongodb',   path: 'carlos-spring-boot/carlos-spring-boot-starter-mongodb',   desc: 'MongoDB 集成'],
    [name: 'carlos-spring-boot-starter-mq',        path: 'carlos-spring-boot/carlos-spring-boot-starter-mq',        desc: '消息队列抽象'],
    [name: 'carlos-spring-boot-starter-mybatis',   path: 'carlos-spring-boot/carlos-spring-boot-starter-mybatis',   desc: 'MyBatis-Plus 集成'],
    [name: 'carlos-spring-boot-starter-openapi',   path: 'carlos-spring-boot/carlos-spring-boot-starter-openapi',   desc: 'OpenAPI/Swagger 文档'],
    [name: 'carlos-spring-boot-starter-oss',       path: 'carlos-spring-boot/carlos-spring-boot-starter-oss',       desc: 'OSS 对象存储抽象'],
    [name: 'carlos-spring-boot-starter-security',  path: 'carlos-spring-boot/carlos-spring-boot-starter-security',  desc: '安全认证'],
    [name: 'carlos-spring-boot-starter-sms',       path: 'carlos-spring-boot/carlos-spring-boot-starter-sms',       desc: '短信发送抽象'],
    [name: 'carlos-spring-boot-starter-snowflake', path: 'carlos-spring-boot/carlos-spring-boot-starter-snowflake', desc: '分布式ID生成'],
    [name: 'carlos-spring-boot-starter-translation',path: 'carlos-spring-boot/carlos-spring-boot-starter-translation',desc: '国际化翻译'],
    [name: 'carlos-spring-boot-starter-web',       path: 'carlos-spring-boot/carlos-spring-boot-starter-web',       desc: 'Web 自动配置'],
    [name: 'carlos-spring-cloud-starter',          path: 'carlos-spring-boot/carlos-spring-cloud-starter',          desc: 'Spring Cloud Alibaba'],
]

def businessModules = [
    [name: 'carlos-auth',         path: 'carlos-integration/carlos-auth',         desc: 'OAuth2 认证授权'],
    [name: 'carlos-system',       path: 'carlos-integration/carlos-system',       desc: '系统管理模块'],
    [name: 'carlos-org',          path: 'carlos-integration/carlos-org',          desc: '组织/用户中心'],
    [name: 'carlos-audit',        path: 'carlos-integration/carlos-audit',        desc: '审计日志模块'],
    [name: 'carlos-message',      path: 'carlos-integration/carlos-message',      desc: '统一消息中心'],
    [name: 'carlos-gateway',      path: 'carlos-integration/carlos-gateway',      desc: 'Spring Cloud Gateway'],
]

def integrationModules = [
    [name: 'carlos-license-core',                    path: 'carlos-integration/carlos-license/carlos-license-core',                    desc: '许可证核心'],
    [name: 'carlos-license-generate',                path: 'carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate', desc: '许可证生成 (开发用)'],
    [name: 'carlos-license-verify',                  path: 'carlos-integration/carlos-license/carlos-spring-boot-starter-license-verify',   desc: '许可证验证 (生产用)'],
    [name: 'carlos-tools',                           path: 'carlos-integration/carlos-tools',                                          desc: 'GUI 代码生成器'],
]

def sampleModules = [
    [name: 'carlos-sample-apm',        path: 'carlos-samples/carlos-sample-apm',        desc: 'APM 示例'],
    [name: 'carlos-sample-cloud',      path: 'carlos-samples/carlos-sample-cloud',      desc: 'Spring Cloud 示例'],
    [name: 'carlos-sample-datacenter', path: 'carlos-samples/carlos-sample-datacenter', desc: '数据中心示例'],
    [name: 'carlos-sample-datascope',  path: 'carlos-samples/carlos-sample-datascope',  desc: '数据权限示例'],
    [name: 'carlos-sample-disruptor',  path: 'carlos-samples/carlos-sample-disruptor',  desc: 'Disruptor 示例'],
    [name: 'carlos-sample-docking',    path: 'carlos-samples/carlos-sample-docking',    desc: '钉钉集成示例'],
    [name: 'carlos-sample-encrypt',    path: 'carlos-samples/carlos-sample-encrypt',    desc: '加密示例'],
    [name: 'carlos-sample-log',        path: 'carlos-samples/carlos-sample-log',        desc: '日志示例'],
    [name: 'carlos-sample-mongodb',    path: 'carlos-samples/carlos-sample-mongodb',    desc: 'MongoDB 示例'],
    [name: 'carlos-sample-mq',         path: 'carlos-samples/carlos-sample-mq',         desc: '消息队列示例'],
    [name: 'carlos-sample-mybatis',    path: 'carlos-samples/carlos-sample-mybatis',    desc: 'MyBatis-Plus 示例'],
    [name: 'carlos-sample-openapi',    path: 'carlos-samples/carlos-sample-openapi',    desc: 'OpenAPI 示例'],
    [name: 'carlos-sample-oss',        path: 'carlos-samples/carlos-sample-oss',        desc: 'OSS 示例'],
    [name: 'carlos-sample-redis',      path: 'carlos-samples/carlos-sample-redis',      desc: 'Redis 示例'],
    [name: 'carlos-sample-sms',        path: 'carlos-samples/carlos-sample-sms',        desc: '短信示例'],
    [name: 'carlos-sample-snowflake',  path: 'carlos-samples/carlos-sample-snowflake',  desc: 'Snowflake 示例'],
    [name: 'carlos-sample-web',        path: 'carlos-samples/carlos-sample-web',        desc: 'Web 示例'],
    [name: 'carlos-test',              path: 'carlos-samples/carlos-test',              desc: '测试应用'],
]

def frontendModules = [
    [name: 'carlos-ui', path: 'carlos-ui', desc: '前端 Vue3 管理后台', isFrontend: true],
]

// ============================================
// 辅助函数: 创建模块流水线
// ============================================

def createModuleJob(String folderName, Map module, String scmUrl, String credsId, String jfPath) {
    String jobName = "${folderName}/${module.name}"
    String modulePath = module.path
    String moduleName = module.name
    String description = module.desc ?: "Build for ${module.name}"
    boolean isFrontend = module.isFrontend ?: false

    pipelineJob(jobName) {
        description(description)
        keepDependencies(false)

        environmentVariables {
            env('MODULE_NAME', moduleName)
            env('MODULE_PATH', modulePath)
            env('IS_FRONTEND', isFrontend ? 'true' : 'false')
        }

        logRotator {
            daysToKeep(7)
            numToKeep(10)
        }

        properties {
            disableConcurrentBuilds {}
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(scmUrl)
                            credentials(credsId)
                        }
                        branch('*/main')
                        extensions {
                            cloneOptions {
                                shallow(true)
                                depth(10)
                                noTags(false)
                            }
                        }
                    }
                }
                scriptPath(jfPath)
                lightweight(false)
            }
        }

        triggers {
            scm('H/10 * * * *')
        }

        if (isFrontend) {
            parameters {
                stringParam('NODE_VERSION', '20', 'Node.js 版本')
            }
        }
    }

    println "Created job: ${jobName}"
}

// ============================================
// 创建文件夹结构
// ============================================

folder('carlos') {
    description('Carlos Framework 模块构建流水线')
}

folder('carlos/foundation') {
    description('基础模块: dependencies, parent, commons')
}

folder('carlos/spring-boot') {
    description('Spring Boot Starter 模块')
}

folder('carlos/business') {
    description('业务模块: auth, system, org, audit, message, gateway')
}

folder('carlos/integration') {
    description('第三方集成模块: license, tools')
}

folder('carlos/samples') {
    description('示例应用程序')
}

folder('carlos/frontend') {
    description('前端项目')
}

// ============================================
// 创建全局构建流水线 (保留原有根目录发布功能)
// ============================================

pipelineJob('carlos/carlos-framework-deploy') {
    description('Carlos Framework 全局依赖发布流水线 (deploy到Nexus)')
    keepDependencies(false)

    logRotator {
        daysToKeep(14)
        numToKeep(20)
    }

    properties {
        disableConcurrentBuilds {}
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitUrl)
                        credentials(credentialsId)
                    }
                    branch('*/main')
                }
            }
            scriptPath('Jenkinsfile')
            lightweight(false)
        }
    }
}

// ============================================
// 批量创建模块流水线
// ============================================

println "========== 开始创建 Carlos Framework 流水线任务 =========="

//foundationModules.each { mod ->
//    createModuleJob('carlos/foundation', mod, gitUrl, credentialsId, jenkinsfilePath)
//}
//
//springBootModules.each { mod ->
//    createModuleJob('carlos/spring-boot', mod, gitUrl, credentialsId, jenkinsfilePath)
//}

businessModules.each { mod ->
    createModuleJob('carlos/business', mod, gitUrl, credentialsId, backendJenkinsfilePath)
}

//integrationModules.each { mod ->
//    createModuleJob('carlos/integration', mod, gitUrl, credentialsId, jenkinsfilePath)
//}

//sampleModules.each { mod ->
//    createModuleJob('carlos/samples', mod, gitUrl, credentialsId, jenkinsfilePath)
//}

frontendModules.each { mod ->
    createModuleJob('carlos/frontend', mod, gitUrl, credentialsId, frontendJenkinsfilePath)
}

// ============================================
// 创建汇总视图
// ============================================

nestedView('Carlos Framework') {
    description('Carlos Framework 全部流水线视图')
    filterBuildQueue(false)
    filterExecutors(false)

    views {
        listView('全部任务') {
            jobs {
                regex('carlos/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }

        listView('基础模块') {
            jobs {
                regex('carlos/foundation/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }

        listView('Spring Boot Starters') {
            jobs {
                regex('carlos/spring-boot/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }

        listView('业务模块') {
            jobs {
                regex('carlos/business/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }

        listView('集成模块') {
            jobs {
                regex('carlos/integration/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }

        listView('示例项目') {
            jobs {
                regex('carlos/samples/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }

        listView('前端项目') {
            jobs {
                regex('carlos/frontend/.*')
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }
    }
}

println "========== 流水线任务创建完成 =========="
println "总计创建: ${foundationModules.size() + +businessModules.size() + frontendModules.size() + 1} 个任务"
