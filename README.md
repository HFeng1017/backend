# 个人简历管理系统

一个前后端分离的个人简历管理系统，支持简历编辑、项目文件管理、权限控制等功能，遵循阿里巴巴 Java 开发手册规范。

## 技术栈

### 后端
- Java 21
- Spring Boot 3.2.0
- MyBatis Plus 3.5.9
- MySQL 8.0
- Spring Security + JWT（jjwt 0.11.5）
- Redis
- Hutool 5.8.32
- Lombok

### 前端
- Vue 3.4
- Vite 5.0
- Element Plus 2.4
- Vue Router 4.2
- Axios 1.6

## 项目结构

```
resume-platform/
├── backend/                          # 后端项目
│   ├── src/main/java/com/resume/platform/
│   │   ├── common/                   # 通用层
│   │   │   ├── BusinessException.java    # 业务异常类
│   │   │   ├── ErrorCode.java           # 统一错误码枚举
│   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │   └── Result.java              # 统一返回结果封装
│   │   ├── config/                   # 配置层
│   │   │   ├── SecurityConfig.java     # Spring Security 安全配置
│   │   │   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   │   │   ├── DataInitializer.java     # 启动数据初始化器
│   │   │   ├── WebMvcConfig.java        # 静态资源映射配置
│   │   │   └── RedisConfig.java         # Redis 序列化配置
│   │   ├── controller/               # 控制器层
│   │   │   ├── AuthController.java      # 认证（登录/刷新Token/重置密码）
│   │   │   ├── ResumeController.java    # 简历管理
│   │   │   ├── ProjectController.java   # 项目文件管理（上传/下载/预览）
│   │   │   ├── PublicController.java    # 公开接口（游客可访问）
│   │   │   └── SystemConfigController.java  # 系统权限配置
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── LoginDTO.java
│   │   │   ├── ResumeSaveDTO.java
│   │   │   ├── SystemConfigUpdateDTO.java
│   │   │   ├── RefreshTokenDTO.java
│   │   │   ├── ResetPasswordDTO.java
│   │   │   └── SendResetCodeDTO.java
│   │   ├── entity/                   # 实体类（均实现 Serializable）
│   │   │   ├── User.java
│   │   │   ├── Resume.java
│   │   │   ├── Project.java
│   │   │   └── SystemConfig.java
│   │   ├── mapper/                   # MyBatis Plus 数据访问层
│   │   ├── service/                  # 服务接口与实现
│   │   │   └── impl/
│   │   ├── utils/                    # 工具类
│   │   │   └── JwtUtil.java
│   │   └── ResumePlatformApplication.java  # 启动入口
│   ├── src/main/resources/
│   │   ├── application.yml           # 应用配置
│   │   └── schema.sql               # 数据库建表脚本
│   └── pom.xml
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── views/                    # 页面组件
│   │   │   ├── Login.vue               # 登录页
│   │   │   ├── Layout.vue              # 布局框架
│   │   │   ├── Home.vue                # 首页仪表盘
│   │   │   ├── Resume.vue              # 简历编辑/预览
│   │   │   ├── Project.vue             # 项目文件管理
│   │   │   └── Admin.vue               # 权限管理
│   │   ├── router/index.js            # 路由配置（含权限守卫）
│   │   ├── utils/request.js           # Axios 封装（拦截器/Token刷新）
│   │   ├── App.vue
│   │   └── main.js
│   ├── vite.config.js
│   └── package.json
├── nginx.conf                        # Nginx 部署配置
└── README.md
```

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 2. 数据库配置

创建 MySQL 数据库并执行初始化脚本：

```sql
CREATE DATABASE IF NOT EXISTS resume_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resume_platform;
-- 执行 backend/src/main/resources/schema.sql 中的建表语句
```

修改 `backend/src/main/resources/application.yml` 中的数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/resume_platform
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 启动后端

```bash
cd backend
mvn clean compile    # 编译验证
mvn spring-boot:run  # 启动服务
```

后端服务将在 `http://localhost:9090` 启动，启动时会自动初始化 admin 用户和示例简历数据。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务将在 `http://localhost:5173` 启动，Vite 已配置代理将 `/api` 和 `/uploads` 转发到后端 9090 端口。

### 5. 构建部署

```bash
# 前端打包
cd frontend
npm run build        # 产物输出到 frontend/dist/

# 后端打包
cd backend
mvn clean package    # 产物输出到 backend/target/platform-1.0.0.jar
```

## 功能说明

### 登录模块
- 用户登录（用户名 + 密码，BCrypt 加密校验）
- JWT 双 Token 机制（AccessToken 短期有效 + RefreshToken 长期刷新）
- 图形验证码
- 忘记密码（邮箱验证码重置，Redis 存储5分钟有效）
- 游客模式（无需登录，按权限配置查看公开内容）

### 首页
- 仪表盘风格（统计卡片、快捷入口）
- 管理员/普通用户/游客三种角色差异展示
- 个人简历摘要预览

### 简历管理
- 简历编辑（姓名、职位、头像、邮箱、电话、简介、技能、工作经历、教育背景、项目经验）
- 简历预览
- 数据隔离：普通用户只能编辑自己的简历，admin 可管理任意
- 游客可查看 admin 公开简历

### 项目文件管理
- 文件上传（支持图片、PDF、文档、压缩包等，最大100MB）
- 文件下载（中文文件名编码兼容）
- 文件在线预览（图片/PDF/文本，通过 query Token 支持新标签页直接打开）
- 文件描述（最大500字符）
- 删除项目时自动清理服务器物理文件
- 文件类型自动识别与徽章展示

### 权限管理（仅 admin）
- 游客模式访问控制（首页/简历/项目开关）
- 普通用户权限控制（简历编辑/项目查看开关）
- 权限配置实时生效，前端路由守卫联动

## 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 拥有全部权限，含示例简历 |
| 普通用户 | user | user123 | 可编辑自己的简历、上传项目文件 |

## API 接口概览

### 公开接口（无需认证）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/refresh` | 刷新 AccessToken |
| POST | `/api/auth/send-reset-code` | 发送重置密码验证码 |
| POST | `/api/auth/reset-password` | 重置密码 |
| GET | `/api/auth/captcha` | 获取图形验证码 |
| GET | `/api/public/home` | 首页数据 |
| GET | `/api/public/permissions` | 获取权限配置 |
| GET | `/api/public/admin-resume` | 获取 admin 公开简历 |
| GET | `/api/public/resume/{userId}` | 查看用户公开简历 |
| GET | `/api/system/config` | 读取系统配置 |
| GET | `/api/project/preview/{id}` | 文件预览（支持 query Token） |

### 认证接口（需登录）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resume/{userId}` | 查询简历 |
| POST | `/api/resume` | 保存简历（越权校验） |
| GET | `/api/project/user/{userId}` | 查询项目列表 |
| POST | `/api/project` | 新增项目 |
| PUT | `/api/project/{id}` | 更新项目信息 |
| DELETE | `/api/project/{id}` | 删除项目 |
| POST | `/api/project/upload` | 上传文件 |
| GET | `/api/project/download/{id}` | 下载文件 |

### 管理员接口（需 admin 角色）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/system/config` | 更新系统权限配置 |

## 安全设计

- **JWT 认证**：AccessToken（1小时）+ RefreshToken（7天），支持 query 参数传 Token 用于文件预览
- **越权校验**：所有写操作前校验资源 userId 归属，admin 可操作任意资源，普通用户仅限自己的
- **权限配置化**：游客/普通用户的访问权限通过 `system_config` 表动态管理
- **文件安全**：上传限制100MB，删除项目时自动清理服务器文件
- **统一异常处理**：BusinessException（业务异常）与系统异常分离，错误码统一管理
- **数据隔离**：每个用户简历独立，普通用户无法查看 admin 的简历内容

## 开发规范

本项目后端代码遵循《阿里巴巴 Java 开发手册》核心规约：

- **命名规范**：类名 UpperCamelCase，方法名/变量名 lowerCamelCase，常量全大写下划线分隔
- **异常处理**：自定义 `BusinessException` + `ErrorCode` 枚举，区分业务异常与系统异常
- **日志规范**：统一使用 SLF4J，禁止 `System.out.println`；WARN 记录业务异常，ERROR 记录系统异常并输出堆栈
- **依赖注入**：使用构造器注入（`@RequiredArgsConstructor` + final 字段），禁止字段 `@Autowired`
- **集合处理**：HashMap 初始化指定容量，遍历时使用 entrySet
- **POJO 规范**：Entity/DTO 均实现 `Serializable` 接口，显式声明 `serialVersionUID`
- **控制语句**：使用卫语句（Guard Clause）减少嵌套层级
- **参数校验**：Controller 层使用 DTO + `@Valid` 注解，禁止 `Map<String, String>` 直接传参
- **注释规范**：所有类/接口/方法补充 Javadoc，字段说明业务含义

## 配置说明

### 后端配置（application.yml）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 9090 | 后端服务端口 |
| `spring.servlet.multipart.max-file-size` | 100MB | 单文件上传大小限制 |
| `file.upload-dir` | uploads | 文件上传目录 |
| `jwt.secret` | - | JWT 签名密钥（建议≥64字节） |
| `jwt.access-token-expire` | 3600000 | AccessToken 有效期（毫秒） |
| `jwt.refresh-token-expire` | 604800000 | RefreshToken 有效期（毫秒） |

### 前端配置

- 开发环境：Vite 代理 `/api` → `http://localhost:9090`
- 生产环境：通过 Nginx 反向代理统一 `/api` 和 `/uploads` 到后端
