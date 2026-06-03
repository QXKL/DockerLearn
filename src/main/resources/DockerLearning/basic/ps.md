## docker ps

`docker ps` 用于**列出当前正在运行的容器**。

### 基本用法

```bash
# 查看正在运行的容器
docker ps

# 查看所有容器（包括已停止的）
docker ps -a

# 只显示容器ID
docker ps -q

# 显示最新创建的5个容器
docker ps -n 5

# 显示最后创建的容器
docker ps -l

# 显示容器大小
docker ps -s
```

### 输出字段说明

| 字段 | 说明 |
|------|------|
| CONTAINER ID | 容器ID（前12位） |
| IMAGE | 使用的镜像 |
| COMMAND | 启动时执行的命令 |
| CREATED | 创建时间 |
| STATUS | 当前状态（Up/Exited） |
| PORTS | 端口映射 |
| NAMES | 容器名称 |

### 常用组合

```bash
# 查看所有容器，显示完整信息
docker ps -a --no-trunc

# 根据状态过滤（created, restarting, running, paused, exited）
docker ps --filter "status=exited"
docker ps --filter "status=running"

# 根据名称过滤
docker ps --filter "name=my-nginx"

# 只显示特定字段
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}"
```

### 实战练习

```bash
# 1. 先运行一个容器
docker run -d --name test-nginx nginx

# 2. 查看运行中的容器
docker ps
# 应该看到 test-nginx 状态为 Up

# 3. 停止它
docker stop test-nginx

# 4. 再次查看运行中的容器
docker ps
# 看不到 test-nginx

# 5. 查看所有容器
docker ps -a
# 现在能看到 test-nginx，状态为 Exited
```

### 清理命令

```bash
# 查看所有已停止的容器ID
docker ps -a --filter "status=exited" -q

# 删除所有已停止的容器
docker rm $(docker ps -a --filter "status=exited" -q)
```