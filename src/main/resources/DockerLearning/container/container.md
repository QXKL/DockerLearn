## docker container

容器（Container）是**镜像的运行实例**，是一个轻量级的独立运行环境。

### 容器 vs 镜像（回顾）

| 概念     | 类比         | 特征        |
|--------|------------|-----------|
| **镜像** | 程序文件（.exe） | 只读、静态、可分发 |
| **容器** | 进程         | 可写、动态、有状态 |

```
镜像：docker images       容器：docker ps -a
         ↓                           ↓
      nginx:latest    ──run──→    web-server (Up)
      redis:alpine    ──run──→    cache (Up)
      mysql:8.0       ──stop─→    db (Exited)
```

---

### 常用命令汇总

```bash
# 查看
docker ps                 # 运行中的容器
docker ps -a              # 所有容器
docker ps -q              # 只显示ID
docker ps -s              # 显示容器大小
docker ps -l              # 最后创建的容器

# 创建与运行
docker run -d 镜像         # 后台运行
docker run -it 镜像 bash   # 交互式运行
docker run --rm 镜像        # 退出自动删除

# 生命周期
docker start 容器          # 启动（已存在）
docker stop 容器           # 优雅停止
docker restart 容器        # 重启
docker pause 容器          # 暂停
docker unpause 容器        # 恢复
docker kill 容器           # 强制停止
docker rm 容器             # 删除（已停止）
docker rm -f 容器          # 强制删除

# 交互与调试
docker exec -it 容器 bash   # 进入容器
docker logs 容器            # 查看日志
docker logs -f 容器         # 实时跟踪
docker logs --tail 100 容器 # 最后100行
docker stats                # 实时资源监控
docker top 容器             # 查看容器内进程

# 信息查看
docker inspect 容器         # 详细信息
docker port 容器            # 查看端口映射
docker diff 容器            # 查看文件变化
```

---

### 容器状态流转

```
                    docker run
                        │
                        ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │  Created │───→│  Running │───→│  Paused  │
    └──────────┘    └──────────┘    └──────────┘
         │               │               │
    docker rm        docker stop      docker unpause
         │               │
         ▼               ▼
    ┌──────────┐    ┌──────────┐
    │  Removed │    │  Exited  │
         ↑     └──────│ docker start
         └────────────┘
              Exited ──→ Running
```

**状态说明：**

| 状态 | 含义 |
|------|------|
| `Created` | 已创建但未启动（docker create） |
| `Running` | 正在运行 |
| `Paused` | 已暂停（进程冻结） |
| `Exited` | 已停止（正常退出或出错） |
| `Dead` | 无法正常停止 |

---

### 实战示例

#### 查看容器

```bash
# 查看运行中的容器
docker ps
# CONTAINER ID   IMAGE     STATUS          NAMES
# abc123         nginx     Up 2 hours      web-1

# 查看所有容器
docker ps -a
# CONTAINER ID   IMAGE     STATUS                    NAMES
# abc123         nginx     Up 2 hours                web-1
# def456         redis     Exited (0) 5 minutes ago  redis-cache

# 格式化输出
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 只看特定名称
docker ps --filter "name=web"
```

#### 创建与运行

```bash
# 最简运行
docker run nginx

# 后台运行
docker run -d nginx

# 交互式运行（进入容器）
docker run -it ubuntu bash

# 指定名称和端口
docker run -d --name my-web -p 8080:80 nginx

# 挂载卷
docker run -d -v mydata:/data --name app myapp

# 自动清理
docker run --rm -it alpine sh
```

#### 生命周期操作

```bash
# 启动已存在的容器
docker start my-web

# 优雅停止（给10秒清理时间）
docker stop my-web
docker stop -t 30 my-web   # 等待30秒

# 重启
docker restart my-web

# 强制停止（不清理）
docker kill my-web

# 暂停/恢复
docker pause my-web
docker unpause my-web

# 删除
docker rm my-web           # 删除已停止的
docker rm -f my-web        # 强制删除运行中的
```

#### 进入容器与调试

```bash
# 进入容器（推荐）
docker exec -it my-web bash
docker exec -it my-web sh   # Alpine 用 sh

# 执行单条命令
docker exec my-web ls -la
docker exec my-web cat /etc/os-release

# 以特定用户执行
docker exec -u www-data my-web whoami

# 查看日志
docker logs my-web
docker logs -f --tail 100 my-web

# 查看资源
docker stats my-web
docker top my-web

# 查看详细信息
docker inspect my-web
docker inspect --format='{{.NetworkSettings.IPAddress}}' my-web
```

---

### 容器资源限制

```bash
# 运行时限制
docker run -d --cpus=1 --memory=512m --name limited nginx

# 更新已运行容器的限制
docker update --cpus=0.5 --memory=256m limited

# 查看限制
docker inspect limited --format='CPU:{{.HostConfig.NanoCpus}} MEM:{{.HostConfig.Memory}}'
```

---

### 容器与宿主机互操作

```bash
# 文件复制
docker cp ./index.html my-web:/usr/share/nginx/html/
docker cp my-web:/var/log/nginx/access.log ./

# 端口映射（只能在 run 时设置）
docker run -d -p 8080:80 nginx   # 主机:容器

# 查看映射
docker port my-web
```

---

### 容器清理

```bash
# 删除所有已停止的容器
docker container prune
docker container prune -f   # 不确认

# 删除所有容器（运行中也会被删）
docker rm -f $(docker ps -aq)

# 删除特定状态的容器
docker rm $(docker ps -aq --filter "status=exited")
docker rm $(docker ps -aq --filter "status=created")

# 删除名称匹配的容器
docker rm $(docker ps -aq --filter "name=test-")
```

---

### docker-compose 对照

```yaml
# docker-compose.yml
services:
  web:
    image: nginx:alpine
    container_name: my-web
    ports:
      - "8080:80"
    volumes:
      - ./html:/usr/share/nginx/html
    restart: always
    cpus: 0.5
    mem_limit: 256m

# 对应 docker 命令
# docker run -d --name my-web -p 8080:80 -v ./html:/usr/share/nginx/html --restart always --cpus=0.5 --memory=256m nginx:alpine
```

```bash
# compose 中的容器管理
docker-compose up -d      # 创建并启动
docker-compose down       # 停止并删除
docker-compose stop       # 只停止
docker-compose start      # 启动
docker-compose restart    # 重启
docker-compose ps         # 查看状态
docker-compose exec web bash  # 进入容器
```

---

### 常见问题

```bash
# Q1: 容器退出后还能恢复吗？
docker start 容器名        # 可以，只是重新启动
# 但容器内的临时文件（未挂载卷）会丢失

# Q2: 如何让容器开机自启？
docker run -d --restart always nginx

# 已运行的容器添加自启
docker update --restart always 容器名

# Q3: 容器名忘了怎么办？
docker ps -a --format "table {{.Names}}\t{{.Image}}\t{{.Status}}"

# Q4: 如何批量操作？
docker stop $(docker ps -q)           # 停止所有
docker restart $(docker ps -aq)       # 重启所有
docker rm -f $(docker ps -aq)         # 删除所有
```