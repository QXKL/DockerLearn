这一节是compose章节

初步理解compose，就是把 docker run 参数写进 docker-compose.yml。

## 1. 最小结构
```yaml
services:
  服务名:
  image: 镜像名

```
例如：
```yaml
services:
  redis:
    image: redis:latest
```

启动： 
```bash
docker compose up -d
```

停止并删除容器：
```bash
docker compose down
```

例：
```bash
docker run -d --name my-rb-container --hostname my-rabbit -p 5672:5672 -p 15672:15672 -v LearnRabbitMQVolume:/var/lib/rabbitmq rabbitmq:3-management
```

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: my-rb-container
    hostname: my-rabbit
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - LearnRabbitMQVolume:/var/lib/rabbitmq

volumes:
  LearnRabbitMQVolume:
```

类似的转换在之前的教学里也常有出现，复习时可以顺便看看。

## 字段对应关系
image           = 镜像
container_name  = 容器名
hostname        = 容器内部主机名
ports           = -p
volumes         = -v
environment     = -e
networks        = --network
depends_on      = 启动顺序依赖

## 加 Redis

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: my-rb-container
    hostname: my-rabbit
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - LearnRabbitMQVolume:/var/lib/rabbitmq

  redis:
    image: redis:latest
    container_name: my-redis
    ports:
      - "6379:6379"

volumes:
  LearnRabbitMQVolume:
```

## bash 命令
```bash
# 启动
docker-compose up -d

# 查看
docker compose ps

# 日志
docker compose logs rabbitmq
docker compose logs redis

# 实时日志
docker compose logs -f rabbitmq
```

## 加上网络
Compose 默认会给当前项目创建一个网络，所以通常不用手写。

这里还是写一下：
```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: my-rb-container
    hostname: my-rabbit
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - LearnRabbitMQVolume:/var/lib/rabbitmq
    networks:
      - learn-net

  redis:
    image: redis:latest
    container_name: my-redis
    ports:
      - "6379:6379"
    networks:
      - learn-net

volumes:
  LearnRabbitMQVolume:

networks:
  learn-net:
```

## Compose 管理命令
```bash
# 启动
docker compose up -d

# 停止并删除容器，默认保留 volume
docker compose down

# 查看当前 compose 项目的容器
docker compose ps

# 看某个服务日志
docker compose logs redis

# 重启某个服务
docker compose restart rabbitmq

# 进入某个服务容器
docker compose exec rabbitmq bash
```