## DockerDemo
个人学习docker的demo项目，包含教学与命令示例

### 话说在前头。这玩意不练不常用，是根本记不下来的。多写写吧，或者用项目里面的All in One

___

### 下面的东西在学完后再来看。

- 1 Dockerfile
用来构建应用镜像，并定义启动时运行的命令
app在开发完成后，maven clean package打包，然后通过Dockerfile构建镜像。 
这个镜像和其它依赖一同放在同一个容器下面，用Docker Compose启动。

#### 构建镜像时执行（只执行一次）
RUN apt-get update
COPY target/*.jar app.jar

#### 容器每次启动时执行（每次 docker start 都会执行）
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=prod"]

放什么？ ——> 放基础环境定义、原信息(label,如作者等)、构建镜像前需要执行的命令(如复制)、镜像(所在的容器)每次启动前需要执行的命令

- 2 Docker Compose
用来构建多容器应用
应用面更广，在这里用来启动包含app及其依赖服务的容器
如项目里的compose.yml

放什么？ ——> 放启动顺序(depends_on)、启动依赖、启动配置、启动命令等。多数配置和命令行的方式是一样的，不要和application.yml弄混了

- 3 spring-boot-docker-compose
自动发现 compose.yml 并启动依赖服务（mysql/redis）
这个在开发阶段，app没有镜像的时候，用来启动app的同时，启动其它docker依赖服务。
如果打包了app镜像，并和其它以来服务放在同一个容器里面，就不要用这个了。直接用Docker Compose

放什么？ ——> 和compose.yml差不多，但是少了app，应当仅包含依赖服务。并且会在spring boot启动/关闭时自动启动/关闭
PS：Spring Boot 会自动忽略 compose.yml 中与 app 同名的服务，所以即使写了也不会启动两个，只是最佳实践是分开。
PPS：该以来还能自动配置网络，让本地应用能通过 mysql、redis、rabbitmq 主机名访问容器。可以不用烦心docker网络的事