## Dockerfile

Dockerfile 是一个**文本文件**，包含构建镜像所需的指令。通过 `docker build` 可以将其打包成镜像。

### 为什么需要 Dockerfile

```bash
# 之前：只能运行现成镜像
docker run nginx
docker run redis

# 之后：打包自己的应用
docker build -t myapp .
docker run myapp
```

---

### 一个完整的 Dockerfile 示例

```dockerfile
# 1. 基础镜像（必须）
FROM openjdk:17-jdk-slim

# 2. 元数据（作者）
LABEL maintainer="your-email@example.com"
LABEL version="1.0"

# 3. 设置工作目录
WORKDIR /app

# 4. 复制文件（先复制依赖，利用缓存）
COPY pom.xml .
COPY src ./src

# 5. 执行构建命令
RUN apt-get update && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# 6. 暴露端口（文档作用，不实际映射）
EXPOSE 8080

# 7. 设置环境变量
ENV APP_ENV=production \
    JAVA_OPTS="-Xms256m -Xmx512m"

# 8. 定义用户（非 root 运行）
RUN useradd -m appuser
USER appuser

# 9. 启动命令
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]
```

---

### 常用指令速查

| 指令            | 用途        | 示例                                         |
|---------------|-----------|--------------------------------------------|
| `FROM`        | 指定基础镜像    | `FROM alpine:3.19`                         |
| `WORKDIR`     | 设置工作目录    | `WORKDIR /app`                             |
| `COPY`        | 复制文件（推荐）  | `COPY . /app`                              |
| `ADD`         | 复制 + 自动解压 | `ADD app.tar.gz /app`                      |
| `RUN`         | 构建时执行命令   | `RUN apk add curl`                         |
| `ENV`         | 设置环境变量    | `ENV NODE_ENV=production`                  |
| `ARG`         | 构建参数      | `ARG VERSION=1.0`                          |
| `EXPOSE`      | 声明端口（文档）  | `EXPOSE 8080`                              |
| `VOLUME`      | 声明挂载点     | `VOLUME /data`                             |
| `CMD`         | 默认命令（可覆盖） | `CMD ["node", "app.js"]`                   |
| `ENTRYPOINT`  | 入口命令（难覆盖） | `ENTRYPOINT ["java", "-jar"]`              |
| `USER`        | 切换用户      | `USER appuser`                             |
| `LABEL`       | 添加元数据     | `LABEL version="1.0"`                      |
| `HEALTHCHECK` | 健康检查      | `HEALTHCHECK CMD curl -f http://localhost` |

---

### COPY vs ADD

```dockerfile
# COPY（推荐）：只复制文件
COPY ./target/app.jar /app/app.jar

# ADD：复制 + 自动解压（适合 tar.gz）
ADD app.tar.gz /app/

# ✅ 推荐：一般用 COPY，需要解压才用 ADD
```

---

### CMD vs ENTRYPOINT

```dockerfile
# CMD：可被覆盖
FROM ubuntu
CMD ["echo", "hello"]
# docker run test           → hello
# docker run test echo bye  → bye（覆盖了）

# ENTRYPOINT：难覆盖
FROM ubuntu
ENTRYPOINT ["echo", "hello"]
# docker run test           → hello
# docker run test bye       → hello bye（参数追加）

# 组合使用（常见模式）
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]
# docker run myapp           → java -jar app.jar
# docker run myapp other.jar → java -jar other.jar
```

---

### 分层构建与缓存优化

```dockerfile
# ❌ 不好：每次构建都要重新下载依赖
FROM node:18
COPY . /app
RUN npm install
CMD ["node", "app.js"]

# ✅ 好：先复制依赖文件，利用缓存
FROM node:18
WORKDIR /app
COPY package*.json ./
RUN npm install           # 只有 package.json 变化才重装
COPY . .
CMD ["node", "app.js"]

# ✅ Java 项目同理
FROM maven:3.8-openjdk-17
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline   # 下载依赖(缓存)
COPY src ./src
RUN mvn package
CMD ["java", "-jar", "target/app.jar"]
```

---

### 多阶段构建（减小镜像体积）

```dockerfile
# ❌ 单个阶段：包含构建工具，镜像很大
FROM maven:3.8-openjdk-17
COPY . .
RUN mvn package
CMD ["java", "-jar", "target/app.jar"]
# 镜像大小：~600MB

# ✅ 多阶段：最终镜像只包含运行时
# 阶段1：构建（使用 Maven）
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

# 阶段2：运行（只复制 jar 包）
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
# 最终镜像大小：~200MB（减小 400MB）
```

---

### 实战：Spring Boot 项目 Dockerfile

```dockerfile
# 多阶段构建 Spring Boot 应用
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制依赖文件（利用缓存）
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制源码并打包
COPY src ./src
RUN mvn package -DskipTests

# 运行时镜像
FROM eclipse-temurin:17-jre-alpine

# 安装必要工具（可选）
RUN apk add --no-cache curl

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /build/target/*.jar app.jar

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]
```

---

### 实战：前端项目 Dockerfile

```dockerfile
# 多阶段构建 React/Vue 应用
FROM node:18-alpine AS builder

WORKDIR /build

# 安装依赖
COPY package*.json ./
RUN npm ci --only=production

# 复制源码并构建
COPY . .
RUN npm run build

# 运行阶段（使用 nginx）
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制自定义 nginx 配置（可选）
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

---

### 常用优化技巧

```dockerfile
# 1. 合并 RUN 命令（减少层数）
# ❌ 不好：3 层
RUN apt-get update
RUN apt-get install -y curl
RUN rm -rf /var/lib/apt/lists/*

# ✅ 好：1 层
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# 2. 使用 .dockerignore（排除不需要的文件）
# .dockerignore 内容：
# .git
# node_modules
# .idea
# *.log
# Dockerfile
# .dockerignore

# 3. 选择合适的基础镜像
# alpine（~5MB）vs ubuntu（~70MB）vs debian（~120MB）

# 4. 固定版本号（不要用 latest）
FROM node:18-alpine    # ✅
FROM node:latest       # ❌

# 5. 清理临时文件
RUN apk add --no-cache curl   # alpine 的 --no-cache 不保留缓存
```

---

### 常用基础镜像选择

| 场景          | 推荐镜像                            | 大小         |
|-------------|---------------------------------|------------|
| Java        | `eclipse-temurin:17-jre-alpine` | ~120MB     |
| Java（构建）    | `maven:3.9-eclipse-temurin-17`  | ~400MB     |
| Node.js     | `node:18-alpine`                | ~170MB     |
| Node.js（构建） | `node:18`                       | ~1GB       |
| Python      | `python:3.12-slim`              | ~120MB     |
| Go          | `golang:1.21-alpine`            | ~300MB（构建） |
| Go（运行）      | `alpine:3.19`                   | ~7MB       |
| Rust        | `rust:1.74-alpine`              | ~500MB（构建） |
| Rust（运行）    | `alpine:3.19`                   | ~7MB       |
| 前端（运行）      | `nginx:alpine`                  | ~23MB      |

---

### 构建命令

```bash
# 基本构建
docker build -t myapp:1.0 .

# 指定 Dockerfile
docker build -f Dockerfile.prod -t myapp:prod .

# 带构建参数
docker build --build-arg VERSION=2.0 -t myapp:2.0 .

# 不使用缓存
docker build --no-cache -t myapp:latest .

# 查看构建过程
docker build --progress=plain -t myapp .

# 构建时指定平台
docker build --platform linux/amd64 -t myapp:amd64 .
```

---

### docker-compose 中的构建

```yaml
# docker-compose.yml
services:
  app:
    build:
      context: .                    # 构建上下文
      dockerfile: Dockerfile        # Dockerfile 路径
      args:                         # 构建参数
        VERSION: 1.0
      target: builder               # 多阶段构建的目标阶段
      cache_from:                   # 缓存来源
        - myapp:latest
        - myapp:1.0
    image: myapp:1.0                # 构建后的镜像名
    container_name: myapp
    ports:
      - "8080:8080"

  db:
    image: mysql:8
    # 不构建，直接使用镜像

# 构建命令
# docker-compose build
# docker-compose build --no-cache app
# docker-compose up -d --build    # 自动重新构建
```

---

### 常见问题

```bash
# Q1: 构建慢怎么办？
# - 利用缓存（先复制依赖文件）
# - 使用国内镜像源
# - 多阶段构建

# Q2: 镜像太大怎么办？
# - 使用 alpine/slim 基础镜像
# - 多阶段构建
# - 清理临时文件

# Q3: 构建时网络问题（apt/npm 慢）
# Dockerfile 中换源
RUN sed -i 's/deb.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list
RUN npm config set registry https://registry.npmmirror.com

# Q4: 权限问题（不能以 root 运行）
RUN useradd -m appuser
USER appuser

# Q5: 时区问题
RUN apk add tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
ENV TZ=Asia/Shanghai
```

---

### 最佳实践总结

```dockerfile
# ✅ 推荐模板
FROM 基础镜像:固定版本

# 设置维护者
LABEL maintainer="email@example.com"

# 设置工作目录
WORKDIR /app

# 先复制依赖文件（利用缓存）
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源码
COPY . .

# 创建非 root 用户
RUN adduser -D appuser
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK CMD curl -f http://localhost:8080/health

# 启动命令
CMD ["node", "app.js"]
```