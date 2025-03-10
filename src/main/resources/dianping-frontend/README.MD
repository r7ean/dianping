## 本地运行

如果仅在本地运行，请先修改配置:   
`./conf/nginx.conf`
```conf
location / {
    root html/hmdp;
    # root   /usr/share/nginx/html/hmdp;
    index  index.html index.htm;
}
...
location /api {  
    ...
    proxy_pass http://localhost:8081;
    # proxy_pass http://host.docker.internal:8081;
    #proxy_pass http://backend;
}
...
upstream backend {
    server localhost max_fails=5 fail_timeout=10s weight=1;
    # server host.docker.internal max_fails=5 fail_timeout=10s weight=1;
    #server 127.0.0.1:8082 max_fails=5 fail_timeout=10s weight=1;
}  
```
双击运行`nginx.exe`文件。

访问`http://localhost:8080/`

## Docker 运行

请先修改配置, 以挂载卷:  

```yaml
...

volumes:
  - {绝对路径}\conf\nginx.conf:/etc/nginx/nginx.conf
  - {绝对路径}\conf:/etc/nginx/conf.d
  - {绝对路径}\html:/usr/share/nginx/html

...
```

在当前目录下运行命令: `docker-compose up -d`  

访问`http://{dockerhost}:8080/`