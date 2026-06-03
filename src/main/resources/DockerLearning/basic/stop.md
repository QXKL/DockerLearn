## docker stop

`docker stop` 用于**优雅地停止一个或多个正在运行的容器**。它会先发送 SIGTERM 信号让容器做清理工作，超时后再发送 SIGKILL 强制停止。

### 基本语法

```bash
docker stop [选项] 容器名或ID [容器名或ID...]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-t` | 等待容器停止的超时时间（秒），默认10秒 | `docker stop -t 30 my-nginx` |

### 实战示例

```bash
# 1. 基本停止
docker stop my-nginx

# 2. 停止多个容器
docker stop my-nginx my-redis my-mysql

# 3. 指定等待时间（给容器足够时间保存数据）
docker stop -t 60 my-database

# 4. 停止所有运行中的容器
docker stop $(docker ps -q)

# 5. 强制停止（等待0秒）
docker stop -t 0 my-app
```

### stop vs kill vs pause

```bash
# stop：优雅停止（SIGTERM -> 等待 -> SIGKILL）
docker stop my-nginx

# kill：立即强制停止（SIGKILL）
docker kill my-nginx

# pause：暂停但不停止（冻结进程，不释放资源）
docker pause my-nginx

# 区别对比
docker stop my-app   # 进程收到 SIGTERM，可以清理资源
docker kill my-app   # 进程收到 SIGKILL，立即终止
docker pause my-app  # 进程被冻结，可恢复
```

### 优雅停止机制

```bash
# 容器内进程可以捕获 SIGTERM 信号做清理
# 示例：在 Dockerfile 中处理停止信号
```

```dockerfile
# Dockerfile
FROM nginx
# nginx 默认会优雅处理 SIGTERM
# 也可以自定义
STOPSIGNAL SIGTERM
```

```bash
# 测试优雅停止
docker run -d --name test-app alpine sleep 300
docker stop -t 10 test-app  # 等待10秒让 sleep 完成

# 查看停止状态
docker ps -a --filter "name=test-app"
# STATUS: Exited (0)  # 正常退出
```

### 实际场景

#### 场景1：停止服务维护
```bash
# 停止 Web 服务进行维护
docker stop my-web
# 执行维护操作
docker exec my-db backup.sh
# 重新启动
docker start my-web
```

#### 场景2：批量停止开发环境
```bash
# 停止所有开发相关容器
docker stop $(docker ps -q --filter "label=environment=dev")

# 或停止所有
docker stop $(docker ps -q)
```

#### 场景3：数据库维护前停止
```bash
# 给数据库足够时间刷写数据
docker stop -t 120 postgres-db
# 进行数据卷备份
docker run --rm -v pgdata:/data alpine tar czf backup.tar.gz /data
# 重启数据库
docker start postgres-db
```

### 信号处理

```bash
# 查看容器进程可以接收的信号
docker exec my-app kill -l

# 自定义停止信号
docker run -d --stop-signal=SIGTERM my-app

# 查看容器的停止信号
docker inspect --format='{{.Config.StopSignal}}' my-app

# 手动发送信号
docker kill --signal=SIGTERM my-app  # 等同于 stop
docker kill --signal=SIGHUP my-app   # 重载配置
```

### docker-compose 对照

```bash
# docker 命令
docker stop my-nginx my-redis

# docker-compose 命令
docker-compose stop           # 停止所有服务
docker-compose stop web       # 停止指定服务
docker-compose stop web db    # 停止多个服务
```

```yaml
# docker-compose.yml
services:
  web:
    image: nginx
    container_name: my-nginx
    stop_grace_period: 30s    # 设置优雅停止等待时间
    stop_signal: SIGTERM       # 设置停止信号

  redis:
    image: redis
    container_name: my-redis

# docker-compose stop -t 30 web  # 覆盖配置文件的等待时间
```

### 停止 vs 删除

```bash
# stop：停止容器，容器还在，可以重新启动
docker stop my-nginx
docker start my-nginx  # ✅ 可以

# rm：删除容器，需要重新创建
docker rm my-nginx
docker start my-nginx  # ❌ Error: No such container.md
docker run --name my-nginx nginx  # 需要重新创建

# 停止并删除（临时容器常用）
docker run --rm -it alpine sh  # 退出后自动删除
```

### 监控和调试

```bash
# 实时监控容器停止事件
docker events --filter event=stop --filter container.md=my-app

# 查看容器退出代码
docker ps -a --filter "name=my-app"
# STATUS: Exited (0)     # 0=正常退出
# STATUS: Exited (137)   # 137=被 SIGKILL 杀死
# STATUS: Exited (143)   # 143=被 SIGTERM 终止

# 查看详细的停止时间
docker inspect --format='{{.State.FinishedAt}}' my-app
```

### 注意事项

```bash
# ⚠️ 停止容器会中断服务
# 生产环境建议先切换到负载均衡

# ⚠️ 未持久化的数据会丢失
docker run -d --name temp-db mysql  # 没有挂载卷
docker stop temp-db
docker rm temp-db  # 数据丢失

# ⚠️ 停止一个已经停止的容器不会报错
docker stop already-stopped-container.md  # 静默成功

# ⚠️ 停止容器不会自动删除
# 大量停止的容器会占用磁盘空间，需要定期清理
docker container.md prune  # 删除所有停止的容器
```

### 常用组合

```bash
# 停止所有容器（包括未运行的？不会）
docker stop $(docker ps -q)

# 停止并删除所有容器
docker stop $(docker ps -q) && docker rm $(docker ps -aq)

# 重启所有容器
docker stop $(docker ps -q) && docker start $(docker ps -aq)

# 重启特定标签的容器
docker stop $(docker ps -q --filter "label=auto-restart")
docker start $(docker ps -aq --filter "label=auto-restart")
```