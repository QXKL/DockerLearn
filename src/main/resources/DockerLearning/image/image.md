## docker image

镜像（Image）是容器的**只读模板**，包含运行应用所需的一切：代码、运行时、库、环境变量、配置文件。

### 镜像 vs 容器

| 概念 | 类比 | 特征 |
|------|------|------|
| **镜像** | 类（Class） | 只读、静态、可复用 |
| **容器** | 实例（Object） | 可写、动态、可运行 |

```
镜像 (Image)         容器 (Container)
    │                    │
    ▼                    ▼
  Dockerfile          docker run
    │                    │
    ▼                    ▼
  docker build        docker start/stop
    │                    │
    ▼                    ▼
  docker pull         docker exec
```

---

### 常用命令

```bash
# 查看本地镜像
docker images
docker image ls

# 拉取镜像
docker pull nginx
docker pull nginx:1.25
docker pull redis:alpine

# 删除镜像
docker rmi nginx
docker rmi -f nginx          # 强制删除

# 查看镜像详情
docker inspect nginx:latest

# 查看镜像历史（分层信息）
docker history nginx:latest

# 标记镜像（打标签）
docker tag nginx:latest my-nginx:v1

# 导出镜像
docker save nginx -o nginx.tar
docker save nginx redis -o images.tar

# 导入镜像
docker load -i nginx.tar

# 清理未使用的镜像
docker image prune           # 删除虚悬镜像
docker image prune -a        # 删除所有未使用镜像
```

---

### 镜像命名规则

```bash
# 完整格式
[仓库地址/]镜像名[:标签]

# 示例
nginx                    # Docker Hub 官方镜像，默认 latest
nginx:1.25              # 指定版本
bitnami/nginx           # 第三方用户
myregistry.com:5000/myapp:v1   # 私有仓库

# 查看镜像
docker images
# REPOSITORY    TAG       IMAGE ID       SIZE
# nginx         latest    abc123...      140MB
# nginx         1.25      def456...      140MB
# redis         alpine    ghi789...      32MB
```

---

### 镜像分层

每个镜像由多个**只读层**组成，分层复用节省空间。

```
nginx:latest 镜像分层结构
┌─────────────────┐
│  容器可写层       │  ← 容器运行时添加
├─────────────────┤
│  配置文件层       │  ← 镜像层（只读）
├─────────────────┤
│  nginx 程序层    │  ← 镜像层（只读）
├─────────────────┤
│  基础系统层       │  ← 镜像层（只读，如 alpine/ubuntu/debian）
└─────────────────┘
```

```bash
# 查看镜像分层
docker history nginx:alpine

# 每行的 IMAGE ID 对应一层
# 相同层在不同镜像间复用，不重复占用磁盘
```

---

### 镜像大小对比

```bash
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# 典型大小参考
# alpine          3.19     7MB     最小基础镜像
# nginx:alpine             23MB    
# redis:alpine             32MB
# nginx:slim               50MB
# python:3.12-slim         120MB
# ubuntu:22.04             77MB
# node:20-alpine           170MB
# mysql:8.0                560MB
# nginx:latest             140MB
```

---

### 常用基础镜像选择

| 镜像 | 大小 | 包管理器 | Shell | 适用场景 |
|------|------|---------|-------|---------|
| alpine | ~7MB | apk | sh | 最简环境，生产推荐 |
| debian:slim | ~80MB | apt | bash | 需要 glibc |
| ubuntu | ~77MB | apt | bash | 通用开发 |
| centos | ~200MB | yum | bash | 旧项目兼容 |

```bash
# Alpine 示例
docker run -it alpine sh
apk add curl vim

# Ubuntu 示例
docker run -it ubuntu bash
apt-get update && apt-get install curl vim
```

---

### 镜像相关操作

#### 拉取与运行

```bash
# 直接运行（自动拉取）
docker run -d nginx

# 先拉取再运行
docker pull nginx:alpine
docker run -d nginx:alpine

# 拉取特定平台（在 M1 Mac 上拉取 amd64）
docker pull --platform linux/amd64 mysql:5.7
```

#### 删除与清理

```bash
# 删除特定镜像
docker rmi nginx:1.25

# 强制删除（有容器使用也会删）
docker rmi -f nginx

# 删除虚悬镜像（<none>:<none>）
docker image prune

# 删除所有未使用的镜像
docker image prune -a

# 删除所有镜像（危险！）
docker rmi -f $(docker images -q)
```

#### 导入导出

```bash
# 导出到文件
docker save nginx:alpine -o nginx.tar
docker save nginx redis -o images.tar

# 压缩导出
docker save nginx:alpine | gzip > nginx.tar.gz

# 导入
docker load -i nginx.tar
docker load < nginx.tar

# 压缩导入
gunzip -c nginx.tar.gz | docker load
```

#### 标签与推送

```bash
# 打标签
docker tag nginx:alpine myregistry.com/my-nginx:v1

# 推送到仓库
docker push myregistry.com/my-nginx:v1

# 登录私有仓库
docker login myregistry.com
docker login -u username -p password
```

---

### 镜像仓库

| 仓库 | 地址 | 说明 |
|------|------|------|
| Docker Hub | docker.io | 默认，官方镜像 |
| 阿里云 | registry.cn-hangzhou.aliyuncs.com | 国内快 |
| 腾讯云 | ccr.ccs.tencentyun.com | |
| Google GCR | gcr.io | |
| GitHub GHCR | ghcr.io | |

```bash
# 配置镜像加速（/etc/docker/daemon.json）
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ]
}

# 重启生效
systemctl restart docker
```

---

### docker-compose 中的镜像

```yaml
# docker-compose.yml
services:
  # 使用现有镜像
  web:
    image: nginx:alpine
    container_name: my-nginx

  # 构建镜像
  app:
    build: ./app           # 从 Dockerfile 构建
    image: myapp:1.0       # 指定构建后的镜像名

  # 总是拉取最新镜像
  db:
    image: mysql:8.0
    pull_policy: always    # always/missing/never/build

  # 私有仓库
  private:
    image: myregistry.com/myapp:v1
```

---

### 查看镜像信息

```bash
# 基本列表
docker images
docker images -a           # 包含中间层

# 过滤
docker images --filter "reference=nginx:*"
docker images --filter "label=maintainer=me"

# 格式化输出
docker images --format "table {{.Repository}}\t{{.Size}}"

# 详细信息
docker inspect nginx:alpine
docker inspect --format='{{.Architecture}}' nginx:alpine

# 查看镜像创建时间
docker inspect --format='{{.Created}}' nginx:alpine
```

---

### 常见问题

```bash
# Q1: 拉取失败 "no matching manifest"
# 架构不匹配
docker pull --platform linux/amd64 mysql

# Q2: 镜像占用空间太大
docker system df           # 查看占用
docker system prune -a     # 清理

# Q3: 删除镜像报错 "image is being used"
docker ps -a --filter "ancestor=nginx"  # 找到使用该镜像的容器
docker rm 容器名
docker rmi nginx

# Q4: 如何查看镜像的标签列表
# 去 Docker Hub 网站查看，或使用 skopeo 等工具
```