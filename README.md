# Travel Agent 智能旅游规划系统

## 项目概述
Travel Agent 是一款基于 Spring Boot 构建的智能旅游规划系统，结合大型语言模型（LLM）和本地知识库，为用户提供个性化、智能化的旅游解决方案。系统可根据用户的出行时间、预算、兴趣偏好等信息，自动生成目的地推荐、详细行程安排、住宿建议等内容，并支持多轮对话交互以优化规划方案。

## 项目结构
```plaintext
travel-agent/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/yhh/travelagent/
│   │   │       ├── agent/              # 代理类（处理模型交互逻辑）
│   │   │       ├── advisor/            # AOP 拦截器（日志、权限等）
│   │   │       ├── chatmemory/         # 对话记忆模块（记录用户历史交互）
│   │   │       ├── rag/                # 检索增强生成（RAG）核心逻辑
│   │   │       ├── tools/              # 工具类（文件操作、API 调用等）
│   │   │       └── travel/             # 旅游规划核心服务
│   │   └── resources/
│   │       ├── document/               # 本地知识库（旅游攻略、FAQ 等文档）
│   │       └── application*.yml        # 配置文件（环境区分配置）
│   └── test/                           # 单元测试与集成测试代码
├── .gitignore                          # Git 忽略规则（过滤无需提交的文件）
├── pom.xml                             # Maven 依赖配置
└── README.md                           # 项目说明文档
```

## 功能特性
个性化旅游规划：根据用户具体需求，如出行时间、预算、旅行偏好等，提供定制化旅游规划方案。
本地知识库支持：利用本地知识库，为用户提供准确、详细的旅游信息。
工具调用能力：支持调用多种工具，如 PDF 生成、HTML 生成、文件读写等工具，增强系统功能。
对话记忆功能：支持对话记忆，记录用户历史对话，提供连贯服务。
流式输出支持：支持流式输出，实时返回处理结果，提升用户体验。
## 技术栈
+ 核心框架：Spring Boot 3.5.3
+ AI 交互：Spring AI、DashScope SDK（阿里大模型服务）
+ 数据存储：MySQL（业务数据）、PgVector（向量数据库，支持 RAG 检索）
+ ORM 框架：MyBatis-Plus（简化数据库操作）
+ API 文档：Knife4j（OpenAPI 3 规范，接口可视化）
+ 工具库：Hutool（Java 工具类集合）
+ 开发语言：Java 21
## 快速开始
### 环境准备
+ JDK 21 及以上
+ Maven 3.6+
+ MySQL 8.0+
+ PgVector 数据库（用于向量存储，可选，视 RAG 功能启用情况）
+ 阿里云百练 DashScope API 密钥（需自行申请，用于调用大模型）
### 部署步骤
#### 克隆项目
```bash
git clone https://github.com/eason-passion/travel-agent.git
cd travel-agent
```

#### 配置环境
修改配置文件中的数据库连接、API 密钥等信息：
```yaml
# 示例：数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/travel_agent?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password

# 阿里大模型配置
  ai:
    dashscope:
      api-key: your_dashscope_api_key
```
#### 构建与启动
```bash
# 编译打包
mvn clean package -Dmaven.test.skip=true

# 启动服务（开发环境）
java -jar target/travel-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```
#### 访问系统
接口文档：启动后访问 
```curl
http://localhost:8080/doc.html（Knife4j 可视化界面）
```
核心接口：
```curl
/api/ai//travel_app/chat/sse/emitter 
```
（接收用户需求，返回旅游规划结果）

### 常见问题
#### 如何修改默认端口？
在application.yml 中添加 server.port=你的端口号 即可。
#### 本地知识库如何更新？
将新的文档（如 PDF、MD等）放入 src/main/resources/document 目录，系统会自动加载（需重启服务）。
#### 大模型调用失败怎么办？
检查 DashScope API 密钥是否有效、网络是否通畅，或在配置文件中切换其他模型。
### 贡献指南
```plaintext
Fork 本仓库
创建特性分支：git checkout -b feature/your-feature
提交修改：git commit -m "Add your feature"
推送分支：git push origin feature/your-feature
提交 Pull Request
```
### 联系作者
+ 项目维护者：Eason
+ 仓库地址：https://github.com/eason-passion/travel-agent
+ 联系邮箱：hy3180777@gmail.com
