# ============================================
# 基于 carlos-base-java21 预构建基础镜像
# 基础镜像已包含: 字体、时区、常用工具、JDK21
# ============================================
FROM 192.168.3.30:8633/carlos/carlos-base-java21:latest

ARG PROJECT_NAME

ENV PORT=8080
ENV JAVA_OPTS="-Xmx1024m -Xms512m -Djava.security.egd=file:/dev/./urandom"
ENV PROJECT_NAME=${PROJECT_NAME}

COPY ./target/${PROJECT_NAME}.jar ./${PROJECT_NAME}.jar

CMD java -server ${JAVA_OPTS} -Dserver.port=${PORT} -jar ${PROJECT_NAME}.jar
