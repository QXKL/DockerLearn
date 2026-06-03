## docker network

容器网络（Network）用于**容器间通信**以及**容器与外部通信**。Docker 自带 DNS，容器名自动成为域名。

### 为什么需要自定义网络

```bash
# 问题：默认 bridge 网络不支持 DNS 解析
docker run -d --name app1 alpine sleep 3600
docker run -it --name app2 alpine sh
ping app1  # ❌ ping: bad address 'app1'

# 解决：创建自定义网络
docker network create mynet
docker run -d --name app1 --network mynet alpine sleep 3600
docker run -it --name app2 --network mynet alpine sh
ping app1  # ✅ 成功！容器名自动解析
```

---

### 网络驱动类型

| 驱动 | 说明 | 适用场景 |
|------|------|---------|
| `bridge` | 默认，容器间隔离 | 单机容器通信 |
| `host` | 直接使用宿主机网络 | 性能要求高 |
| `none` | 无网络 | 安全隔离 |
| `overlay` | 跨多台宿主机 | Swarm / K8s |
| `macvlan` | 分配 MAC 地址 | 需要物理网络直连 |

```bash
# 查看网络
docker network ls
# NETWORK ID     NAME      DRIVER    SCOPE
# abc123         bridge    bridge    local
# def456         host      host      local
# ghi789         none      null      local
```

---

### 常用命令

```bash
# 查看网络
docker network ls
docker network inspect bridge

# 创建网络
docker network create mynet
docker network create --driver bridge --subnet 172.20.0.0/16 mynet

# 连接/断开容器
docker network connect mynet 容器名
docker network disconnect mynet 容器名

# 删除网络
docker network rm mynet
docker network prune      # 删除未使用的网络
```

---

### 网络实战

#### 创建并使用自定义网络

```bash
# 1. 创建网络
docker network create learn-net

# 2. 启动容器加入网络
docker run -d --name rabbitmq --network learn-net rabbitmq:3-management
docker run -d --name redis --network learn-net redis
docker run -d --name mysql --network learn-net -e MYSQL_ROOT_PASSWORD=123456 mysql:8

# 3. 测试通信
docker run --rm --network learn-net alpine sh -c "ping rabbitmq"
# ✅ 64 bytes from 172.18.0.2: seq=0 ttl=64 time=0.1ms

# 4. 查看网络详情
docker network inspect learn-net
# "Containers": {
#   "rabbitmq": { "IPv4Address": "172.18.0.2" },
#   "redis":    { "IPv4Address": "172.18.0.3" },
#   "mysql":    { "IPv4Address": "172.18.0.4" }
# }
```

#### 容器名即域名

```bash
# Spring Boot 配置示例
spring:
  rabbitmq:
    host: rabbitmq    # 容器名，不是 localhost
    port: 5672
  redis:
    host: redis
    port: 6379
  datasource:
    url: jdbc:mysql://mysql:3306/mydb
    username: root
    password: 123456
```

#### 多网络连接（一个容器可连多个网络）

```bash
# 创建两个网络
docker network create frontend
docker network create backend

# 容器同时连接两个网络
docker run -d --name api --network frontend nginx
docker network connect backend api

# 验证
docker inspect api
# "Networks": {
#   "frontend": { "IPAMConfig": {}, ... },
#   "backend":  { "IPAMConfig": {}, ... }
# }

# 作用：api 可以同时与 frontend 和 backend 网络的容器通信
```

---

### 四种网络驱动详解

#### 1. bridge（默认）

```bash
# Docker 默认创建的 bridge 网络
docker network inspect bridge

# 容器默认使用 bridge
docker run -d --name web nginx

# 缺点：不支持容器名 DNS 解析，需要用 --link（已废弃）
docker run -d --name web2 --link web nginx  # 不推荐

# 自定义 bridge 才支持 DNS
docker network create my-bridge
docker run -d --name web --network my-bridge nginx
docker run -it --network my-bridge alpine ping web  # ✅
```

#### 2. host

```bash
# 容器直接使用宿主机网络栈，无网络隔离
docker run -d --network host nginx
# 访问 http://localhost:80 直接访问容器

# 特点：
# - 性能最好（无 NAT）
# - 端口冲突（容器端口=宿主机端口）
# - 不支持端口映射（-p 无效）

# 适用：对网络性能要求高的场景
```

#### 3. none

```bash
# 完全无网络
docker run -it --network none alpine sh
ifconfig  # 只有 lo

# 适用：不需要网络的容器（如离线计算）
```

#### 4. overlay

```bash
# 需要 Swarm 模式，用于多机通信
docker swarm init
docker network create -d overlay my-overlay
docker service create --network my-overlay --name web nginx
```

---

### 端口映射（外部访问容器）

```bash
# 基本端口映射
docker run -d -p 8080:80 nginx    # 宿主机8080 → 容器80
docker run -d -p 80:80 nginx      # 直接使用80端口

# 指定 IP（多网卡情况）
docker run -d -p 127.0.0.1:8080:80 nginx   # 只监听本地

# 随机端口
docker run -d -P nginx             # 大写 P，随机映射
docker port 容器名                  # 查看随机端口

# 多个端口
docker run -d \
  -p 8080:80 \
  -p 8443:443 \
  -p 27017:27017 \
  --name web \
  nginx

# UDP 端口
docker run -d -p 53:53/udp --name dns ubuntu

# 查看端口映射
docker port web
# 80/tcp -> 0.0.0.0:8080
# 443/tcp -> 0.0.0.0:8443
```

---

### 容器间通信方式对比

| 方式 | 命令 | DNS | 跨主机 | 推荐度 |
|------|------|-----|--------|--------|
| 自定义网络 | `--network mynet` | ✅ | ❌ | ⭐⭐⭐⭐⭐ |
| 默认 bridge | 默认 | ❌ | ❌ | ⭐ |
| --link | `--link 容器名` | ✅ | ❌ | ❌（已废弃） |
| host | `--network host` | - | ❌ | ⭐⭐⭐（特定场景） |
| 端口映射 | `-p 8080:80` | - | ✅ | ⭐⭐⭐⭐（对外暴露） |

```bash
# ✅ 推荐：自定义网络
docker network create app-net
docker run -d --name db --network app-net mysql
docker run -d --name app --network app-net myapp

# ❌ 不推荐：--link（Docker 已废弃）
docker run -d --name db mysql
docker run -d --name app --link db myapp
```

---

### 网络别名

```bash
# 一个容器可以有多个 DNS 别名
docker network create mynet

docker run -d --name db \
  --network mynet \
  --network-alias mysql \
  --network-alias database \
  mysql

# 其他容器可以用三个名字访问
docker run --rm --network mynet alpine sh -c "ping db"       # ✅
docker run --rm --network mynet alpine sh -c "ping mysql"    # ✅
docker run --rm --network mynet alpine sh -c "ping database" # ✅
```

---

### IP 地址管理

```bash
# 创建指定子网的网络
docker network create \
  --driver bridge \
  --subnet 172.20.0.0/16 \
  --ip-range 172.20.10.0/24 \
  --gateway 172.20.10.1 \
  mynet

# 容器指定 IP
docker run -d --name db \
  --network mynet \
  --ip 172.20.10.100 \
  mysql

# 查看容器 IP
docker inspect db --format='{{.NetworkSettings.Networks.mynet.IPAddress}}'
```

---

### 网络排查

```bash
# 进入容器测试网络
docker exec -it 容器名 sh

# 常用测试命令
apk add iputils curl   # Alpine 安装工具
apt-get update && apt-get install iputils-ping curl  # Ubuntu

ping 目标容器名
curl http://服务名:端口
nslookup 服务名
netstat -tunlp

# 查看网络命名空间
ls -la /var/run/docker/netns/
```

---

### docker-compose 中的 Network

```yaml
version: '3.8'

services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    networks:
      - app-net
      - monitor-net   # 可连多个网络

  redis:
    image: redis
    container_name: redis
    networks:
      - app-net

  mysql:
    image: mysql:8
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
    networks:
      - app-net

  app:
    image: myapp:1.0
    container_name: spring-app
    ports:
      - "8080:8080"
    networks:
      - app-net
    depends_on:
      - mysql
      - redis
      - rabbitmq

  nginx:
    image: nginx
    ports:
      - "80:80"
    networks:
      - app-net

  prometheus:
    image: prom/prometheus
    networks:
      - monitor-net   # 监控网络，与应用隔离

networks:
  app-net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1

  monitor-net:
    external: true     # 使用外部已创建的网络
    name: monitoring

# docker-compose 会自动创建网络（默认：目录名_default）
# docker-compose up -d
# docker network ls | grep 目录名
```

---

### 网络清理

```bash
# 删除未使用的网络
docker network prune
docker network prune -f

# 删除特定网络
docker network rm mynet

# 注意：有容器在用的网络不能删除
docker network rm app-net
# Error: network app-net has active endpoints
```

---

### 常见问题

```bash
# Q1: 容器 ping 不通另一个容器
# 检查：
docker network ls                    # 网络是否存在
docker inspect 容器1 --format='{{.NetworkSettings.Networks}}'
docker inspect 容器2 --format='{{.NetworkSettings.Networks}}'
# 确保在同一网络

# Q2: 端口被占用
netstat -tunlp | grep 8080          # 查看谁占用了
# 换端口或停占用进程

# Q3: 容器能访问外网吗？
docker run --rm alpine ping 8.8.8.8  # 默认可以
# 如果不行，检查宿主机 iptables

# Q4: 如何让容器使用宿主机代理
docker run -e HTTP_PROXY=http://proxy:8080 alpine

# Q5: 容器重启后 IP 会变吗？
# 会变，所以用容器名通信，不要写死 IP
```

---

### 最佳实践

```bash
# ✅ 推荐：每个应用独立网络
docker network create order-system
docker network create user-system

# ✅ 推荐：网络隔离（前端不能直连数据库）
docker network create frontend
docker network create backend
# 只让 API 容器同时连两个网络

# ✅ 推荐：compose 自动管理网络
# 让 compose 创建，不用手动管理

# ❌ 避免：使用默认 bridge 做服务发现
# 默认 bridge 不支持 DNS

# ❌ 避免：生产环境用 host 网络（安全风险）
# 除非性能要求极高

# ❌ 避免：写死 IP 地址
# 容器 IP 会变化
```