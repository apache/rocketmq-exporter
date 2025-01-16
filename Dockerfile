# 使用官方的Java镜像作为基础镜像
FROM openjdk:11-jre-slim

# 设置工作目录
WORKDIR /app

# 复制配置文件
COPY application.properties /app/application.properties

# 将编译好的jar包复制到工作目录
COPY target/rocketmq-exporter-*.jar /app/rocketmq-exporter.jar

# 暴露默认端口
EXPOSE 5557

# 启动RocketMQ-Exporter
CMD ["java", "-jar", "rocketmq-exporter.jar"]
