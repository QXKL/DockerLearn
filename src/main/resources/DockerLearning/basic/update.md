## docker update

`docker update` 用于**更新正在运行或已停止的容器资源配置**，无需重启容器即可生效部分配置。

### 基本语法

```bash
docker update [选项] 容器名或ID [容器名或ID...]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `--cpus` | CPU 核心数限制 | `docker update --cpus=1.5 my-app` |
| `--memory` | 内存限制 | `docker update --memory=512m my-app` |
| `--memory-swap` | 交换内存限制 | `docker update --memory-swap=1g my-app` |
| `--restart` | 重启策略 | `docker update --restart=always my-app` |
| `--cpu-shares` | CPU 权重（相对值） | `docker update --cpu-shares=512 my-app` |
| `--cpuset-cpus` | 绑定特定 CPU 核心 | `docker update --cpuset-cpus="0,2" my-app` |
| `--kernel-memory` | 内核内存限制 | `docker update --kernel-memory=100m my-app` |

### 实战示例

```bash
# 1. 限制 CPU 使用
docker run -d --name stress-app stress --cpu 4
docker update --cpus=1 stress-app  # 限制只用1个CPU核心

# 2. 限制内存
docker update --memory=256m --memory-swap=512m my-app

# 3. 修改重启策略
docker update --restart=always my-nginx
docker update --restart=unless-stopped my-redis

# 4. 绑定特定 CPU 核心
docker update --cpuset-cpus="0,1,2" my-app  # 只能用0,1,2号核心

# 5. 调整 CPU 权重（竞争时）
docker update --cpu-shares=1024 important-app
docker update --cpu-shares=512 normal-app
```

### 可更新 vs 不可更新

```bash
# ✅ 可以动态更新（无需重启）
docker update --cpus=1 --memory=512m my-app
docker update --restart=always my-app
docker update --cpu-shares=1024 my-app

# ❌ 不可更新（需要重新创建容器）
# 端口映射
# 卷挂载
# 环境变量
# 网络设置
# 镜像

# 示例：想修改端口映射需要重新创建
docker rm -f my-app
docker run -d -p 8080:80 --name my-app nginx
```

### 实际场景

#### 场景1：动态调整资源限制
```bash
# 白天业务高峰期，增加资源
docker update --cpus=2 --memory=1g web-server

# 晚上低峰期，减少资源
docker update --cpus=0.5 --memory=256m web-server
```

#### 场景2：修复忘记设置重启策略
```bash
# 忘记设置 --restart=always
docker run -d --name important-db mysql

# 事后补救
docker update --restart=always important-db
# 现在服务器重启后会自动启动
```

#### 场景3：多容器资源调配
```bash
# 三个服务竞争资源
docker run -d --name app1 nginx
docker run -d --name app2 nginx
docker run -d --name app3 nginx

# 给重要的服务更多权重
docker update --cpu-shares=2048 app1
docker update --cpu-shares=512 app2
docker update --cpu-shares=512 app3
```

#### 场景4：批量更新
```bash
# 限制所有运行中容器的内存（谨慎使用）
for container.md in $(docker ps -q); do
  docker update --memory=512m $container
done

# 更新特定名称的容器
docker update --restart=always $(docker ps -aq --filter "name=db")
```

### 监控资源使用

```bash
# 查看当前资源限制
docker inspect --format='{{.HostConfig.Memory}}' my-app
docker inspect --format='{{.HostConfig.CpuShares}}' my-app
docker inspect --format='{{.HostConfig.RestartPolicy.Name}}' my-app

# 实时查看资源使用
docker stats my-app

# 查看所有容器资源限制
docker inspect $(docker ps -aq) --format='{{.Name}} CPU:{{.HostConfig.CpuShares}} MEM:{{.HostConfig.Memory}}'
```

### docker-compose 对照

```bash
# docker-compose.yml 中声明资源限制
```

```yaml
services:
  web:
    image: nginx
    deploy:                    # swarm 模式
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
  
  db:
    image: mysql
    restart: always
    # 非 swarm 模式可以用
    cpus: 1.5
    mem_limit: 1g
    memswap_limit: 2g

# 注意：docker-compose update 命令不存在
# 需要手动使用 docker update
```

### 资源限制详解

```bash
# CPU 限制的几种方式
docker update --cpus=0.5 my-app        # 最多使用 0.5 个 CPU
docker update --cpus=2 my-app          # 最多使用 2 个 CPU
docker update --cpu-shares=1024 my-app # 权重（默认1024）
docker update --cpuset-cpus="0-3" my-app # 绑定 0-3 号核心

# 内存限制
docker update --memory=256m my-app     # 256 MB
docker update --memory=1g my-app       # 1 GB
docker update --memory=512m --memory-swap=1g my-app  # 限制swap

# 重启策略
docker update --restart=no my-app              # 不重启
docker update --restart=always my-app          # 总是重启
docker update --restart=unless-stopped my-app  # 除非手动停止
docker update --restart=on-failure:3 my-app    # 失败时重启，最多3次
```

### 注意事项

```bash
# ⚠️ 内存限制不能超过系统可用内存
docker update --memory=32g my-app  # 如果只有16G内存会失败

# ⚠️ 缩小内存限制可能触发 OOM Kill
docker update --memory=128m heavy-app  # 容器可能被杀死

# ⚠️ CPU 绑定需要容器有足够核心
docker update --cpuset-cpus="8" my-app  # 系统只有4核心会失败

# ⚠️ 某些配置更新后需要重启才能完全生效
docker update --restart=always my-app
# 重启策略立即生效，但容器当前状态不变

# ⚠️ 容器被停止后仍可更新
docker stop my-app
docker update --restart=always my-app  # ✅ 可以
```

### 验证更新效果

```bash
# 1. 创建测试容器
docker run -d --name test --cpus=0.5 --memory=256m stress --cpu 2

# 2. 查看初始限制
docker inspect test --format='CPU:{{.HostConfig.NanoCpus}} MEM:{{.HostConfig.Memory}}'

# 3. 更新限制
docker update --cpus=1 --memory=512m test

# 4. 验证更新
docker inspect test --format='CPU:{{.HostConfig.NanoCpus}} MEM:{{.HostConfig.Memory}}'

# 5. 实时查看效果
docker stats test --no-stream
```