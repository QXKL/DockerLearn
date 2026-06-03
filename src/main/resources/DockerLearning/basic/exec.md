## docker exec

`docker exec` 用于**在运行的容器中执行命令**。这是进入容器内部或执行管理命令最常用的方式。

### 基本语法

```bash
docker exec [选项] 容器名或ID 命令 [参数...]
```

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-i` | 交互模式，保持 STDIN 打开 | `docker exec -i my-app bash` |
| `-t` | 分配伪终端 | `docker exec -t my-app bash` |
| `-it` | 交互式终端（最常用） | `docker exec -it my-app bash` |
| `-d` | 后台运行命令 | `docker exec -d my-app ./script.sh` |
| `-e` | 设置环境变量 | `docker exec -e MY_VAR=value my-app env` |
| `-w` | 指定工作目录 | `docker exec -w /app my-app pwd` |
| `-u` | 指定用户执行 | `docker exec -u www-data my-app whoami` |

### 实战示例

```bash
# 1. 进入容器交互式 shell（最常用）
docker exec -it my-nginx bash
docker exec -it my-nginx sh      # Alpine 镜像用 sh
docker exec -it my-nginx /bin/bash

# 2. 在容器中执行单个命令
docker exec my-nginx ls -la
docker exec my-nginx cat /etc/os-release
docker exec my-nginx nginx -t     # 测试 nginx 配置

# 3. 以特定用户执行
docker exec -u root my-app whoami
docker exec -u 1000 my-app id

# 4. 设置环境变量执行
docker exec -e ENV=production my-app env

# 5. 在指定目录执行
docker exec -w /var/log my-nginx pwd

# 6. 后台执行命令
docker exec -d my-nginx touch /tmp/health-check

# 7. 执行多个命令
docker exec my-nginx sh -c "echo hello && ls -la"
```

### 进入容器的几种方式对比

```bash
# exec（推荐）
docker exec -it my-app bash
# 退出后容器继续运行 ✅

# attach（不推荐）
docker attach my-app
# 退出会停止容器 ⚠️ 按 Ctrl+P+Q 可安全退出

# 启动时进入（交互式运行）
docker run -it ubuntu bash
# 退出后容器停止

# SSH（过时，不推荐）
# 传统方式，容器不应运行 sshd
```

### 实际场景

#### 场景1：查看日志和调试
```bash
# 查看 nginx 访问日志
docker exec my-nginx cat /var/log/nginx/access.log

# 实时查看日志
docker exec my-nginx tail -f /var/log/nginx/error.log

# 检查进程
docker exec my-nginx ps aux

# 查看网络连接
docker exec my-nginx netstat -tunlp

# 测试配置文件
docker exec my-nginx nginx -t
```

#### 场景2：管理数据库
```bash
# MySQL 数据库操作
docker exec -it mysql-db mysql -u root -p
docker exec mysql-db mysql -u root -p123456 -e "SHOW DATABASES;"
docker exec mysql-db mysqldump -u root -p123456 mydb > backup.sql

# PostgreSQL
docker exec -it postgres-db psql -U postgres
docker exec postgres-db psql -U postgres -c "SELECT * FROM users;"

# Redis
docker exec -it redis-db redis-cli
docker exec redis-db redis-cli SET key value
```

#### 场景3：容器内文件操作
```bash
# 复制文件到容器（docker cp 更方便）
docker exec my-app sh -c "echo 'data' > /app/data.txt"

# 修改配置文件
docker exec -it my-app vi /etc/nginx/nginx.conf  # 需要 vi
# 或使用 cat 重写
echo "server_name example.com;" | docker exec -i my-app tee -a /etc/nginx/conf.d/default.conf

# 创建目录和文件
docker exec my-app mkdir -p /app/uploads
docker exec my-app chown www-data:www-data /app/uploads
```

#### 场景4：安装调试工具
```bash
# 进入容器
docker exec -it alpine-app sh

# 在容器内安装工具（临时调试）
apk add curl vim net-tools   # Alpine
apt-get update && apt-get install curl vim  # Ubuntu/Debian

# 注意：容器重启后工具会丢失
```

#### 场景5：健康检查和监控
```bash
# 检查服务是否正常
docker exec my-app curl -f http://localhost:8080/health

# 检查内存使用
docker exec my-app cat /proc/meminfo

# 检查磁盘
docker exec my-app df -h

# 组合检查脚本
docker exec my-app sh -c 'curl -f http://localhost:8080 && echo "OK"'
```

### docker-compose 对照

```bash
# docker 命令
docker exec -it my-nginx bash

# docker-compose 命令
docker-compose exec web bash          # 进入 web 服务容器
docker-compose exec -T web ls         # 不带伪终端（用于脚本）
docker-compose exec -w /app web pwd   # 指定工作目录
docker-compose exec --index=2 web bash # 进入第2个实例
```

```yaml
# docker-compose.yml
services:
  web:
    image: nginx
    container_name: my-nginx
  
  db:
    image: postgres
    environment:
      POSTGRES_PASSWORD: secret

# 使用示例：
# docker-compose exec web nginx -t
# docker-compose exec db psql -U postgres
# docker-compose exec web /bin/sh
```

### 常见容器操作

```bash
# Alpine 容器（最小镜像）
docker exec -it alpine-app sh      # 注意：bash 可能不存在

# Ubuntu/Debian
docker exec -it ubuntu-app bash

# CentOS/RHEL
docker exec -it centos-app bash

# 容器没有 shell？
docker exec -it my-app /bin/sh     # 尝试 sh
docker exec -it my-app ls          # 或者直接执行命令

# 查看容器可用的 shell
docker exec my-app cat /etc/shells
```

### 高级用法

```bash
# 1. 通过命名管道执行（避免历史记录）
echo "secret_command" | docker exec -i my-app bash

# 2. 执行本地脚本
cat local-script.sh | docker exec -i my-app bash

# 3. 交互式执行复杂命令
docker exec -it my-app bash -c "cd /app && ./deploy.sh"

# 4. 为所有容器执行命令
for container.md in $(docker ps -q); do
  docker exec $container hostname
done

# 5. 带超时的命令执行
timeout 5 docker exec my-app long-running-command

# 6. 使用命名容器执行一次性任务
docker exec my-app bash -c "backup.sh && echo 'Done'"
```

### 注意事项

```bash
# ⚠️ 容器必须正在运行
docker exec stopped-container.md bash
# Error: Container xxx is not running

# ⚠️ 容器内可能没有 bash
docker exec alpine-app bash
# OCI runtime exec failed: exec: "bash": executable file not found
# 解决：使用 sh

# ⚠️ 退出终端不会停止容器
docker exec -it my-app bash
exit  # 容器继续运行 ✅

# ⚠️ 环境变量问题
docker exec my-app env  # 查看当前环境变量
docker exec -e MY_VAR=value my-app env  # 传递环境变量

# ⚠️ 信号处理
docker exec my-app nginx -s reload  # 发送信号给 nginx
```

### 故障排查

```bash
# 问题：无法进入容器
docker ps | grep my-app  # 确认容器在运行
docker logs my-app       # 查看容器日志

# 问题：命令找不到
docker exec my-app which bash  # 查找命令位置
docker exec my-app ls /bin     # 查看可用命令

# 问题：权限不足
docker exec -u root my-app command  # 用 root 执行
docker exec -u 0 my-app command     # 同上

# 问题：交互式无响应
# 可能没有分配伪终端，加上 -t
docker exec -it my-app bash  # 确保有 -t
```