#!/bin/bash
# =====================================================
# YuzhonBlog 生产环境启动脚本
# 针对 4核4G 服务器优化（预留空间给 Astro + MySQL + OS）
# =====================================================

JAR_NAME="YuzhonBlog.jar"
PROFILE="prod"

# JVM 内存配置
# 堆内存: 初始512M，最大1536M（约1.5G，留2.5G给系统+Astro+MySQL）
# 元空间: 最大256M（Spring Boot + Hibernate 类加载需要）
# GC: G1 收集器，适合中等堆大小，低延迟
JAVA_OPTS="
  -Xms512m
  -Xmx1536m
  -XX:MaxMetaspaceSize=256m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -Djava.security.egd=file:/dev/./urandom
"

# 环境变量（按需修改）
export SPRING_PROFILES_ACTIVE=${PROFILE}
export DB_HOST=${DB_HOST:-localhost}
export DB_PORT=${DB_PORT:-3306}
export DB_NAME=${DB_NAME:-yuzhong_blog}
export DB_USERNAME=${DB_USERNAME:-root}
export DB_PASSWORD=${DB_PASSWORD:-}
export JWT_SECRET=${JWT_SECRET:-}
export APP_ENCRYPTION_SECRET=${APP_ENCRYPTION_SECRET:-}

echo "Starting YuzhonBlog with profile: $PROFILE"
echo "JVM Heap: 512M ~ 1536M"
echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"

nohup java $JAVA_OPTS -jar "$JAR_NAME" \
  --spring.profiles.active=$PROFILE \
  > app.log 2>&1 &

echo "PID: $!"
echo "Logs: tail -f app.log"
