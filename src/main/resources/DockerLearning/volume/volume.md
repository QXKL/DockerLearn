## docker volume

数据卷（Volume）用于**持久化和共享容器数据**。容器删除后，数据卷依然保留。

### 为什么需要 Volume

```bash
# 问题：容器删除，数据丢失
docker run --name test alpine touch /data.txt
docker rm test
# /data.txt 丢失了 ❌

# 解决：使用 Volume
docker run -v mydata:/data --name test alpine touch /data/test.txt
docker rm test
# 数据还在 mydata 卷中 ✅
```

---

### 三种数据挂载方式

| 类型 | 语法 | 管理方 | 适用场景 |
|------|------|--------|---------|
| **Volume（命名卷）** | `-v 卷名:/容器路径` | Docker | 生产环境，数据持久化 |
| **Bind Mount（绑定挂载）** | `-v 宿主机路径:/容器路径` | 用户 | 开发环境，配置文件 |
| **tmpfs** | `--tmpfs /容器路径` | Docker | 临时数据，不需要持久化 |

```
Volume（命名卷）
┌─────────────────────────────────────┐
│  Docker 管理区域（/var/lib/docker/volumes/）│
│  ┌─────────┐  ┌─────────┐           │
│  │ mydata  │  │ db-data │           │
│  └─────────┘  └─────────┘           │
└─────────────────────────────────────┘
         ↑              ↑
    容器A:/data    容器B:/var/lib/mysql

Bind Mount
宿主机 /home/user/project  ←→  容器 /app
（直接映射，双向同步）
```

---

### 常用命令

```bash
# 查看卷
docker volume ls
docker volume ls -f "dangling=true"   # 过滤无主卷

# 创建卷
docker volume create mydata

# 查看卷详情
docker volume inspect mydata

# 删除卷
docker volume rm mydata
docker volume prune          # 删除所有未使用的卷
docker volume prune -f       # 不确认

# 查看卷使用情况
docker system df
```

---

### Volume 实战

#### 创建和使用 Volume

```bash
# 1. 创建卷
docker volume create postgres-data

# 2. 使用卷运行容器
docker run -d \
  --name postgres \
  -v postgres-data:/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD=123456 \
  postgres:13

# 3. 查看卷
docker volume inspect postgres-data
# [
#   {
#     "Name": "postgres-data",
#     "Mountpoint": "/var/lib/docker/volumes/postgres-data/_data",
#     ...
#   }
# ]

# 4. 删除容器，卷还在
docker rm -f postgres
docker volume ls | grep postgres-data   # 还在

# 5. 新容器复用旧数据
docker run -d --name postgres-new -v postgres-data:/var/lib/postgresql/data -e POSTGRES_PASSWORD=123456 postgres:13
# 数据恢复了 ✅
```

#### 匿名卷

```bash
# 不指定卷名，Docker 自动生成随机名
docker run -d -v /var/lib/mysql mysql
# 查看匿名卷
docker volume ls
# DRIVER    VOLUME NAME
# local     2b3f4a5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a

# 缺点：容器删除后难以找到匿名卷
docker rm mysql-container  # 匿名卷变成 dangling volume
docker volume prune        # 清理无主匿名卷
```

---

### Bind Mount 实战

```bash
# 挂载当前目录到 /app
docker run -d -v $(pwd):/app node:18 npm start

# 挂载配置文件（只读）
docker run -d -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro nginx

# 挂载多个路径
docker run -d \
  -v $(pwd)/src:/app/src \
  -v $(pwd)/logs:/var/log/app \
  --name dev-app \
  node:18

# Windows 路径（使用绝对路径）
docker run -v D:/project:/app node:18
```

---

### Volume vs Bind Mount 选择

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 数据库持久化 | Volume | Docker 管理，性能好 |
| 配置文件 | Bind Mount | 方便修改，实时生效 |
| 代码开发 | Bind Mount | 源码改动立即同步 |
| 备份恢复 | Volume | 路径固定，易操作 |
| 多容器共享 | Volume | 命名卷更清晰 |
| 临时数据 | tmpfs | 不写磁盘，速度快 |

```bash
# 开发环境：Bind Mount
docker run -v $(pwd):/app -v /app/node_modules node:18

# 生产环境：Volume
docker run -v db-data:/var/lib/mysql mysql
```

---

### 多容器共享数据

#### 共享 Volume

```bash
# 创建共享卷
docker volume create shared-data

# 容器A（写入）
docker run -d --name writer -v shared-data:/data alpine \
  sh -c "while true; do echo $(date) >> /data/log.txt; sleep 1; done"

# 容器B（读取）
docker run -it --name reader -v shared-data:/data alpine cat /data/log.txt

# 查看共享数据
docker exec reader cat /data/log.txt
```

#### --volumes-from（继承卷）

```bash
# 创建数据容器（不运行业务）
docker create --name db-data -v postgres-data:/var/lib/postgresql/data alpine

# 其他容器继承卷
docker run -d --name postgres1 --volumes-from db-data postgres:13
docker run -d --name postgres2 --volumes-from db-data postgres:13

# 数据容器只是引用，删除不影响真实数据
docker rm db-data
# postgres1、postgres2 仍然可以访问数据
```

---

### 备份与恢复

#### 备份 Volume

```bash
# 方式1：使用临时容器打包
docker run --rm \
  -v mydata:/data \
  -v $(pwd):/backup \
  alpine \
  tar czf /backup/mydata-backup.tar.gz -C /data .

# 方式2：直接复制（需要知道挂载点）
docker volume inspect mydata --format='{{.Mountpoint}}'
sudo cp -r /var/lib/docker/volumes/mydata/_data ./backup/
```

#### 恢复 Volume

```bash
# 从备份文件恢复
docker run --rm \
  -v mydata:/data \
  -v $(pwd):/backup \
  alpine \
  tar xzf /backup/mydata-backup.tar.gz -C /data

# 验证
docker run --rm -v mydata:/data alpine ls -la /data
```

---

### Volume 驱动

#### 本地驱动（默认）

```bash
# 默认 local 驱动，存储在 /var/lib/docker/volumes/
docker volume create local-volume

# 指定驱动选项（如 NFS）
docker volume create \
  --driver local \
  --opt type=nfs \
  --opt o=addr=192.168.1.100,rw \
  --opt device=:/export/nfs \
  nfs-volume
```

#### 第三方驱动

| 驱动 | 说明 |
|------|------|
| `nfs` | 网络文件系统 |
| `azurefile` | Azure 文件存储 |
| `aws-ebs` | AWS EBS 块存储 |
| `vsphere` | VMware vSphere |

---

### 清理 Volume

```bash
# 查看卷使用情况
docker system df -v

# 删除未使用的卷
docker volume prune
docker volume prune -f

# 删除特定卷
docker volume rm mydata

# 删除所有未使用的卷（危险）
docker volume prune -a

# 删除所有卷（包括在用，会报错）
docker volume rm $(docker volume ls -q)
```

---

### docker-compose 中的 Volume

```yaml
version: '3.8'

services:
  db:
    image: mysql:8.0
    container_name: mysql-db
    environment:
      MYSQL_ROOT_PASSWORD: 123456
    volumes:
      - db-data:/var/lib/mysql           # 命名卷
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql  # Bind Mount
      - ./conf/my.cnf:/etc/mysql/conf.d/my.cnf:ro  # 只读

  app:
    image: myapp
    volumes:
      - app-logs:/var/log/app
      - shared-data:/data

  backup:
    image: alpine
    volumes:
      - db-data:/db-data:ro              # 只读挂载
      - shared-data:/shared-data
    command: sh -c "tar czf /backup/db-backup.tar.gz /db-data"

volumes:
  db-data:          # 命名卷声明
    driver: local
    name: myproject-db-data  # 自定义卷名

  app-logs:
    external: true   # 使用外部已存在的卷

  shared-data:
    name: shared-data

# 查看 compose 创建的卷
# docker volume ls | grep myproject
```

---

### 常见问题

```bash
# Q1: 容器内没有权限写入挂载的文件
# 解决：指定用户 ID
docker run -u 1000:1000 -v $(pwd):/app node:18

# Q2: Bind Mount 在 Windows 上路径问题
# 使用正斜杠或双反斜杠
docker run -v D:/project:/app
docker run -v D:\\project:/app

# Q3: 查看容器使用了哪些卷
docker inspect 容器名 --format='{{.Mounts}}'

# Q4: 容器删除时自动删除卷
docker run --rm -v mytemp:/data alpine  # --rm 不影响卷
docker run -v mytemp:/data alpine       # 卷仍然存在

# 要自动删除卷需要手动
docker run --rm -v /data alpine  # 匿名卷会被删除
```

---

### 最佳实践

```bash
# ✅ 推荐：命名卷 + 定期备份
docker volume create prod-data
docker run -v prod-data:/data myapp

# ✅ 推荐：开发用 Bind Mount
docker run -v $(pwd):/app -v /app/node_modules node:18

# ✅ 推荐：配置文件用 Bind Mount + 只读
docker run -v ./config:/app/config:ro myapp

# ❌ 避免：容器内路径写死但没挂载
docker run alpine touch /var/lib/mysql/test  # 数据会丢失

# ❌ 避免：生产环境用 Bind Mount 存数据库
docker run -v $(pwd)/data:/var/lib/mysql mysql  # 权限易出问题
```