[![image](https://img.shields.io/github/v/release/eritpchy/aliyundrive-webdav)](https://github.com/eritpchy/aliyundrive-webdav/releases)  [![image](https://img.shields.io/maven-central/v/net.xdow/aliyundrive-sdk-openapi)](https://central.sonatype.com/artifact/net.xdow/aliyundrive-sdk-openapi/)

说明
> [1.1.0版本](https://github.com/zxbu/aliyundrive-webdav/releases/tag/v1.1.0)支持阿里Teambition网盘的webdav协议

> 2.x版本仅支持阿里云盘, 不再维护Teambition网盘版本

> 3.x版本支持阿里云盘OpenApi

> 3.4.0及以上版本默认支持直连模式下载

> 4.0.0及以上版本支持web文件管理以及访问备份盘和资源库

目录
- [aliyundrive-webdav](#aliyundrive-webdav)
- [如何使用](#如何使用)
    - [Jar包运行](#jar包运行)
    - [容器运行](#容器运行)
    - [Docker-Compose](#docker-compose)
    - [Kubernetes](#Kubernetes)
- [连接说明](#连接说明)
- [参数说明](#参数说明)
- [QQ群](#qq群)
- [新手教程](#新手教程)
- [客户端兼容性](#客户端兼容性)
- [浏览器获取refreshToken方式](#浏览器获取refreshtoken方式仅webapi需要)
- [功能说明](#功能说明)
    - [支持的功能](#支持的功能)
    - [注意事项](#注意事项)
- [免责声明](#免责声明)

# aliyundrive-webdav
本项目实现了阿里云盘的webdav协议, 只需要简单的配置一下, 就可以让阿里云盘变身为webdav协议的文件服务器。
基于此, 你可以把阿里云盘挂载为Windows、Linux、Mac系统的磁盘, 可以通过NAS系统做文件管理或文件同步, 更多玩法等你挖掘


## 直接运行(Windows/Linux/macOS/Android)

[点击下载](https://file.xdow.net/aliyundriver)

- Windows
```powershell
aliyundrive-webdav-windows-amd64.exe
```
- Linux(X64)
```bash
./aliyundrive-webdav-linux-amd64
```
- Linux(ARM64)
```bash
./aliyundrive-webdav-linux-arm64
```
- macOS(Intel)
> 最新的native编译工具链目前有问题, 请先用jar版替代
> 
> ~~./aliyundrive-webdav-darwin-x86_64~~
```bash
java -jar ./aliyundrive-webdav.jar
```
- WebApi
```bash
./aliyundrive-webdav-darwin-x86_64 --aliyundrive.driver=WebApi
```
## Jar包运行
> 建议自己下载源码编译, 以获得最新代码
```bash
java -jar webdav.jar
```
## 容器运行
<details>
  <summary>点击展开</summary>
  <pre>mkdir $(pwd)/conf
docker run -d \
  --name=aliyundrive-webdav \
  --restart=always -p 8080:8080  \
  -v /etc/localtime:/etc/localtime \
  -v $(pwd)/conf:/conf \
  -e TZ="Asia/Shanghai" \
  -e ALIYUNDRIVE_DRIVER=OpenApi \
  -e ALIYUNDRIVE_DOWNLOAD_PROXY_MODE=Auto \
  -e ALIYUNDRIVE_REFRESH_TOKEN="your refreshToken" \
  -e ALIYUNDRIVE_AUTH_PASSWORD="admin" \
  eritpchy/aliyundrive-webdav</pre>

- /conf 挂载卷自动维护了最新的refreshToken, 建议挂载
- ALIYUNDRIVE_AUTH_PASSWORD 是admin账户的密码, 建议修改
</details>

## Docker-Compose
<details>
  <summary>点击展开</summary>
  <pre>version: "3.0"
services:
  aliyundrive-webdav:
    image: eritpchy/aliyundrive-webdav
    container_name: aliyundriver
    environment:
      - TZ=Asia/Shanghai
      - ALIYUNDRIVE_DRIVER=OpenApi
      - ALIYUNDRIVE_DOWNLOAD_PROXY_MODE=Auto
      - ALIYUNDRIVE_REFRESH_TOKEN=refreshToken
      - ALIYUNDRIVE_AUTH_USER_NAME=admin
      - ALIYUNDRIVE_AUTH_PASSWORD=admin
    volumes:
      - ./docker/conf:/conf
    ports:
      - 6666:8080
    restart: always</pre>

- "refreshToken"请根据下文说明自行获取。
- "ALIYUNDRIVE_AUTH_USER-NAME"和"ALIYUNDRIVE_AUTH_PASSWORD"为连接用户名和密码, 建议更改。
- "./docker/conf/:/conf", 可以把冒号前改为指定目录, 比如"/homes/USER/docker/alidriver/:/conf"。
- 删除了"/etc/localtime:/etc/localtime", 如有需要同步时间请自行添加在environment下。
- 端口6666可自行按需更改, 此端口为WebDAV连接端口,8080为容器内配置端口, 修改请量力而为。
- 建议不要保留这些中文注释, 以防报错, 比如QNAP。
</details>

## Kubernetes
<details>
  <summary>点击展开</summary>
  <pre># 参考根目录内中的[k8s_app.yaml](k8s_app.yaml), 需要文件中修改container的环境变量值。  
# use this to deploy in truenas scale
sudo k3s kubectl apply -f k8s_app.yaml
# or other k8s cluster
sudo kubectl apply -f k8s_app.yaml</pre>
</details>

## 连接说明
### 4.0.0及以上版本
文件管理: http://{ip地址}:{端口号}/

Webdav: http://{ip地址}:{端口号}/dav

例如: http://127.0.0.1:8080/dav

注意: Webdav路径为 /dav 必须填写

### 4.0.0之前版本
Webdav: http://{ip地址}:{端口号}

## 参数说明
```bash
--aliyundrive.refresh-token
    阿里云盘的refreshToken, 获取方式见下文
--server.port
    非必填, 服务器端口号, 默认为8080
--aliyundrive.auth.enable=true
    是否开启Webdav账户验证, 默认开启
--aliyundrive.auth.user-name=admin
    Webdav账户, 默认admin
--aliyundrive.auth.password=admin
    Webdav密码, 默认admin
--aliyundrive.work-dir=./conf
    token挂载路径, 如果在同一个路径多开, 需修改此配置
--aliyundrive.driver=OpenApi
    驱动引擎, 默认官方OpenApi, 可选WebApi
--aliyundrive.download-proxy-mode=Auto
    文件下载模式, 默认Auto, 自动模式, 默认直连模式, 客户端不支持直连模式时使用代理模式
    可选Direct, 强制直连模式, 使用此模式, 一些客户端不兼容, 将会直接报错400,302,403等错误, 详见 '客户端兼容性'
    可选Proxy, 代理模式, 文件下载由程序中转, 3.3.0以前版本默认模式, 如遇问题或报上述错误可尝试使用Proxy模式
    
```

## SDK使用
```gradle
//依赖
compileOnly "org.projectlombok:lombok:1.18.26"
annotationProcessor "org.projectlombok:lombok"
implementation "com.squareup.okhttp3:okhttp:3.12.13" //api19
implementation "com.squareup.okhttp3:logging-interceptor:3.12.13" //api19
implementation "com.google.code.gson:gson:2.8.9"

//主要
implementation "net.xdow:aliyundrive-sdk-openapi:2.0.5"
implementation "net.xdow:aliyundrive-sdk-webapi:2.0.5"

//可选
implementation "net.xdow:webdav:2.0.5"
implementation "net.xdow:webdav-jakarta:2.0.5"
implementation "net.xdow:webdav-javax:2.0.5"
implementation "net.xdow:aliyundrive-webdav-internal:2.0.5"
implementation "net.xdow:aliyundrive-android-core:2.0.5"
implementation "net.xdow:jap-http:2.0.5"
implementation "net.xdow:jap-http-jakarta-adapter:2.0.5"
implementation "net.xdow:jap-http-javax-adapter:2.0.5"
```
## SDK基础用法
```java
AliyunDrive.newAliyunDrive()
```

# QQ群
<img src="https://github.com/eritpchy/aliyundrive-webdav/assets/8630635/f7242176-dd43-40c5-80a7-7657b53f4be4" alt="QQ交流群: [789738128]" width="500">

## 新手教程
![imaage](./doc/img/openapi_login.gif)

## 客户端兼容性
| 客户端           |               下载 |         上传         |                           备注                            |
|:--------------|-----------------:|:------------------:|:-------------------------------------------------------:|
| 群辉Cloud Sync  |             代理模式 | :white_check_mark: |                        建议使用单向同步                         | 
| Rclone        |     :rocket:直连模式 | :white_check_mark: |             推荐, 支持各个系统, 直连模式需要添加参数,<br/>见下方配置说明             |
| Mac原生         |     :rocket:直连模式 | :white_check_mark: |                                                         | 
| Transmit      |     :rocket:直连模式 | :white_check_mark: |                                                         | 
| Windows原生     |     :rocket:直连模式 | :white_check_mark: |         不推荐!有4GB文件传输限制,首次使用还需配置http,<br/>见下方'注意事项'          |
| RaiDrive      |     :rocket:直连模式 | :white_check_mark: |                     Windows平台下建议用这个                     |
| WinSCP 6.1.1+ |     :rocket:直连模式 | :white_check_mark: |                    6.1.1以下版本不支持直连模式                     |
| Fileball      | ~~:rocket:直连模式~~ | :white_check_mark: |                                                         |
| nPlayer       |     :rocket:直连模式 | :white_check_mark: |                           推荐                            |
| MT管理器         |     :rocket:直连模式 | :white_check_mark: |                           推荐                            |
| ES文件浏览器       |     :rocket:直连模式 | :white_check_mark: |                                                         |
| Kodi 20.0+    | ~~:rocket:直连模式~~ | :white_check_mark: |                  ~~2023年后编译版本可用直连模式~~                   |
| IINA 2.0.1+   |     :rocket:直连模式 | :white_check_mark: |                        macOS 推荐                         |
| MXPlayer      |     :rocket:直连模式 | :white_check_mark: |                                                         |
| jetAudio      |     :rocket:直连模式 | :white_check_mark: | 受jetAudio限制,端口号只能为80(http) 或 443(https),<br/>安卓端不可直接监听以上端口! |
| VLC           | ~~:rocket:直连模式~~ | :white_check_mark: |                                                         |
| Zotero        |     :rocket:直连模式 | :white_check_mark: |                                                         |

- 所有客户端均默认支持代理模式
- ~~:rocket:直连模式~~: 由于阿里云盘目前直链有效期为15分钟, 部分播放器遇阿里云盘链接失效不会主动回webdav请求, 常见表现为播放停止, 无法拖动进度条, 中途直接切换下一集等等, 以上有标注的播放器均默认禁用直连模式, 普通连续下载文件不受影响
- 请勿使用超过8个线程对直链进行下载, 大概率封号
- 请勿分享直链, 永封

## Rclone 配置说明
- Rclone 1.62.2及以下版本应选择Vendor为Nextcloud以支持rclone自身的数据校验功能
- Rclone 1.63.0及以上版本(目前为beta版本, [点击前往下载beta版](https://beta.rclone.org/)) 请选择Vendor为Fastmail Files, 如选择Vendor为Nextcloud, 则advanced config中nextcloud_chunk_size应设置为0, 否则使用时报错
- Vendor 选择为Other无数据校验功能
- Vendor 选择为Owncloud, 因Rclone本身只校验md5无数据校验功能
- 直连模式需要在Rclone 命令行参数添加 --header="Referer:", 否则报错403
```shell
例如: R:\rclone1.63.0.exe --header="Referer:" copy test:/demo/demo.mkv R:/test
```

## 浏览器获取refreshToken方式(仅WebApi需要)
<details>
  <summary>方式1</summary>
1. 先通过浏览器（建议chrome）打开阿里云盘官网并登录：https://www.aliyundrive.com/drive/
2. 登录成功后, 按F12打开开发者工具, 点击Application, 点击Local Storage, 点击 Local Storage下的 [https://www.aliyundrive.com/](https://www.aliyundrive.com/), 点击右边的token, 此时可以看到里面的数据, 其中就有refresh_token, 把其值复制出来即可。（格式为小写字母和数字, 不要复制双引号。例子：ca6bf2175d73as2188efg81f87e55f11）
3. 第二步有点繁琐, 大家结合下面的截图就看懂了
   ![image](https://user-images.githubusercontent.com/32785355/119246278-e6760880-bbb2-11eb-877c-aca16cf75d89.png)
</details>
<details>
  <summary>方式2</summary>

1. 先通过浏览器（建议chrome）打开阿里云盘官网并登录：https://www.aliyundrive.com/drive/
2. 登录成功后, 在地址栏输入 javascript:
   ![imgage](./doc/img/step1.jpg)
3. 粘贴下列代码到javascript: 后面,然后按回车键
   ![image](./doc/img/step2.jpg)
   弹窗
   ![image](./doc/img/step3.jpg)
 ```javascript
var p=document.createElement('p');p.style='text-align:center;margin-top:30px';p.innerHTML='refresh_token: <span style="color:red;">'+JSON.parse(localStorage.getItem('token')).refresh_token+'</span>';var win=window.open('','_blank','width=800,height=100');win.document.body.appendChild(p);
```
同时, 也可以将上述代码组合为
 ```javascript
javascript:var p=document.createElement('p');p.style='text-align:center;margin-top:30px';p.innerHTML='refresh_token: <span style="color:red;">'+JSON.parse(localStorage.getItem('token')).refresh_token+'</span>';var win=window.open('','_blank','width=800,height=100');win.document.body.appendChild(p);
```
添加为浏览器书签, 在https://www.aliyundrive.com/drive/ 页面点击该书签也会弹出refresh_token弹窗
</details>

## 功能说明
1. 查看文件夹、查看文件
2. 文件移动目录
3. 文件重命名
4. 文件下载
5. 文件删除
6. 文件上传（支持大文件自动分批上传）
7. 支持超大文件上传（官方限制30G）
8. 支持Webdav权限校验（默认账户密码：admin/admin）
9. 文件下载断点续传
10. Webdav下的流媒体播放等功能
11. 支持文件名包含 `/` 字符
12. 数据校验

## 注意事项
1. 移动文件到其他目录的同时, 修改文件名。比如 /a.zip 移动到 /b/a1.zip, 是不支持的
2. 文件上传断点续传
3. 部分客户端兼容性不好
4. 由于http协议在公网上明文传输密码, 部署在公网切记要开https, 否则不安全, 用宝塔反代即可
<details>
  <summary>5. Windows提示无法访问</summary>
  <pre>注册表: HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\WebClient\Parameters
BasicAuthLevel 改为2, 改完重启计算机或WebClient服务</pre>
</details>
<details>
  <summary>6. Windows提示文件大小超过允许的限制，无法保存</summary>
  <pre>注册表: HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\WebClient\Parameters
FileSizeLimitInBytes 改为FFFFFFFF, 也就是最大4GB限制, 改完重启计算机或WebClient服务
其他教程: <a href="http://blog.51yip.com/linux/2221.html" target="_blank">文件大小超过允许的限制，无法保存</a></pre>
</details>

7. 直连模式下播放停止, 无法拖动进度条, 中途直接切换下一集等等, 详见[客户端兼容性](#客户端兼容性)
8. 部分设备浏览器内核较老(比如索尼电视), 程序启动时会自动加载浏览器内核, 如无法加载, 请根据您的设备类型选择其中一个版本手动下载并安装, 下载地址: [镜像链接](https://file.xdow.net/aliyundriver/浏览器内核包) 或 [
Android System WebView](https://www.apkmirror.com/uploads/?appcategory=android-system-webview)

## 免责声明
1. 本软件为免费开源项目, 无任何形式的盈利行为。
2. 本软件服务于阿里云盘, 旨在让阿里云盘功能更强大。如有侵权, 请与我联系, 会及时处理。
3. 本软件皆调用官方接口实现, 无任何"Hack"行为, 无破坏官方接口行为。
4. 本软件仅做流量转发, 不拦截、存储、篡改任何用户数据。
5. 严禁使用本软件进行盈利、损坏官方、散落任何违法信息等行为。
6. 本软件不作任何稳定性的承诺, 如因使用本软件导致的文件丢失、文件破坏等意外情况, 均与本软件无关。
