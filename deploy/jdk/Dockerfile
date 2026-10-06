# ============================================
# Carlos Framework 后端基础镜像
# 用途: 统一后端服务运行环境，预装字体、时区、常用工具
# 构建后推送至: 192.168.3.30:8633/carlos/base/carlos-eclipse-temurin:21-jre-alpine
# ============================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="carlos" \
      description="Carlos Framework Base Image (JDK21 + Fonts + Utils)"

# 换源、安装工具与字体、清理缓存
# font-wqy-zenhei: 轻量中文字体 (~15MB)，替代体积巨大的 font-noto-cjk (~200MB)
# 如需更高质量字体，可替换为 font-noto-cjk
RUN sed -i 's|https\?://[^/]*/alpine|https://mirrors.aliyun.com/alpine|g' /etc/apk/repositories \
    && apk add --no-cache \
        bash \
        busybox-extras \
        ca-certificates \
        curl \
        wget \
        netcat-openbsd \
        net-tools \
        iputils \
        procps \
        tzdata \
        ttf-dejavu \
        font-wqy-zenhei \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata \
    && rm -rf /var/cache/apk/* /tmp/*

# 设置时区环境变量（可被覆盖）
ENV TZ=Asia/Shanghai
ENV LANG=C.UTF-8
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Shanghai"

# 统一工作目录
RUN mkdir -p /jar
WORKDIR /jar

# 容器内部统一端口
EXPOSE 8080

# 默认健康检查（各服务 Dockerfile 中可覆盖）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# 默认启动命令（各服务 Dockerfile 中覆盖）
CMD ["java", "-version"]
