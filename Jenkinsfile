#!/usr/bin/env groovy

// ============================================
// Carlos Framework 框架依赖发布流水线
// 发布目标: Nexus (192.168.3.30:8081)
// 发布内容:
//   - carlos-dependencies (BOM)
//   - carlos-parent (父 POM)
//   - carlos-spring-boot (及所有 starter 子模块)
// ============================================

pipeline {
    agent any
    tools {
        jdk 'jdk-21'
        maven 'maven-3.9'
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '7', numToKeepStr: '10'))
        disableConcurrentBuilds()
    }
    parameters {
        choice(
            name: 'DEPLOY_TYPE',
            choices: ['snapshot', 'release'],
            description: '发布类型: snapshot 发布到 maven-snapshots, release 发布到 maven-releases'
        )
        booleanParam(name: 'SKIP_TEST', defaultValue: true, description: '跳过测试')
    }
    stages {
        stage('Maven Deploy') {
            steps {
                configFileProvider([configFile(fileId: 'carlos-maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'nexus-carlos',
                            usernameVariable: 'NEXUS_USERNAME',
                            passwordVariable: 'NEXUS_PASSWORD'
                        )
                    ]) {
                        script {
                            def skipTests = params.SKIP_TEST ? '-DskipTests' : ''
                            sh """
                                mvn -B -U -s ${MAVEN_SETTINGS} clean deploy ${skipTests} -pl carlos-dependencies,carlos-parent,carlos-spring-boot -am -P carlos
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo '框架依赖发布成功'
            echo "   Nexus Snapshots: http://192.168.3.10:8081/nexus/repository/maven-snapshots/"
            echo "   Nexus Releases:  http://192.168.3.10:8081/nexus/repository/maven-releases/"
        }
        failure {
            error('框架依赖发布失败，请检查 Nexus 连接与 Maven settings.xml 配置')
        }
    }
}
