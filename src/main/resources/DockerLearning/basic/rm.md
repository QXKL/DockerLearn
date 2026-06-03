## docker rm

`docker rm` 用于**删除一个或多个已停止的容器**。如果要删除正在运行的容器，需要加 `-f` 参数。

### 基本语法

```bash
docker rm [选项] 容器名或ID [容器名或ID...]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-f` | 强制删除正在运行的容器（先停止再删除） | `docker rm -f my-app` |
| `-v` | 删除容器关联的匿名卷 | `docker rm -v my-app` |
| `-l` | 删除容器间的网络连接，不删除容器本身 | `docker rm -l db-link` |

### 实战示例

```bash
# 1. 删除已停止的容器
docker rm my-stopped-container.md

# 2. 删除多个容器
docker rm container1 container2 container3

# 3. 强制删除正在运行的容器
docker rm -f my-running-app

# 4. 删除容器并删除关联的匿名卷
docker rm -v my-app

# 5. 删除所有已停止的容器
docker container.md prune

# 6. 删除所有容器（包括运行中的）
docker rm -f $(docker ps -aq)
```

### rm vs stop vs kill vs prune

```bash
# 创建测试容器
docker run -d --name test1 nginx
docker run -d --name test2 nginx
docker run -d --name test3 nginx

# stop：停止但保留容器
docker stop test1
docker ps -a | grep test1  # 还在，状态 Exited

# rm：删除已停止的容器
docker rm test1
docker ps -a | grep test1  # 不存在了

# rm -f：强制删除运行中的容器
docker rm -f test2  # 无需先 stop

# prune：批量删除
docker container.md prune  # 删除所有已停止的容器
docker system prune -a  # 更彻底的清理
```

### 实际场景

#### 场景1：清理临时容器
```bash
# 临时测试容器，用完即删
docker run --rm -it alpine sh  # 退出时自动删除

# 手动清理
docker run -it --name temp ubuntu bash
exit
docker rm temp

# 批量清理以 temp- 开头的容器
docker rm $(docker ps -a -q --filter "name=temp-")
```

#### 场景2：重新创建容器
```bash
# 更新配置需要重建容器
docker rm -f my-nginx
docker run -d --name my-nginx -p 8080:80 nginx

# 或者一条命令完成
docker rm -f my-nginx 2>/dev/null; docker run -d --name my-nginx nginx

# 使用 docker-compose（推荐）
docker-compose down && docker-compose up -d
```

#### 场景3：清理开发环境
```bash
# 停止并删除所有开发容器
docker stop $(docker ps -q)
docker rm $(docker ps -aq)

# 删除指定标签的容器
docker rm $(docker ps -a -q --filter "label=environment=dev")

# 保留特定容器
docker rm $(docker ps -a -q | grep -v $(docker ps -q --filter "name=keep-me"))
```

#### 场景4：磁盘空间清理
```bash
# 查看容器占用空间
docker ps -a -s

# 删除已停止的容器
docker container.md prune -f

# 删除所有未使用的容器、网络、镜像
docker system prune -a --volumes

# 查看释放的空间
docker system df
```

### 删除策略

```bash
# 方式1：逐个删除
docker rm container1
docker rm container2

# 方式2：批量删除（空格分隔）
docker rm container1 container2 container3

# 方式3：命令替换
docker rm $(docker ps -a -q)                    # 删除所有
docker rm $(docker ps -a -q --filter "status=exited")  # 只删除已停止的
docker rm $(docker ps -a -q --filter "name=test")      # 删除名称包含 test 的

# 方式4：使用 xargs
docker ps -a -q | xargs docker rm -f

# 方式5：使用 prune（推荐）
docker container.md prune
docker container.md prune -f  # 不提示确认
```

### 过滤条件

```bash
# 删除退出状态为 0 的容器
docker rm $(docker ps -a -q --filter "exited=0")

# 删除创建超过24小时的容器
docker ps -a --filter "until=24h" -q | xargs docker rm

# 删除特定镜像创建的容器
docker rm $(docker ps -a -q --filter "ancestor=nginx")

# 删除没有名称的容器
docker rm $(docker ps -a -q --filter "name=.*")

# 组合过滤
docker ps -a \
  --filter "status=exited" \
  --filter "exited=1" \
  -q | xargs docker rm
```

### docker-compose 对照

```bash
# docker 命令
docker rm my-nginx
docker rm -f $(docker ps -aq)

# docker-compose 命令
docker-compose down        # 停止并删除容器（保留镜像）
docker-compose down -v     # 同时删除卷
docker-compose down --rmi all  # 同时删除镜像

# 删除特定服务容器
docker-compose rm web      # 删除 web 服务容器
docker-compose rm -f web   # 强制删除
docker-compose rm -v web   # 删除关联卷
```

```yaml
# docker-compose.yml
services:
  web:
    image: nginx
    container_name: my-nginx
  
  db:
    image: postgres
    volumes:
      - db-data:/var/lib/postgresql/data

volumes:
  db-data:

# docker-compose down      # 停止并删除容器，但保留 db-data 卷
# docker-compose down -v   # 同时删除 db-data 卷
```

### 安全删除

```bash
# 1. 确认容器状态
docker ps -a --filter "name=my-app"

# 2. 备份容器数据（如果需要）
docker cp my-app:/app/data ./backup

# 3. 导出容器配置
docker inspect my-app > my-app-config.json

# 4. 删除容器
docker rm my-app

# 5. 确认删除
docker ps -a --filter "name=my-app"
```

### 常见问题

```bash
# Q1: 删除时报错 "container.md is running"
# 解决：先停止再删除，或用 -f
docker stop my-app && docker rm my-app
docker rm -f my-app

# Q2: 删除时报错 "No such container.md"
# 解决：容器已不存在，忽略即可
docker rm missing-container.md || true

# Q3: 批量删除时某个容器出错导致停止
# 解决：使用 xargs 继续执行
docker ps -aq | xargs -I {} docker rm -f {} || true

# Q4: 如何删除所有停止的容器（保留运行的）
docker container.md prune
# 或
docker rm $(docker ps -aq --filter "status=exited")

# Q5: 删除后如何恢复？
# 无法恢复，需要重新运行
docker run ...  # 重新创建
```

### 与其他命令配合

```bash
# 停止并删除所有容器
docker stop $(docker ps -q) && docker rm $(docker ps -aq)

# 删除所有已停止的容器和虚悬镜像
docker container.md prune && docker image prune

# 清理所有未使用资源（危险）
docker system prune -a --volumes

# 定时清理脚本
#!/bin/bash
# cleanup-docker.sh
echo "Cleaning up old containers..."
docker container.md prune -f
docker image prune -f
docker volume prune -f
docker network prune -f

# 添加到 crontab
# 0 2 * * * /usr/local/bin/cleanup-docker.sh
```

### 最佳实践

```bash
# ✅ 推荐：使用 --rm 自动清理
docker run --rm -it alpine sh

# ✅ 推荐：使用 prune 定期清理
docker container.md prune -f

# ✅ 推荐：给容器打标签便于批量清理
docker run -d --label env=dev --name dev-db mysql
docker rm $(docker ps -aq --filter "label=env=dev")

# ❌ 避免：直接删除运行中的容器（可能丢失数据）
docker rm -f running-db  # 数据库容器可能丢失数据

# ✅ 正确：先优雅停止
docker stop running-db
docker rm running-db
```