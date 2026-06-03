## docker pull

`docker pull` 用于**从镜像仓库拉取镜像到本地**。如果不指定仓库，默认从 Docker Hub 拉取。

### 基本语法

```bash
docker pull [选项] 镜像名[:标签] [仓库地址/]镜像名[:标签]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-a` | 拉取仓库中的所有标签（所有版本） | `docker pull -a ubuntu` |
| `-q` | 静默模式，不显示下载进度 | `docker pull -q nginx` |
| `--platform` | 指定平台（linux/amd64, linux/arm64） | `docker pull --platform=linux/arm64 nginx` |

### 镜像命名规则

```bash
# 完整格式
[仓库地址/]镜像名[:标签]@摘要

# 常见格式示例
docker pull nginx                    # Docker Hub 官方镜像，默认 latest 标签
docker pull nginx:1.25              # 指定版本标签
docker pull ubuntu:22.04            # Ubuntu 22.04
docker pull alpine:3.19             # Alpine Linux 3.19

# 第三方镜像（用户名/镜像名）
docker pull bitnami/nginx           # Bitnami 的 nginx
docker pull linuxserver/qbittorrent # LinuxServer 的 qbittorrent

# 私有仓库
docker pull myregistry.com:5000/myapp:1.0
docker pull registry.example.com/project/app:latest

# 通过摘要拉取（不可变）
docker pull nginx@sha256:abc123...
```

### 实战示例

```bash
# 1. 拉取最新版 nginx
docker pull nginx

# 2. 拉取指定版本
docker pull nginx:1.25
docker pull nginx:alpine

# 3. 拉取多个镜像
docker pull nginx redis mysql postgres

# 4. 静默拉取（不显示进度条）
docker pull -q alpine

# 5. 拉取特定平台镜像（在 ARM 机器上拉取 AMD64 镜像）
docker pull --platform=linux/amd64 nginx

# 6. 拉取所有标签（不推荐，可能很大）
docker pull -a ubuntu  # 拉取所有 Ubuntu 版本
```

### 镜像仓库说明

```bash
# Docker Hub（默认）
docker pull nginx                    # = docker pull docker.io/nginx
docker pull library/nginx            # library 是官方镜像前缀

# 阿里云容器镜像（国内常用）
docker pull registry.cn-hangzhou.aliyuncs.com/aliyun_containers/nginx

# Google Container Registry
docker pull gcr.io/google-containers/nginx

# GitHub Container Registry
docker pull ghcr.io/username/repo:tag

# 腾讯云容器镜像
docker pull ccr.ccs.tencentyun.com/namespace/repo:tag
```

### 拉取 vs 运行

```bash
# 方式1：先拉取再运行
docker pull nginx
docker run -d --name my-nginx nginx

# 方式2：直接运行（自动拉取）
docker run -d --name my-nginx nginx  # 本地没有时会自动 pull

# 查看本地已有镜像
docker images

# 查看镜像详细信息
docker inspect nginx:latest
```

### 镜像层和缓存

```bash
# 拉取过程显示层级
docker pull nginx
# Using default tag: latest
# latest: Pulling from library/nginx
# a2abf6c4d29d: Pull complete      # 每一层
# a9edb18cadd1: Pull complete
# 589b7251471a: Pull complete
# ...

# 已有层会使用缓存
docker pull nginx:latest
# a2abf6c4d29d: Already exists     # 已存在，跳过下载
```

### 实际场景

#### 场景1：确保使用最新镜像
```bash
# 删除旧镜像并拉取最新
docker rmi nginx:latest
docker pull nginx:latest

# 或强制拉取（覆盖本地缓存）
docker pull --no-cache nginx  # 注意：--no-cache 不是 pull 的参数
# 实际做法：
docker system prune -f         # 清理缓存
docker pull nginx
```

#### 场景2：多架构镜像
```bash
# Docker Hub 的官方镜像支持多架构
docker pull nginx  # 自动适配当前系统架构

# 查看镜像支持的架构
docker manifest inspect nginx

# 在 Mac M1/M2 上拉取 AMD64 镜像（用于兼容）
docker pull --platform=linux/amd64 mysql:5.7
```

#### 场景3：代理加速（国内用户）
```bash
# 配置 Docker 镜像加速器
# /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}

# 重启 Docker
systemctl restart docker

# 现在拉取镜像会自动使用镜像加速
docker pull nginx
```

#### 场景4：批量拉取
```bash
# 从文件列表批量拉取
cat images.txt | while read img; do
  docker pull $img
done

# images.txt 内容：
# nginx:1.25
# redis:7.0
# mysql:8.0
# postgres:15

# 并行拉取（谨慎使用）
cat images.txt | xargs -P 4 -I {} docker pull {}
```

### docker-compose 对照

```yaml
# docker-compose.yml
services:
  web:
    image: nginx:1.25
    # 启动前会检查本地是否有镜像，没有则自动 pull
  
  db:
    image: postgres:15
    pull_policy: always      # 总是拉取最新镜像（Compose v2.11+）
    # pull_policy 可选值：
    # always      - 总是拉取
    # never       - 从不拉取（本地不存在则失败）
    # missing     - 本地缺失时拉取（默认）
    # build       - 构建镜像

  app:
    build: ./app
    image: myapp:latest
    pull_policy: never       # 使用本地构建的镜像
```

```bash
# docker-compose 拉取命令
docker-compose pull           # 拉取所有服务镜像
docker-compose pull web db    # 拉取指定服务
docker-compose pull --ignore-pull-failures  # 忽略拉取失败
docker-compose pull --parallel               # 并行拉取
```

### 镜像管理

```bash
# 查看本地镜像
docker images
docker image ls

# 查看镜像大小
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# 删除镜像
docker rmi nginx:1.25
docker rmi -f nginx           # 强制删除（即使有容器在使用）

# 清理未使用的镜像
docker image prune            # 删除虚悬镜像
docker image prune -a         # 删除所有未使用的镜像

# 保存和加载镜像
docker save nginx -o nginx.tar
docker load -i nginx.tar

# 查看镜像历史
docker history nginx:latest
```

### 常见问题

```bash
# Q1: 拉取太慢怎么办？
# 使用国内镜像加速器（配置 daemon.json）
# 或使用代理
export HTTP_PROXY=http://proxy:8080
docker pull nginx

# Q2: 拉取失败 "no matching manifest"
# 检查平台架构
docker pull --platform=linux/amd64 nginx

# Q3: 空间不足
docker system df              # 查看磁盘使用
docker system prune -a        # 清理未使用资源

# Q4: 如何拉取私有仓库镜像
docker login myregistry.com   # 先登录
docker pull myregistry.com/myapp:1.0

# Q5: 验证镜像完整性
docker pull nginx@sha256:具体摘要值
```

### 标签说明

```bash
# 常用标签约定
latest     # 最新稳定版（会更新）
alpine     # 基于 Alpine Linux（体积小）
slim       # 精简版（去除不必要工具）
buster     # Debian Buster 基础镜像
bullseye   # Debian Bullseye 基础镜像

# 选择合适标签
docker pull nginx:alpine        # 生产推荐（~23MB）
docker pull nginx:slim          # 中等大小（~50MB）
docker pull nginx:latest        # 完整版（~140MB）

# 查看镜像所有可用标签（需要第三方工具）
# 或访问 Docker Hub 网站查看
```