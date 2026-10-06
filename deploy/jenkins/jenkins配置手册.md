# Carlos Framework - Jenkins 配置手册

> 本文档提供从 0 到 1 的每一步点击/命令操作，跟随执行即可完成全部配置。

---

## 环境信息

| 组件      | 地址                       | 说明                |
|---------|--------------------------|-------------------|
| Jenkins | http://192.168.3.10:8080 | CI/CD 服务器         |
| GitLab  | http://192.168.3.10      | 源码仓库              |
| Harbor  | 192.168.3.30:8633        | 镜像仓库              |
| Nexus   | 192.168.3.30:8081        | Maven 仓库          |
| 部署服务器   | 192.168.3.20             | Docker Compose 运行 |

---

## 目录

1. [第一阶段：Jenkins 安装](#第一阶段jenkins-安装)
2. [第二阶段：安装必需插件](#第二阶段安装必需插件)
3. [第三阶段：配置工具链](#第三阶段配置工具链)
4. [第四阶段：配置凭据](#第四阶段配置凭据)
5. [第五阶段：配置 Maven settings.xml](#第五阶段配置-maven-settingsxml)
6. [第六阶段：创建流水线 Job](#第六阶段创建流水线-job)
7. [第七阶段：配置 GitLab Webhook](#第七阶段配置-gitlab-webhook)
8. [第八阶段：部署服务器准备](#第八阶段部署服务器准备)
9. [第九阶段：首次构建验证](#第九阶段首次构建验证)
10. [第十阶段：安全加固](#第十阶段安全加固)
11. [附录 A：系统全局配置](#附录-a系统全局配置)
12. [附录 B：Jenkins 节点配置](#附录-bjenkins-节点配置)
13. [附录 C：流水线参数速查](#附录-c流水线参数速查)
14. [附录 D：常见问题排查](#附录-d常见问题排查)
15. [附录 E：防火墙端口检查](#附录-e防火墙端口检查)

---

## 第一阶段：Jenkins 安装

### 步骤 1.1：在 192.168.3.10 上安装 Jenkins

SSH 登录到 192.168.3.10，执行以下命令：

```bash
# 1. 创建 Jenkins 数据目录（宿主机持久化）
mkdir -p /data/jenkins/home
chmod 777 /data/jenkins/home

# 2. 拉取并运行 Jenkins LTS（JDK21 版本）
docker run -d \
  --name jenkins \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 50000:50000 \
  -v /data/jenkins/home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(which docker):/usr/bin/docker \
  -e TZ=Asia/Shanghai \
  jenkins/jenkins:2.492.1-lts-jdk21

# 3. 等待容器启动（约 30 秒）
sleep 30

# 4. 查看初始管理员密码
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
# 复制输出的密码，例如：a1b2c3d4e5f6789012345678
```

### 步骤 1.2：初始化配置

1. 浏览器访问 `http://192.168.3.10:8080`
2. 在 **Unlock Jenkins** 页面，粘贴步骤 1.1 获取的密码
3. 点击 **Continue**
4. 在 **Customize Jenkins** 页面，选择 **Install suggested plugins**（先安装推荐插件，后续再补充）
5. 等待插件安装完成（约 5-10 分钟，取决于网络）
6. 在 **Create First Admin User** 页面：
   ```
   Username: admin
   Password: Hadoop@2022
   Confirm password: Hadoop@2022
   Full name: Jenkins Admin
   E-mail address: jenkins@carlos.local
   ```
7. 点击 **Save and Continue**
8. 在 **Instance Configuration** 页面，确认 URL：`http://192.168.3.10:8080/`
9. 点击 **Save and Finish** > **Start using Jenkins**

### 步骤 1.3：确保 Jenkins 容器内可用 Docker

```bash
# 测试 Jenkins 容器内是否能执行 docker 命令
docker exec -it jenkins bash -c "docker ps"
# 预期输出：CONTAINER ID   IMAGE   ...（列出当前运行的容器）

# 如果报错 permission denied，执行：
docker exec jenkins usermod -aG root jenkins
docker restart jenkins
```

---

## 第二阶段：安装必需插件

### 步骤 2.1：通过图形界面安装

1. 点击左侧菜单 **Manage Jenkins**
2. 点击 **Plugins**
3. 点击上方 **Available plugins** 标签
4. 在右上角搜索框依次搜索并勾选以下插件：

| 搜索关键词                  | 勾选插件                              | 用途                                |
|------------------------|-----------------------------------|-----------------------------------|
| `Pipeline`             | Pipeline                          | Declarative Pipeline 支持（必选）       |
| `Pipeline Stage View`  | Pipeline: Stage View              | 可视化流水线阶段（必选）                      |
| `Git`                  | Git                               | Git 仓库拉取（必选）                      |
| `GitLab`               | GitLab                            | GitLab Webhook 集成（必选）             |
| `Maven Integration`    | Maven Integration                 | Maven 构建支持（必选）                    |
| `Config File Provider` | Config File Provider              | 管理 `settings.xml` 等配置文件（必选）       |
| `NodeJS`               | NodeJS                            | Node.js / pnpm 前端构建（必选）           |
| `docker pipeline`      | Docker Pipeline                   | `docker.withRegistry()` 语法支持（必选）  |
| `SSH Agent`            | SSH Agent                         | `sshagent()` 步骤，SSH 免密登录部署服务器（必选） |
| `SSH Build Agents`     | SSH Build Agents                  | 远程构建节点（必选）                        |
| `Blue Ocean`           | Blue Ocean                        | 现代化流水线 UI（推荐）                     |
| `Timestamper`          | Timestamper                       | 控制台输出添加时间戳（推荐）                    |
| `AnsiColor`            | AnsiColor                         | 控制台彩色输出（推荐）                       |
| `Role-based`           | Role-based Authorization Strategy | 基于角色的权限控制（推荐）                     |
| `Workspace Cleanup`    | Workspace Cleanup                 | 构建后清理工作区（必选）                      |

5. 点击页面底部 **Install** 按钮
6. 安装完成后，勾选 **Restart Jenkins when installation is complete and no jobs are running**
7. 等待 Jenkins 重启

### 步骤 2.2：通过脚本批量安装（备选）

如果图形界面搜索太慢，可通过脚本控制台批量安装：

1. 点击 **Manage Jenkins**
2. 点击底部 **Script Console**
3. 粘贴以下 Groovy 脚本并点击 **Run**：

```groovy
def plugins = [
    'pipeline-stage-view',
    'git', 'gitlab-plugin', 'generic-webhook-trigger',
    'maven-plugin', 'config-file-provider', 'nodejs',
    'docker-workflow', 'docker-commons',
    'ssh-agent', 'ssh-slaves',
    'blue-ocean', 'timestamper', 'ansicolor',
    'role-strategy', 'credentials-binding', 'ws-cleanup'
]

def pm = Jenkins.instance.pluginManager
def uc = Jenkins.instance.updateCenter
plugins.each { name ->
    if (!pm.getPlugin(name)) {
        def plugin = uc.getPlugin(name)
        if (plugin) {
            plugin.deploy()
            println "Installing: ${name}"
        } else {
            println "NOT FOUND: ${name}"
        }
    } else {
        println "Already installed: ${name}"
    }
}
println "DONE. Please restart Jenkins."
```

4. 重启 Jenkins：点击右上角用户图标旁的 **注销**，然后重新登录；或在服务器执行 `docker restart jenkins`

---

## 第三阶段：配置工具链

### 步骤 3.1：配置 JDK

1. 点击 **Manage Jenkins > Tools**
2. 找到 **JDK installations** 区域，点击 **Add JDK**
3. 填写：
   ```
   Name: jdk-21
   Install automatically: [取消勾选]
   JAVA_HOME: /opt/java/openjdk
   ```
   > 注：Jenkins 官方 Docker 镜像内置 JDK 21 在 `/opt/java/openjdk`。可通过
   `docker exec jenkins ls /opt/java/openjdk/bin/java` 验证。

4. 点击页面底部 **Save**

### 步骤 3.2：配置 Maven

1. 点击 **Manage Jenkins > Tools**
2. 找到 **Maven installations** 区域，点击 **Add Maven**
3. 填写：
   ```
   Name: maven-3.9
   Install automatically: [勾选]
   Version: 3.9.6
   ```
4. 点击 **Save**

### 步骤 3.3：配置 Node.js

1. 点击 **Manage Jenkins > Tools**
2. 找到 **NodeJS installations** 区域，点击 **Add NodeJS**
3. 填写：
   ```
   Name: node-24
   Install automatically: [勾选]
   Version: NodeJS 24.4.1（或最新 24.x）
   Global npm packages to install: pnpm
   ```
4. 点击 **Save**

### 步骤 3.4：配置 Git

1. 点击 **Manage Jenkins > Tools**
2. 找到 **Git installations** 区域
3. 通常已自动检测到 `git`，名称为 `Default`，Path 为 `git`
4. 如未检测到，点击 **Add Git**，填写：
   ```
   Name: git
   Path to Git executable: /usr/bin/git
   ```
5. 点击 **Save**

### 步骤 3.5：Docker 配置

无需在 Jenkins Tools 中额外配置 Docker，只要 Jenkins 用户能执行 `docker` 命令即可。

```bash
# 在 Jenkins 容器内验证
docker exec -it jenkins bash
docker ps
```

---

## 第四阶段：配置凭据

### 步骤 4.1：添加 Harbor 镜像仓库凭据

1. 点击 **Manage Jenkins > Credentials**
2. 在 **Stores scoped to Jenkins** 下，点击 **System**
3. 点击 **Global credentials (unrestricted)**
4. 点击左侧 **Add Credentials**
5. 填写：
   ```
   Kind: Username with password
   Scope: Global
   Username: admin
   Password: Hadoop@2022
   ID: harbor-admin
   Description: Harbor 镜像仓库管理员
   ```
6. 点击 **Create**

> 流水线引用：`docker.withRegistry("http://192.168.3.30:8633", 'harbor-admin')`

### 步骤 4.2：添加部署服务器 SSH 私钥

**前置操作：在 Jenkins 服务器生成 SSH 密钥对**

```bash
# SSH 登录到 Jenkins 服务器 192.168.3.10
docker exec -it jenkins bash

# 在容器内生成密钥对（如尚未生成）
ssh-keygen -t ed25519 -C "jenkins@carlos" -f /var/jenkins_home/.ssh/id_ed25519 -N ""

# 查看公钥
cat /var/jenkins_home/.ssh/id_ed25519.pub
# 复制输出的内容，例如：ssh-ed25519 AAAAC3NzaC... jenkins@carlos

# 退出 Jenkins 容器
exit
```

**将公钥部署到目标服务器（192.168.3.20）**

```bash
# 在 Jenkins 服务器上执行
ssh-copy-id -p 2022 -i /data/jenkins/home/.ssh/id_ed25519.pub carlos@192.168.3.20

# 测试免密登录
ssh -p 2022 -i /data/jenkins/home/.ssh/id_ed25519 carlos@192.168.3.20
# 预期结果：无需密码直接登录成功
```

**在 Jenkins 中添加 SSH 凭据**

1. 点击 **Manage Jenkins > Credentials > System > Global credentials**
2. 点击 **Add Credentials**
3. 填写：
   ```
   Kind: SSH Username with private key
   Scope: Global
   Username: carlos
   Private Key: [x] Enter directly
   Key: [点击 Add，粘贴私钥内容]
   ```

   私钥内容获取方式：
   ```bash
   # 在 Jenkins 服务器执行
   cat /data/jenkins/home/.ssh/id_ed25519
   # 复制全部内容（包括 -----BEGIN OPENSSH PRIVATE KEY----- 和 -----END...）
   ```

4. 继续填写：
   ```
   Passphrase: [留空]
   ID: carlos-ai-ssh
   Description: 部署服务器 192.168.3.20 SSH 私钥
   ```
5. 点击 **Create**

### 步骤 4.3：添加 GitLab 凭据

1. 点击 **Add Credentials**
2. 填写：
   ```
   Kind: Username with password
   Scope: Global
   Username: [GitLab 用户名]
   Password: [GitLab 密码 或 Personal Access Token]
   ID: gitlab-carlos
   Description: GitLab 访问凭据
   ```
3. 点击 **Create**

---

## 第五阶段：配置 Maven settings.xml

### 步骤 5.1：添加 Nexus 凭据

1. 点击 **Add Credentials**
2. 填写：
   ```
   Kind: Username with password
   Scope: Global
   Username: [admin
   Password: [hadoop543216
   ID: nexus-carlos
   Description:  Carlos Nexus 私服凭据
   ```
3. 点击 **Create**

### 步骤 5.2：创建 Managed File

1. 点击 **Manage Jenkins > Managed files**（Config File Provider 插件提供）
2. 点击 **Add a new Config**
3. 选择 **Global Maven settings.xml**，点击 **Next**
4. 填写：
   ```
   Name: carlos-maven-settings
   ID: carlos-maven-settings
   Comment: Carlos Framework Maven 全局配置
   ```
5. 在 **Content** 文本框中粘贴以下内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">

    <!-- 本地仓库路径（Jenkins 构建节点上） -->
    <localRepository>/var/jenkins_home/.m2/repository</localRepository>
    <!-- Jenkins 构建专用 settings.xml —— 仅 carlos 环境 -->
    <!-- 用法: mvn deploy -Pcarlos -->

    <!-- ===== 镜像配置 ===== -->
    <mirrors>
        <mirror>
            <id>carlos-http-mirror-pub</id>
            <mirrorOf>carlos-repo,carlos-releases,carlos-snapshots</mirrorOf>
            <name>Carlos HTTP Mirror Public</name>
            <url>http://192.168.3.10:8081/nexus/repository/maven-public/</url>
            <blocked>false</blocked>
        </mirror>
    </mirrors>

    <!-- ===== 私服账号 ===== -->
    <servers>
        <server>
            <id>carlos-repo</id>
            <!-- Jenkins 中通过 Credentials Binding 注入 -->
            <username>${env.NEXUS_USERNAME}</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
        <server>
            <id>carlos-releases</id>
            <username>${env.NEXUS_USERNAME}</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
        <server>
            <id>carlos-snapshots</id>
            <username>${env.NEXUS_USERNAME}</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
    </servers>

    <!-- ===== carlos 环境 Profile ===== -->
    <profiles>
        <profile>
            <id>carlos</id>
            <properties>
                <nexus.url>http://192.168.3.10:8081/nexus/repository</nexus.url>
                <altReleaseDeploymentRepository>
                    carlos-releases::default::http://192.168.3.10:8081/nexus/repository/maven-releases/
                </altReleaseDeploymentRepository>
                <altSnapshotDeploymentRepository>
                    carlos-snapshots::default::http://192.168.3.10:8081/nexus/repository/maven-snapshots/
                </altSnapshotDeploymentRepository>
            </properties>
            <repositories>
                <repository>
                    <id>carlos-repo</id>
                    <url>${nexus.url}/maven-public/</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>carlos-repo</id>
                    <url>${nexus.url}/maven-public/</url>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <!-- 默认激活 carlos profile -->
    <activeProfiles>
        <activeProfile>carlos</activeProfile>
    </activeProfiles>

</settings>
```

6. 点击 **Submit**

### 在流水线中使用

如需显式指定 settings.xml：

```groovy
stage('Maven构建') {
    steps {
        configFileProvider([configFile(fileId: 'carlos-maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh "mvn -B -s ${MAVEN_SETTINGS} -pl ${MODULE_PATH}/carlos-auth-cloud -am clean package ${skipTests}"
        }
    }
}
```

> 当前 Jenkinsfile 中未显式指定 settings.xml，依赖 Jenkins 全局默认配置。如需统一管控，建议修改流水线使用
`configFileProvider`。

---

## 第六阶段：创建流水线 Job

### 步骤 6.1：Job 清单

共需创建 **10 个 Pipeline Job**：

| #  | Job 名称                    | Jenkinsfile 路径                                  | 说明              | 触发方式           |
|----|---------------------------|-------------------------------------------------|-----------------|----------------|
| 1  | `carlos-framework-deploy` | `Jenkinsfile`（根目录）                              | 发布框架依赖到 Nexus   | 手动             |
| 2  | `carlos-base-java21`      | `deploy/jdk/Jenkinsfile`                        | 构建 Java 21 基础镜像 | 手动/基础镜像变更时     |
| 3  | `carlos-base-nginx`       | `deploy/nginx/Jenkinsfile`                      | 构建 Nginx 基础镜像   | 手动/基础镜像变更时     |
| 4  | `carlos-gateway`          | `carlos-integration/carlos-gateway/Jenkinsfile` | 网关服务            | 手动/GitLab Push |
| 5  | `carlos-auth`             | `carlos-integration/carlos-auth/Jenkinsfile`    | 认证服务            | 手动/GitLab Push |
| 6  | `carlos-system`           | `carlos-integration/carlos-system/Jenkinsfile`  | 系统服务            | 手动/GitLab Push |
| 7  | `carlos-org`              | `carlos-integration/carlos-org/Jenkinsfile`     | 组织服务            | 手动/GitLab Push |
| 8  | `carlos-message`          | `carlos-integration/carlos-message/Jenkinsfile` | 消息服务            | 手动/GitLab Push |
| 9  | `carlos-audit`            | `carlos-integration/carlos-audit/Jenkinsfile`   | 审计服务            | 手动/GitLab Push |
| 10 | `carlos-ui`               | `carlos-ui/Jenkinsfile`                         | 前端 UI           | 手动/GitLab Push |

### 步骤 6.2：创建根项目 Job（发布框架依赖）

1. 点击 Jenkins 首页左侧 **New Item**
2. 输入名称：`carlos-framework-deploy`
3. 选择 **Pipeline**，点击 **OK**
4. 在配置页面，**General** 区域：
   ```
   [勾选] Discard old builds
     Days to keep builds: 7
     Max # of builds to keep: 10
   [勾选] Do not allow concurrent builds
   ```
5. 滚动到页面底部 **Pipeline** 区域：
   ```
   Definition: Pipeline script from SCM
   SCM: Git
     Repositories:
       Repository URL: http://192.168.3.10/carlos/carlos-framework.git
       Credentials: gitlab-carlos
     Branches to build:
       Branch Specifier: */main
   Script Path: Jenkinsfile
   ```
6. 点击 **Save**

此 Job 用于发布框架 BOM 和 Starter 到 Nexus，**不构建 Docker 镜像**。

参数说明：

- `DEPLOY_TYPE`: `snapshot` 发布到 `maven-snapshots`，`release` 发布到 `maven-releases`
- `SKIP_TEST`: 默认跳过测试

### 步骤 6.3：创建基础镜像 Job

#### Job：carlos-base-java21

1. **New Item** > 名称：`carlos-base-java21` > **Pipeline** > **OK**
2. General：
   ```
   [勾选] Discard old builds (7 days, 10 builds)
   [勾选] Do not allow concurrent builds
   ```
3. Pipeline：
   ```
   Definition: Pipeline script from SCM
   SCM: Git
     Repository URL: http://192.168.3.10/carlos/carlos-framework.git
     Credentials: gitlab-carlos
     Branch Specifier: */main
   Script Path: deploy/jdk/Jenkinsfile
   ```
4. 点击 **Save**

#### Job：carlos-base-nginx

1. **New Item** > 名称：`carlos-base-nginx` > **Pipeline** > **OK**
2. Pipeline 配置：
   ```
   Script Path: deploy/nginx/Jenkinsfile
   ```
   其余同上。
3. 点击 **Save**

> 基础镜像变更频率低，建议手动触发。

### 步骤 6.4：创建后端服务 Job（以 carlos-gateway 为例）

1. **New Item** > 名称：`carlos-gateway` > **Pipeline** > **OK**
2. **General** 区域：
   ```
   [勾选] Discard old builds
     Days to keep builds: 7
     Max # of builds to keep: 10
   [勾选] Do not allow concurrent builds
   ```
3. **Build Triggers** 区域（可选，实现自动触发）：

   **方式 A - GitLab Webhook：**
   ```
   [勾选] Build when a change is pushed to GitLab
   [勾选] Push Events
   [勾选] Comments
   ```
   记下 **GitLab webhook URL**，例如：`http://192.168.3.10:8080/project/carlos-gateway`

   点击 **Advanced**，在 **Secret token** 处点击 **Generate**，复制生成的 Token。

   **方式 B - 仅手动触发：**
   ```
   [不勾选] 任何触发器
   ```

4. **Pipeline** 区域：
   ```
   Definition: Pipeline script from SCM
   SCM: Git
     Repository URL: http://192.168.3.10/carlos/carlos-framework.git
     Credentials: gitlab-carlos
     Branch Specifier: */main
   Script Path: carlos-integration/carlos-gateway/Jenkinsfile
   ```
5. 点击 **Save**

### 步骤 6.5：批量创建其余后端服务 Job

重复步骤 6.4，创建以下 Job（只需修改名称和 Script Path）：

| Job 名称         | Script Path                                     |
|----------------|-------------------------------------------------|
| carlos-auth    | `carlos-integration/carlos-auth/Jenkinsfile`    |
| carlos-system  | `carlos-integration/carlos-system/Jenkinsfile`  |
| carlos-org     | `carlos-integration/carlos-org/Jenkinsfile`     |
| carlos-message | `carlos-integration/carlos-message/Jenkinsfile` |
| carlos-audit   | `carlos-integration/carlos-audit/Jenkinsfile`   |

### 步骤 6.6：创建前端 Job

1. **New Item** > 名称：`carlos-ui` > **Pipeline** > **OK**
2. General 同步骤 6.4
3. Pipeline：
   ```
   Definition: Pipeline script from SCM
   SCM: Git
     Repository URL: http://192.168.3.10/carlos/carlos-framework.git
     Credentials: gitlab-carlos
     Branch Specifier: */main
   Script Path: carlos-ui/Jenkinsfile
   ```
4. 点击 **Save**

---

## 第七阶段：配置 GitLab Webhook（自动触发）

如果不需要自动触发，可跳过本节。所有 Job 均支持手动点击 **Build with Parameters** 运行。

### 步骤 7.1：为每个 Job 配置 Webhook

以 `carlos-gateway` 为例：

1. 登录 GitLab（http://192.168.3.10）
2. 进入项目 `carlos/carlos-framework`
3. 点击左侧菜单 **Settings > Webhooks**
4. 点击 **Add new webhook**
5. 填写：
   ```
   URL: http://192.168.3.10:8080/project/carlos-gateway
   Secret token: [步骤 6.4 中生成的 Token]
   Trigger:
     [x] Push events
     Branch filter pattern: main
   ```
6. 点击 **Add webhook**
7. 在 Webhooks 列表中，点击新添加的 webhook 旁的 **Test** > **Push events**
8. 预期结果：页面顶部显示 "Hook executed successfully"
9. 回到 Jenkins，进入 `carlos-gateway` Job，确认有新的构建被触发

### 步骤 7.2：批量配置其他服务

对每个后端服务和前端重复步骤 7.1，URL 中的 Job 名称对应替换：

| GitLab Webhook URL                                |
|---------------------------------------------------|
| `http://192.168.3.10:8080/project/carlos-auth`    |
| `http://192.168.3.10:8080/project/carlos-system`  |
| `http://192.168.3.10:8080/project/carlos-org`     |
| `http://192.168.3.10:8080/project/carlos-message` |
| `http://192.168.3.10:8080/project/carlos-audit`   |
| `http://192.168.3.10:8080/project/carlos-ui`      |

---

## 第八阶段：部署服务器准备

### 步骤 8.1：在 192.168.3.20 上准备部署目录

SSH 登录到部署服务器：

```bash
ssh -p 2022 carlos@192.168.3.20

# 创建部署目录
sudo mkdir -p /data/carlos
sudo chown -R carlos:carlos /data/carlos
cd /data/carlos

# 复制 docker-compose.yml（从代码仓库或手动创建）
# 方式 1：从 GitLab 拉取
git clone http://192.168.3.10/carlos/carlos-framework.git /tmp/carlos-framework
cp /tmp/carlos-framework/deploy/docker-compose.yml /data/carlos/
cp /tmp/carlos-framework/deploy/.env.example /data/carlos/.env
rm -rf /tmp/carlos-framework

# 方式 2：手动创建 docker-compose.yml 和 .env
# [参见项目 deploy/docker-compose.yml 和 deploy/.env.example]

# 编辑 .env 文件，确认配置正确
vim /data/carlos/.env
```

`.env` 文件内容示例：

```bash
NACOS_ADDR=192.168.3.10:8848
NACOS_NAMESPACE=carlos-prod
NACOS_GROUP=DEFAULT_GROUP
NACOS_USERNAME=nacos
NACOS_PASSWORD=Hadoop@2022

HARBOR_URL=192.168.3.30:8633
HARBOR_PROJECT=carlos
```

### 步骤 8.2：配置 Docker 登录 Harbor

```bash
# 在部署服务器上执行
docker login 192.168.3.30:8633 -u admin -p Hadoop@2022
# 预期输出：Login Succeeded

# 确保登录信息持久化（~/.docker/config.json）
cat ~/.docker/config.json
```

### 步骤 8.3：测试拉取基础镜像

```bash
# 测试从 Harbor 拉取基础镜像
docker pull 192.168.3.30:8633/carlos/carlos-base-java21:latest
docker pull 192.168.3.30:8633/carlos/carlos-base-nginx:latest
```

> 注：首次需要先运行 Jenkins 的 `carlos-base-java21` 和 `carlos-base-nginx` Job 构建并推送基础镜像。

---

## 第九阶段：首次构建验证

### 步骤 9.1：构建并推送基础镜像

**构建 Java 基础镜像：**

1. 进入 Jenkins，点击 `carlos-base-java21` Job
2. 点击左侧 **Build Now**
3. 点击底部构建记录（#1）
4. 点击 **Console Output**，观察日志：
    - `docker build` 成功
    - `docker push` 成功
    - 最后显示：`基础镜像构建并推送成功`

**构建 Nginx 基础镜像：**

1. 进入 `carlos-base-nginx` Job
2. 点击 **Build Now**
3. 确认 Console Output 成功

### 步骤 9.2：发布框架依赖到 Nexus

1. 进入 `carlos-framework-deploy` Job
2. 点击左侧 **Build with Parameters**
3. 选择参数：
   ```
   DEPLOY_TYPE: snapshot
   SKIP_TEST: [勾选]
   ```
4. 点击 **Build**
5. 等待完成，确认 Console Output 中显示发布成功
6. 访问 Nexus（http://192.168.3.30:8081）验证 artifacts 已上传

### 步骤 9.3：构建并部署网关服务

1. 进入 `carlos-gateway` Job
2. 点击 **Build with Parameters**
3. 选择参数：
   ```
   DEPLOY_ENV: 开发
   IMAGE_TAG: [留空]
   SKIP_TEST: [勾选]
   AUTO_DEPLOY: [勾选]
   ```
4. 点击 **Build**
5. 观察构建进度：
    - Stage View 显示 `Maven构建` > `构建Docker镜像` > `部署`
6. 构建完成后，SSH 登录部署服务器验证：
   ```bash
   ssh -p 2022 carlos@192.168.3.20
   docker ps | grep carlos-gateway
   # 预期输出包含 carlos-gateway 容器，状态为 Up

   # 测试健康检查
   curl http://localhost:9510/actuator/health
   # 预期返回 {"status":"UP"}
   ```

### 步骤 9.4：构建并部署前端

1. 进入 `carlos-ui` Job
2. 点击 **Build with Parameters**
3. 选择参数：
   ```
   DEPLOY_ENV: 开发
   IMAGE_TAG: [留空]
   AUTO_DEPLOY: [勾选]
   ```
4. 点击 **Build**
5. 等待构建完成（前端构建较慢，约 3-5 分钟）
6. 验证部署：
   ```bash
   ssh -p 2022 carlos@192.168.3.20
   docker ps | grep carlos-ui

   # 测试访问
   curl -I http://localhost:80
   # 预期返回 HTTP/1.1 200 OK
   ```

### 步骤 9.5：构建其余服务

依次对每个后端服务执行步骤 9.3：

1. `carlos-auth`
2. `carlos-system`
3. `carlos-org`
4. `carlos-message`
5. `carlos-audit`

全部完成后，在部署服务器上执行：

```bash
ssh -p 2022 carlos@192.168.3.20
cd /data/carlos
docker compose ps

# 预期看到所有 7 个服务均为 Up 状态
```

---

## 第十阶段：安全加固

### 步骤 10.1：启用 Role-Based 权限控制

1. 点击 **Manage Jenkins > Security**
2. 在 **Authorization** 区域，选择 **Role-Based Strategy**
3. 点击 **Save**
4. 点击 **Manage Jenkins > Manage and Assign Roles**
5. 点击 **Manage Roles** > **Add** > 填写 `developer`
6. 勾选 `developer` 角色的权限：
   ```
   [x] Overall - Read
   [x] Job - Build
   [x] Job - Cancel
   [x] Job - Read
   [x] Job - Workspace
   ```
7. 点击 **Save**

### 步骤 10.2：为生产部署增加审批

如需为生产环境增加人工审批，需修改对应服务的 Jenkinsfile，在 `部署` Stage 前添加：

```groovy
stage('生产环境审批') {
    when {
        expression { params.DEPLOY_ENV == '生产' }
    }
    steps {
        script {
            input message: '确认部署到生产环境？',
                  ok: '确认部署',
                  submitterParameter: 'APPROVER'
        }
    }
}
```

### 步骤 10.3：凭据安全

- [x] Harbor 密码使用 Jenkins 凭据管理，**严禁**硬编码在 Jenkinsfile 中
- [x] SSH 私钥使用 `SSH Username with private key` 类型
- [ ] 定期轮换 Harbor 和 SSH 密钥
- [ ] 考虑使用 Harbor 机器人账户代替 admin 账户

### 步骤 10.4：网络隔离

- Jenkins（192.168.3.10）需能访问：
    - GitLab（192.168.3.10，本地或同网段）
    - Harbor（192.168.3.30:8633）
    - Nexus（192.168.3.30:8081）
    - 部署服务器 SSH 端口（192.168.3.20:2022）

---

## 验证清单

完成全部配置后，对照以下清单逐项确认：

- [ ] Jenkins 可访问：http://192.168.3.10:8080
- [ ] 插件全部安装完成（无安装失败提示）
- [ ] 工具链配置完成（JDK、Maven、Node.js、Git）
- [ ] 凭据已添加（harbor-admin、carlos-ai-ssh、gitlab-carlos）
- [ ] Maven settings.xml Managed File 已创建
- [ ] 10 个 Pipeline Job 已创建
- [ ] SSH 免密登录测试通过：`ssh -p 2022 carlos@192.168.3.20`
- [ ] Harbor 登录测试通过：`docker login 192.168.3.30:8633`
- [ ] 基础镜像已推送至 Harbor
- [ ] 框架依赖已发布到 Nexus
- [ ] 至少一个服务构建并成功部署到 192.168.3.20
- [ ] `docker compose ps` 显示所有服务状态为 Up
- [ ] 服务健康检查端点返回 `{"status":"UP"}`

---

## 附录 A：系统全局配置

### A.1 系统配置（Configure System）

访问 **Manage Jenkins > System**。

#### Jenkins Location

```
Jenkins URL: http://192.168.3.10:8080/
System Admin e-mail address: jenkins@carlos.local
```

#### GitLab 集成（如使用 GitLab Webhook）

```
Connection name: carlos-gitlab
GitLab host URL: http://192.168.3.10/
Credentials: 选择 GitLab API Token（需提前创建）
```

### A.2 安全配置（Configure Global Security）

访问 **Manage Jenkins > Security**。

#### 授权策略

选择 **Role-Based Strategy**（需安装 role-strategy 插件）。

初始权限矩阵：

| 角色          | 权限         |
|-------------|------------|
| `admin`     | 全部权限       |
| `developer` | 构建、查看、取消构建 |
| `viewer`    | 只读         |

#### Agent 协议

关闭不安全的协议，仅保留 **Inbound TCP Agent Protocol/4**。

---

## 附录 B：Jenkins 节点配置

### B.1 当前架构

本项目所有 Job 使用 `agent any`，在 Jenkins Master 节点上直接构建。

### B.2 扩展：添加构建节点（推荐）

当构建负载增加时，建议添加专用构建节点：

```bash
# 在 192.168.3.10 上准备构建节点
# 确保节点已安装：JDK 21, Maven 3.8+, Docker, Git
```

**Jenkins 配置步骤：**

1. **Manage Jenkins > Nodes > New Node**
2. Node name: `build-agent-1`
3. 类型：Permanent Agent
4. 配置：
   ```
   Remote root directory: /data/jenkins-agent
   Labels: docker maven java21
   Usage: Only build jobs with label expressions matching this node
   Launch method: Launch agents via SSH
     Host: 192.168.3.10（或另一台构建机）
     Credentials: carlos-ai-ssh
   ```

**修改流水线使用指定节点：**

```groovy
pipeline {
    agent { label 'docker && maven && java21' }
    // ...
}
```

### B.3 部署服务器作为节点（可选）

如需在部署服务器上执行 Docker Compose 操作，可将其注册为 Jenkins 节点，替代 SSH 方式：

```groovy
stage('部署') {
    agent { label 'deploy-server' }
    steps {
        sh """
            cd /data/carlos
            docker compose up -d ${APP_NAME}
        """
    }
}
```

---

## 附录 C：流水线参数速查

### 后端服务流水线参数

| 参数名           | 类型      | 默认值  | 说明                       |
|---------------|---------|------|--------------------------|
| `DEPLOY_ENV`  | choice  | 开发   | 环境标签（dev/test/demo/prod） |
| `IMAGE_TAG`   | string  | 空    | 自定义镜像标签                  |
| `SKIP_TEST`   | boolean | true | 是否跳过测试                   |
| `AUTO_DEPLOY` | boolean | true | 构建后自动部署                  |

### 前端流水线参数

| 参数名           | 类型      | 默认值  | 说明      |
|---------------|---------|------|---------|
| `DEPLOY_ENV`  | choice  | 开发   | 环境标签    |
| `IMAGE_TAG`   | string  | 空    | 自定义镜像标签 |
| `AUTO_DEPLOY` | boolean | true | 构建后自动部署 |

### 框架发布流水线参数

| 参数名           | 类型      | 默认值      | 说明               |
|---------------|---------|----------|------------------|
| `DEPLOY_TYPE` | choice  | snapshot | snapshot/release |
| `SKIP_TEST`   | boolean | true     | 是否跳过测试           |

---

## 附录 D：常见问题排查

### Q1: `docker: permission denied`

**原因**：Jenkins 用户不在 docker 组。

**解决**：

```bash
# 宿主机执行
usermod -aG docker jenkins
# 或如果是 Docker 内运行 Jenkins
usermod -aG docker $(whoami)
```

### Q2: `sshagent` 步骤报错 `No such DSL method 'sshagent'`

**原因**：未安装 SSH Agent 插件。

**解决**：安装 `SSH Agent` 插件并重启 Jenkins。

### Q3: Maven 构建时找不到 `carlos-dependencies` 依赖

**原因**：本地仓库没有父 POM 或 BOM。

**解决**：

1. 先运行 `carlos-framework-deploy` Job 安装/发布依赖到本地仓库或 Nexus
2. 确保 `mvn -pl` 命令中 `-am`（also-make）参数正确，会自动构建依赖模块

### Q4: Harbor 推送失败 `unauthorized`

**原因**：凭据 ID 不匹配或凭据类型错误。

**解决**：

1. 检查凭据 ID 是否为 `harbor-admin`
2. 确保凭据类型是 `Username with password`
3. 验证 Harbor 地址和端口：`192.168.3.30:8633`

### Q5: 前端构建 `pnpm: command not found`

**原因**：NodeJS 插件未安装或未配置。

**解决**：

1. 安装 `NodeJS` 插件
2. 在 **Manage Jenkins > Tools** 中配置 Node.js 安装
3. 或在流水线中改用 `npm install -g pnpm`

### Q6: `sed -i` 在 Jenkins SSH 步骤中不生效

**原因**：部分 sed 版本需要指定备份后缀。

**解决**：将 `sed -i "s|...|...|"` 改为 `sed -i.bak "s|...|...|" && rm -f docker-compose.yml.bak`

---

## 附录 E：防火墙端口检查

确保以下端口在对应服务器上开放：

| 服务器          | 端口        | 服务        | 访问方           |
|--------------|-----------|-----------|---------------|
| 192.168.3.10 | 8080      | Jenkins   | 开发者           |
| 192.168.3.10 | 22/2022   | SSH       | Jenkins       |
| 192.168.3.20 | 2022      | SSH       | Jenkins       |
| 192.168.3.20 | 80        | carlos-ui | 用户            |
| 192.168.3.20 | 9501-9513 | 后端服务      | 网关/前端         |
| 192.168.3.30 | 8633      | Harbor    | Jenkins/部署服务器 |
| 192.168.3.30 | 8081      | Nexus     | Jenkins       |

---

## 附录 F：部署服务器目录结构

```
/data/carlos/
├── docker-compose.yml      # Docker Compose 配置文件
├── .env                    # 环境变量（从 .env.example 复制）
├── logs/                   # 容器日志挂载（可选）
└── backup/                 # 配置备份
```

---

> 文档版本: 1.1
> 适用项目: Carlos Framework v3.0.0-SNAPSHOT
> 最后更新: 2026-05-30
