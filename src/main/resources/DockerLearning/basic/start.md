## docker start

`docker start` 用于**启动一个或多个已停止的容器**。

### 基本语法

```bash
docker start [选项] 容器名或ID [容器名或ID...]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-a` | 附加到容器并显示输出 | `docker start -a my-nginx` |
| `-i` | 交互模式 | `docker start -i my-ubuntu` |

### 实战示例

```bash
# 1. 先创建并停止一个容器
docker run -d --name test-nginx nginx
docker stop test-nginx

# 2. 启动已停止的容器
docker start test-nginx

# 3. 启动多个容器
docker start test-nginx test-ubuntu

# 4. 启动并附加到容器（查看输出）
docker start -a test-nginx

# 5. 启动交互式容器
docker run --name my-ubuntu -it ubuntu bash  # 然后 exit 退出
docker start -i my-ubuntu  # 重新进入
```

### 实际场景

#### 场景1：重启之前运行的服务
```bash
# 早上启动，晚上停止
docker stop web-app db-app
# 第二天早上
docker start web-app db-app
```

```yaml
# 对于不存在的容器，会默认创建，即run。此处默认存在，即执行start
service:
  web-app:
    # ...
  db-app:
    # ...
```

#### 场景2：批量启动容器
```bash
# 启动所有名称包含 "app" 的容器
docker start $(docker ps -a -q --filter "name=app")

# 启动所有已停止的容器
docker start $(docker ps -a -q --filter "status=exited")
```

#### 场景3：启动并查看日志
```bash
# 启动并实时查看输出
docker start -a web-app

# 或者先启动再查看日志
docker start web-app
docker logs web-app
```

### docker-compose 对照

```bash
# docker 命令
docker start my-nginx my-redis

# docker-compose 命令（需要在 compose 目录下）
docker-compose start

# 启动特定服务
docker-compose start web db
```

```yaml
# docker-compose.yml
services:
  web:
    image: nginx
    container_name: my-nginx
  db:
    image: redis
    container_name: my-redis

# docker-compose start web   # 只启动 web
# docker-compose start       # 启动所有
```

### 状态检查

```bash
# 启动前查看容器状态
docker ps -a --filter "name=my-nginx"
# STATUS: Exited

# 启动
docker start my-nginx

# 验证是否成功
docker ps --filter "name=my-nginx"
# STATUS: Up 5 seconds
```

### 注意事项

```bash
# ⚠️ start 不能修改容器配置
# 下面的操作无效：
docker start --restart=always my-nginx  # 不会改变重启策略

# 要修改配置需要：
docker update.md --restart=always my-nginx  # 更新配置
# 或者重新创建容器

# ⚠️ 启动已删除的容器会报错
docker start missing-container.md
# Error: No such container.md: missing-container.md
```

### 常用组合命令

```bash
# 停止所有运行中的容器
docker stop $(docker ps -q)

# 启动所有已停止的容器
docker start $(docker ps -a -q)

# 重启所有容器（stop + start）
docker restart $(docker ps -a -q)

# 清理并重启
docker stop $(docker ps -q) && docker start $(docker ps -a -q)
```