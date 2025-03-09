## 本地运行

如果仅在本地运行，直接在当前目录下运行nginx.exe文件。  

访问`http://localhost:8080/`

## Docker 运行

在当前目录下运行命令: `docker build -t name .`

然后运行命令: 
```powershell
docker run -d -p 8080:8080 --name dianping-frontend \
  -v "{绝对路径}\dianping-frontend\conf\nginx.conf:/etc/nginx/nginx.conf" \
  -v "{绝对路径}\dianping\src\main\resources\dianping-frontend\html:/usr/share/nginx/html" \
  dianping-frontend:latest
```

访问`http://{dockerhost}:8080/`