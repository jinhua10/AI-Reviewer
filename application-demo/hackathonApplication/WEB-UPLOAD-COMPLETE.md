# Web上传功能实现完成总结

## ✅ 已完成的功能

### 1. 账户管理服务 (AccountService)
**文件位置**: `src/main/java/top/yumbo/ai/application/hackathon/web/service/AccountService.java`

**功能**:
- 从account.csv加载团队账户信息
- 验证access_code
- 返回团队信息(team_id, lead_email, access_code)

**使用方法**:
```java
accountService.loadAccounts("/path/to/project/root");
TeamAccount account = accountService.validateAccessCode("secret123");
```

### 2. 文件上传服务 (FileUploadService)  
**文件位置**: `src/main/java/top/yumbo/ai/application/hackathon/web/service/FileUploadService.java`

**功能**:
- 上传ZIP文件到{team_id}/文件夹
- 创建done.txt标记文件
- 检查done.txt是否存在
- 列出已上传的ZIP文件

**使用方法**:
```java
fileUploadService.setProjectRootPath("/path/to/project/root");
fileUploadService.uploadZipFile("T00001", multipartFile);
fileUploadService.createDoneFile("T00001");
```

### 3. Web控制器 (UploadController)
**文件位置**: `src/main/java/top/yumbo/ai/application/hackathon/web/controller/UploadController.java`

**功能**:
- `/` - 登录页面
- `/login` - 处理登录请求
- `/upload` - 文件上传页面(需要登录)
- `/upload` (POST) - 处理文件上传
- `/done` (POST) - 标记提交完成
- `/logout` - 退出登录

**特性**:
- Cookie认证(7天有效期)
- 自动团队文件夹创建
- 文件大小限制(500MB)

### 4. HTML页面

#### login.html
**文件位置**: `src/main/resources/templates/login.html`

**特性**:
- 现代渐变设计
- access_code输入
- 错误/成功消息显示
- 响应式布局

#### upload.html  
**文件位置**: `src/main/resources/templates/upload.html`

**特性**:
- 拖放文件上传
- 已上传文件列表
- "标记为完成"按钮
- 团队信息显示
- 完成状态徽章

### 5. 配置更新

**pom.xml** - 添加了依赖:
- spring-boot-starter-web
- spring-boot-starter-thymeleaf  
- commons-fileupload

**application.yml** - 添加了配置:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB
server:
  port: 8080
```

### 6. 自动集成

**HackathonAutoConfiguration.java** - 已更新:
- 添加AccountService和FileUploadService作为Bean参数
- 在--reviewAll模式下自动初始化web服务
- 加载account.csv
- 设置项目根路径

## 📋 使用指南

### 管理员设置

1. **创建account.csv文件**

在--reviewAll指定的目录下创建account.csv:

```csv
team_id,lead_email,access_code
T00001,team1@example.com,demo123
T00002,team2@example.com,test456
T00003,team3@example.com,pass789
```

2. **启动应用**

```bash
java -jar hackathonApplication.jar --reviewAll=/home/jinhua/hackathon2025-project-artifacts
```

3. **访问Web界面**

```
http://localhost:8080
```

### 团队使用流程

1. **登录**
   - 打开浏览器访问 `http://your-server:8080`
   - 输入团队的access_code
   - 点击"Login"

2. **上传项目**
   - 点击"Click to select ZIP file"或拖放文件
   - 选择项目ZIP文件(最大500MB)
   - 点击"Upload File"
   - 可以上传多个ZIP文件

3. **标记完成**
   - 上传完所有文件后,点击"Mark as Done"
   - 系统会在团队文件夹下创建done.txt
   - AI审查系统会在下次扫描(2分钟)时开始评分

4. **退出**
   - 点击"Logout"退出登录

## 🔧 工作原理

### 文件存储结构

```
/home/jinhua/hackathon2025-project-artifacts/
├── account.csv                    # 团队账户文件
├── review_results.csv            # AI评分结果
├── T00001/                       # 团队文件夹
│   ├── project.zip              # 上传的文件
│   └── done.txt                 # 完成标记
├── T00002/
│   ├── backend.zip
│   ├── frontend.zip  
│   └── done.txt
```

### 工作流程

1. **用户上传** → ZIP文件保存到 `{team_id}/` 文件夹
2. **点击Done** → 创建 `{team_id}/done.txt`
3. **AI扫描** → HackathonAIEngineV2每2分钟扫描一次
4. **发现done.txt** → 解压ZIP并进行AI评分
5. **记录结果** → 分数和评论保存到review_results.csv
6. **生成报告** → 创建markdown报告文件

## 🌟 功能特点

✅ **安全认证** - Cookie-based,7天有效期
✅ **团队隔离** - 每个团队独立文件夹
✅ **多文件支持** - 可上传多个ZIP文件
✅ **拖放上传** - 现代化拖放界面
✅ **文件大小限制** - 500MB上限
✅ **自动done.txt** - 一键标记完成  
✅ **状态显示** - 实时显示上传状态
✅ **响应式设计** - 支持移动设备
✅ **自动集成** - 与现有批处理系统无缝集成
✅ **CSV记录** - 自动记录评分结果

## 📝 配置说明

### 修改端口

在application.yml中修改:
```yaml
server:
  port: 8080  # 改为你想要的端口
```

### 修改文件大小限制

在application.yml中修改:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 500MB     # 修改最大文件大小
      max-request-size: 500MB  # 修改最大请求大小
```

### 修改Cookie有效期

在UploadController.java中修改:
```java
private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 改为需要的秒数
```

## 🚀 部署建议

### Ubuntu生产环境

1. **安装Java**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

2. **创建服务**
```bash
sudo nano /etc/systemd/system/aireviewer.service
```

内容:
```ini
[Unit]
Description=AI Reviewer Service
After=network.target

[Service]
Type=simple
User=jinhua
WorkingDirectory=/home/jinhua/AI-Reviewer
ExecStart=/usr/bin/java -jar hackathonApplication.jar --reviewAll=/home/jinhua/hackathon2025-project-artifacts
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

3. **启动服务**
```bash
sudo systemctl daemon-reload
sudo systemctl enable aireviewer
sudo systemctl start aireviewer
sudo systemctl status aireviewer
```

### Nginx反向代理(HTTPS)

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 文件上传大小限制
        client_max_body_size 500M;
    }
}
```

## 📖 文档文件

已创建的文档:
- `README-WebUpload.md` - 详细使用文档
- `IMPLEMENTATION-STATUS.md` - 实现状态说明
- `account.csv.example` - 示例账户文件
- 本文件 - 完整实现总结

## ✨ 总结

Web上传功能已完全实现并集成到现有系统中。团队可以通过友好的Web界面上传项目,系统会自动进行AI评分并生成报告。整个流程无缝衔接,无需管理员手动干预。

**核心优势**:
- 🎯 用户友好的Web界面
- 🔒 安全的访问控制
- 📁 自动文件管理
- 🤖 与AI评分系统集成
- 📊 自动结果记录
- 🔄 持续监控扫描

现在团队可以随时通过浏览器上传项目,AI系统会自动在后台进行评分!

