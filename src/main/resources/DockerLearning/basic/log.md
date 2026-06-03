## docker logs

`docker logs` 用于**查看容器的日志输出**。可以查看容器内应用打印到 stdout 和 stderr 的日志。

### 基本语法

```bash
docker logs [选项] 容器名或ID
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-f` | 实时跟踪日志输出（类似 tail -f） | `docker logs -f my-app` |
| `--tail` | 显示最后 N 行日志 | `docker logs --tail 100 my-app` |
| `--since` | 显示指定时间之后的日志 | `docker logs --since 2024-01-01 my-app` |
| `--until` | 显示指定时间之前的日志 | `docker logs --until 2024-01-02 my-app` |
| `-t` | 显示时间戳 | `docker logs -t my-app` |
| `--details` | 显示额外的日志详细信息 | `docker logs --details my-app` |

### 实战示例

```bash
# 1. 查看所有日志
docker logs my-nginx

# 2. 查看最后 50 行
docker logs --tail 50 my-nginx

# 3. 实时跟踪日志
docker logs -f my-nginx

# 4. 显示带时间戳的日志
docker logs -t my-nginx

# 5. 查看最近 5 分钟的日志
docker logs --since 5m my-nginx

# 6. 查看特定时间段的日志
docker logs --since "2024-01-01T10:00:00" --until "2024-01-01T11:00:00" my-app

# 7. 组合使用
docker logs -f --tail 100 --since 10m my-nginx
```

### 时间格式

```bash
# --since 和 --until 支持的时间格式

# 相对时间
docker logs --since 5m my-app      # 5分钟前
docker logs --since 1h my-app      # 1小时前
docker logs --since 2d my-app      # 2天前
docker logs --since 30s my-app     # 30秒前

# 绝对时间（RFC3339）
docker logs --since "2024-01-01T10:00:00" my-app
docker logs --since "2024-01-01T10:00:00Z" my-app
docker logs --since "2024-01-01T10:00:00+08:00" my-app

# 日期格式
docker logs --since "2024-01-01" my-app
docker logs --since "2024-01-01 10:00:00" my-app
```

### 实际场景

#### 场景1：实时监控应用
```bash
# 实时跟踪 nginx 访问日志
docker logs -f my-nginx

# 查看错误日志
docker logs -f my-nginx 2>&1 | grep ERROR

# 监控特定关键词
docker logs -f my-app | grep -i "exception"

# 实时统计错误数量
docker logs -f my-app | grep -c "ERROR"
```

#### 场景2：排查启动失败
```bash
# 容器启动失败，查看日志
docker logs my-failed-container.md

# 查看最后 20 行（启动日志）
docker logs --tail 20 my-failed-container.md

# 查看容器退出前的日志
docker logs --tail 50 my-failed-container.md
```

#### 场景3：日志分析和统计
```bash
# 统计错误数量
docker logs my-app | grep -c "ERROR"

# 查看最近的错误
docker logs --since 1h my-app | grep ERROR

# 按时间排序（已有时间戳）
docker logs -t my-app | head -20

# 统计访问量（nginx）
docker logs my-nginx | grep -c "GET"
```

#### 场景4：多容器日志管理
```bash
# 同时查看多个容器日志
docker logs -f web-app &
docker logs -f db-app &
docker logs -f redis-app &

# 使用脚本合并日志
for container.md in web-app db-app redis-app; do
  docker logs --tail 10 $container &
done

# 使用 docker-compose
docker-compose logs -f web db redis
```

### 日志驱动

Docker 支持多种日志驱动，默认是 `json-file`

```bash
# 查看当前日志驱动
docker info | grep "Logging Driver"
docker inspect --format='{{.HostConfig.LogConfig.Type}}' my-app

# 运行时指定日志驱动
docker run -d --log-driver syslog --name my-app nginx
docker run -d --log-driver fluentd --log-opt fluentd-address=localhost:24224 nginx

# 常用日志驱动
# json-file    - 默认，JSON 格式存储
# syslog       - 发送到 syslog
# journald     - systemd journal
# fluentd      - fluentd 日志收集
# loki         - Grafana Loki
# none         - 禁用日志
```

### 日志限制和轮转

```bash
# 设置日志大小和轮转（json-file 驱动）
docker run -d \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  --name my-app \
  nginx

# 全局配置（/etc/docker/daemon.json）
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3",
    "compress": "true"
  }
}

# 查看日志文件位置
docker inspect --format='{{.LogPath}}' my-app
# 通常在 /var/lib/docker/containers/<container.md-id>/<container.md-id>-json.log
```

### docker-compose 对照

```yaml
# docker-compose.yml
services:
  web:
    image: nginx
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
  
  db:
    image: mysql
    logging:
      driver: fluentd
      options:
        fluentd-address: localhost:24224
        tag: docker.mysql

  app:
    image: myapp
    logging:
      driver: none   # 禁用日志

# 查看日志
# docker-compose logs web
# docker-compose logs -f web db
# docker-compose logs --tail 50 web
```

### 日志输出最佳实践

```bash
# 应用内最佳实践
# 1. 输出到 stdout/stderr 而不是文件
echo "Error: something wrong" >&2   # stderr
echo "Info: server started"         # stdout

# 2. 使用结构化日志（JSON）
echo '{"level":"info","message":"user login","user_id":123}'

# 3. 避免敏感信息
echo "User logged in"  # ✅
echo "Password: secret" # ❌
```

### 高级用法

```bash
# 1. 使用 jq 处理 JSON 日志
docker logs my-app | jq '.["message"]'

# 2. 彩色输出
docker logs my-app | sed 's/ERROR/\x1b[31mERROR\x1b[0m/g'

# 3. 保存日志到文件
docker logs my-app > app.log 2>&1
docker logs my-app &>> app.log

# 4. 循环日志检测
while true; do
  if docker logs --tail 1 my-app | grep -q "ERROR"; then
    echo "Error detected!"
  fi
  sleep 1
done

# 5. 发送日志到远程
docker logs -f my-app | nc logs-server.com 5140

# 6. 统计日志速率
docker logs -f my-app | pv -l > /dev/null
```

### 故障排查

```bash
# 问题：没有日志输出
docker logs my-app  # 无输出
# 检查：
docker exec my-app ls /proc/1/fd/  # 检查 stdout/stderr
docker exec my-app echo "test"     # 测试输出

# 问题：日志文件太大
docker inspect --format='{{.LogPath}}' my-app
ls -lh /var/lib/docker/containers/.../...-json.log
# 解决：
docker run --log-opt max-size=10m ...  # 限制大小
echo "" > $(docker inspect --format='{{.LogPath}}' my-app)  # 清空（不推荐）

# 问题：日志驱动不可用
docker run --log-driver=none my-app
docker logs my-app  # Error: no logging driver configured

# 问题：时区不对
docker run -e TZ=Asia/Shanghai my-app
docker logs -t my-app  # 检查时间戳
```

### 清理日志

```bash
# 清空容器日志（不推荐直接删除）
truncate -s 0 $(docker inspect --format='{{.LogPath}}' my-app)

# 重启容器（会创建新日志文件）
docker restart my-app

# 删除并重建容器（更彻底）
docker rm -f my-app
docker run -d --name my-app --log-opt max-size=10m nginx

# 全局清理所有日志
docker system prune -a --volumes

# 清理脚本
for container.md in $(docker ps -aq); do
  log_path=$(docker inspect --format='{{.LogPath}}' $container)
  if [ -f "$log_path" ]; then
    truncate -s 0 $log_path
  fi
done
```