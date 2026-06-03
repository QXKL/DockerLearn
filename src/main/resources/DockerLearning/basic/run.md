## docker run

`docker run` 用于**创建并启动一个新容器**。这是最常用的命令之一。

### 基本语法

```bash
docker run [选项] 镜像名 [命令] [参数]
```

### 注意: run命令基本上都能写成compose配置文件，下面示例里面会同步给出

### 常用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-d` | 后台运行（detach） | `docker run -d nginx` |
| `-it` | 交互式运行（i=交互，t=终端） | `docker run -it ubuntu bash` |
| `--name` | 指定容器名称 | `docker run --name my-web nginx` |
| `-p` | 端口映射（主机:容器） | `docker run -p 8080:80 nginx` |
| `-v` | 挂载卷（主机:容器） | `docker run -v /data:/app/data nginx` |
| `-e` | 设置环境变量 | `docker run -e MY_VAR=value nginx` |
| `--rm` | 容器停止后自动删除 | `docker run --rm nginx` |
| `--restart` | 重启策略 | `docker run --restart=always nginx` |

### 实战示例

```bash
# 1. 最简单的运行（前台运行，Ctrl+C 退出）
docker run nginx

# 2. 后台运行 nginx
docker run -d nginx

# 3. 后台运行并指定名称和端口映射
docker run -d --name my-nginx -p 8080:80 nginx
# 访问 http://localhost:8080 就能看到 nginx 页面

# 4. 交互式运行 Ubuntu（会进入容器内部）
docker run -it --name my-ubuntu ubuntu bash
# 退出：输入 exit 或 Ctrl+D

# 5. 一次性容器（执行完就删除）
docker run --rm -it alpine echo "Hello World"

# 6. 带环境变量的容器
docker run -d -e MYSQL_ROOT_PASSWORD=123456 --name my-mysql mysql

# 7. 挂载本地目录
docker run -d -v $(pwd)/html:/usr/share/nginx/html -p 8080:80 nginx
```

### 几种运行模式对比

```bash
# 前台运行（阻塞终端）
docker run nginx
# 输出日志到终端，Ctrl+C 停止容器

# 后台运行（daemon）
docker run -d nginx
# 返回容器ID，不阻塞终端

# 交互式运行
docker run -it ubuntu bash
# 进入容器 shell，exit 退出时容器停止

# 交互式 + 后台（不太常用）
docker run -dit ubuntu
# 后台运行但可以随时 attach 进去
```

### 常见场景

#### 场景1：运行一个 Web 服务
```bash
docker run -d \
  --name my-web \
  -p 80:80 \
  -v /var/www:/usr/share/nginx/html \
  --restart=always \
  nginx
```

```bash
docker run -d --name my-web -p 80:80 -v /var/www:/usr/share/nginx/html --restart=always nginx
```

```yaml
service:
  web:
      container_name: my-web
      ports: 
        - "80:80"
      volumes:
        - /var/www:/usr/share/nginx/html
      restart: always
      image: nginx
```

#### 场景2：运行数据库
```bash
docker run -d \
  --name postgres-db \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_USER=admin \
  -e POSTGRES_DB=mydb \
  -p 5432:5432 \
  -v pgdata:/var/lib/postgresql/data \
  postgres:13
```

```bash
docker run -d --name postgres-db -e POSTGRES_PASSWORD=secret -e POSTGRES_USER=admin -e POSTGRES_DB=mydb -p 5432:5432 -v pgdata:/var/lib/postgresql/data postgres:13
```

```yaml
services:
  postgres-db:
    container_name: postgres-db
    environment:
      POSTGRES_PASSWORD: secret
      POSTGRES_USER: admin
      POSTGRES_DB: mydb
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    image: postgres:13
```

#### 场景3：测试性运行（用完即删）
```bash
# 测试某个命令
docker run --rm alpine cat /etc/os-release

# 测试 MySQL 连接
docker run --rm -it mysql mysql -h 192.168.1.100 -u root -p
```

```yaml
service:
  test:
    image: alpine
    command: cat /etc/os-release
```

```yaml
service:
  mysql:
    container_name: mysql
    image: mysql
    command: mysql -h 192.168.1.100 -u root -p
```


### 常见问题

```bash
# Q: 容器退出了怎么办？
docker ps -a              # 查看退出原因
docker logs 容器名         # 查看日志

# Q: 如何进入后台运行的容器？
docker exec -it 容器名 bash   # 推荐
docker attach 容器名          # 不推荐（退出会停止容器）

# Q: 端口被占用怎么办？
# 换一个主机端口
docker run -d -p 8081:80 nginx
# 或者使用随机端口
docker run -d -P nginx    # 大写 P 随机映射端口
```