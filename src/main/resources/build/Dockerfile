### Dockerfile

### 这是个运行脚本，用于定义镜像(Image).运行容器(Container)所需要的所有文件和配置。
### 严格来说，不能用于操作docker，只能用于制作镜像

# 基础镜像(环境)
FROM eclipse-temurin:21-jre
# 作者
LABEL authors="QX"

# 将同目录下的 DockerDemo-0.0.1-SNAPSHOT.jar 复制到容器中，并命名为 app.jar
COPY DockerDemo-0.0.1-SNAPSHOT.jar app.jar

# 容器每次启动时执行的命令
ENTRYPOINT ["java","-jar","/app.jar"]

### 制作好的镜像文件应该在docker里面了，使用docker ps查看
