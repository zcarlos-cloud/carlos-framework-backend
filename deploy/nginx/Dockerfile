# ============================================
# Carlos Framework Nginx 基础镜像
# 用途: 统一前端服务运行环境，预装字体、时区、常用工具
# 构建后推送至: 192.168.3.30:8633/carlos/base/carlos-nginx:alpine
# ============================================
FROM nginx:alpine

LABEL maintainer="carlos" \
      description="Carlos Framework Nginx Base Image (Fonts + Utils)"

# 安装常用工具、字体、时区数据
# font-wqy-zenhei: 轻量中文字体 (~15MB)，替代体积巨大的 font-noto-cjk (~200MB)
RUN sed -i 's|https\?://[^/]*/alpine|https://mirrors.aliyun.com/alpine|g' /etc/apk/repositories \
    && apk add --no-cache \
        bash \
        curl \
        wget \
        netcat-openbsd \
        tzdata \
        ttf-dejavu \
        font-wqy-zenhei \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata \
    && rm -rf /var/cache/apk/* /tmp/*

ENV TZ=Asia/Shanghai
ENV LANG=C.UTF-8

EXPOSE 80

# 默认健康检查（各服务 Dockerfile 中可覆盖）
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

# 不重复声明 CMD，继承 nginx:alpine 的默认启动命令
