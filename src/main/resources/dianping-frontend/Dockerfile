# 使用官方 Nginx 镜像作为基础镜像
FROM nginx:1.18

# 将本地的 Nginx 配置文件复制到容器中
COPY conf/nginx.conf /etc/nginx/nginx.conf

# 将前端静态文件复制到容器中的 Nginx 默认静态文件目录
COPY html /usr/share/nginx/html

# 暴露 8080 端口
EXPOSE 8080

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]