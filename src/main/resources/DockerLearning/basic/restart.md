## docker restart

`docker restart` 用于**重启一个或多个正在运行的容器**。相当于先执行 `docker stop`，再执行 `docker start`。

### 基本语法

```bash
docker restart [选项] 容器名或ID [容器名或ID...]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-t` | 等待容器停止的超时时间（秒），默认10秒 | `docker restart -t 30 my-nginx` |

### 实战示例

```bash
# 1. 基本重启
docker restart my-nginx

# 2. 重启多个容器
docker restart my-nginx my-redis my-mysql

# 3. 指定停止前的等待时间（给容器时间做清理工作）
docker restart -t 30 my-nginx  # 等待30秒后才强制停止

# 4. 重启所有运行中的容器
docker restart $(docker ps -q)

# 5. 重启特定名称的容器
docker restart $(docker ps -q --filter "name=web")
```

### restart vs stop + start

```bash
# 两种方式效果相同啦
```

### 实际场景

#### 场景1：应用配置更新后重启
```bash
# 修改了容器内的配置文件
docker exec my-nginx nginx -s reload  # Nginx 可以热重载
# 如果应用不支持热重载，需要重启
docker restart my-nginx

# 或者更新了环境变量（需要重启才能生效）
docker update.md --restart=always my-nginx
docker restart my-nginx
```

#### 场景2：容器卡死或响应缓慢
```bash
# 强制重启（减少等待时间）
docker restart -t 1 my-slow-app

# 查看重启后的状态
docker ps -a
docker logs my-slow-app
```

#### 场景3：定期重启（配合 cron）
```bash
# crontab -e
# 每天凌晨3点重启容器
0 3 * * * docker restart my-app

# 每小时重启一次
0 * * * * docker restart log-processor
```

### 重启策略 vs restart命令

```bash
# --restart 策略：容器退出时自动重启（被动）
docker run -d --restart=always my-app

# restart 命令：手动主动重启
docker restart my-app

# 查看重启策略
docker inspect --format='{{.HostConfig.RestartPolicy.Name}}' my-app
```

### docker-compose 对照

```bash
# docker 命令
docker restart my-nginx my-redis

# docker-compose 命令
docker-compose restart          # 重启所有服务
docker-compose restart web      # 重启指定服务
docker-compose restart web db   # 重启多个服务
```

```yaml
# docker-compose.yml
services:
  web:
    image: nginx
    container_name: my-nginx
    restart: always        # 这个是被动重启策略
    # restart: unless-stopped
    # restart: on-failure

  redis:
    image: redis
    container_name: my-redis

# docker-compose restart web   # 手动重启 web 服务
```

### 监控和调试

```bash
# 查看容器运行时间（uptime）
docker ps --format "table {{.Names}}\t{{.Status}}"

# 重启前后的状态对比
docker ps --filter "name=my-nginx" --format "{{.Status}}"
# 重启前: Up 5 days
docker restart my-nginx
# 重启后: Up 5 seconds

# 查看重启日志
docker logs my-nginx --since 1m

# 监控容器事件
docker events --filter event=restart --filter container.md=my-nginx
```

### 注意事项

```bash
# ⚠️ 重启会中断服务
# 生产环境要注意影响，考虑负载均衡或零停机部署

# ⚠️ 数据持久化问题
# 重启不会删除容器，数据卷中的数据依然保留
# 但容器内的临时数据（非挂载卷）会丢失

# ⚠️ 重启失败的情况
docker restart stopped-container.md
# Error: Cannot restart container.md xxx: container.md is not running

# 需要先用 start
docker start stopped-container.md
# 或直接 restart 不会报错，但实际无效（需要先运行）
```

### 高级用法

```bash
# 1. 条件重启（只重启运行时间超过1小时的容器）
for container.md in $(docker ps -q); do
  UPTIME=$(docker inspect --format='{{.State.StartedAt}}' $container)
  # 判断逻辑...
  docker restart $container
done

# 2. 滚动重启（多个服务实例逐一重启）
docker restart web-1
sleep 10
docker restart web-2
sleep 10
docker restart web-3

# 3. 带健康检查的重启
while ! docker exec my-app curl -f http://localhost:8080/health; do
  echo "App unhealthy, restarting..."
  docker restart my-app
  sleep 5
done
```