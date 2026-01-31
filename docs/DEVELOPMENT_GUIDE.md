# SQL Agent 开发指南

## 目录

1. [项目概述](#项目概述)
2. [环境准备](#环境准备)
3. [项目结构](#项目结构)
4. [核心架构](#核心架构)
5. [开发流程](#开发流程)
6. [核心功能详解](#核心功能详解)
7. [调试技巧](#调试技巧)
8. [测试指南](#测试指南)
9. [发布流程](#发布流程)
10. [常见问题](#常见问题)

---

## 项目概述

SQL Agent 是一个 IntelliJ IDEA 插件，通过集成 OpenCode AI 为用户提供智能 SQL 优化建议。

### 技术栈

- **语言**: Java 17
- **构建工具**: Gradle 8.x
- **插件平台**: IntelliJ Platform Gradle Plugin 2.1.0
- **目标平台**: IntelliJ IDEA Community Edition 2024.3.2
- **主要依赖**:
  - OkHttp 4.12.0 - HTTP 客户端
  - Gson 2.10.1 - JSON 处理
  - JSqlParser 4.7 - SQL 解析
  - HikariCP 5.1.0 - 数据库连接池
  - MySQL Connector 8.2.0
  - PostgreSQL Driver 42.7.1

---

## 环境准备

### 必要软件

1. **JDK 17** - IntelliJ 平台要求
   ```bash
   # 验证 Java 版本
   java -version  # 应显示 17.x.x
   ```

2. **IntelliJ IDEA Community Edition 2024.3.2+**
   - 用于开发和测试插件

3. **Gradle 8.x**（可选，项目使用 Gradle Wrapper）
   ```bash
   ./gradlew --version
   ```

### 克隆项目

```bash
git clone <repository-url>
cd SqlAgent
```

### 首次构建

```bash
# 清理并构建项目
./gradlew clean build

# 首次运行可能需要下载依赖，需要一些时间
```

---

## 项目结构

```
SqlAgent/
├── build.gradle.kts          # Gradle 构建配置
├── gradle.properties         # Gradle 属性配置
├── settings.gradle.kts       # Gradle 设置
├── CLAUDE.md                 # Claude Code 项目说明
├── docs/
│   └── DEVELOPMENT_GUIDE.md  # 本文档
├── src/
│   ├── main/
│   │   ├── java/cn/mklaus/sqlagent/
│   │   │   ├── config/        # 配置管理
│   │   │   │   ├── DatabaseConfigurable.java    # IDE 配置界面
│   │   │   │   ├── DatabaseConfigForm.java      # 配置表单
│   │   │   │   └── DatabaseConfigStore.java     # 配置存储
│   │   │   ├── database/      # 数据库访问层
│   │   │   │   ├── DatabaseConnectionManager.java
│   │   │   │   ├── MetadataAdapter.java         # 元数据适配器接口
│   │   │   │   ├── MetadataAdapterFactory.java
│   │   │   │   ├── MySQLMetadataAdapter.java
│   │   │   │   └── PostgreSQLMetadataAdapter.java
│   │   │   ├── model/         # 数据模型
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   ├── OptimizationRequest.java
│   │   │   │   ├── OptimizationResponse.java
│   │   │   │   ├── TableMetadata.java
│   │   │   │   ├── ColumnInfo.java
│   │   │   │   └── SuggestionType.java
│   │   │   ├── opencode/      # OpenCode AI 集成
│   │   │   │   ├── OpenCodeClient.java
│   │   │   │   └── SessionManager.java
│   │   │   ├── service/       # 业务逻辑层
│   │   │   │   ├── SqlOptimizerService.java
│   │   │   │   └── PromptBuilder.java
│   │   │   └── ui/            # 用户界面
│   │   │       ├── OptimizeSqlAction.java       # 右键菜单操作
│   │   │       ├── OptimizationToolWindowFactory.java
│   │   │       ├── OptimizationPanel.java
│   │   │       └── DiffViewer.java
│   │   └── resources/
│   │       └── META-INF/
│   │           ├── plugin.xml  # 插件描述符
│   │           └── pluginIcon.svg
│   └── test/
│       └── java/cn/mklaus/sqlagent/
│           └── service/        # 单元测试
├── build/                     # 构建输出
│   ├── idea-sandbox/          # 测试用沙箱环境
│   ├── distributions/         # 插件分发包
│   └── libs/                  # 编译后的 JAR
└── gradle/                    # Gradle wrapper 文件
```

---

## 核心架构

### 分层架构

```
┌─────────────────────────────────────────────┐
│              UI Layer (ui)                   │
│  - OptimizeSqlAction                        │
│  - OptimizationToolWindowFactory            │
│  - DiffViewer                               │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Service Layer (service)             │
│  - SqlOptimizerService                      │
│  - PromptBuilder                            │
└─────────────────────────────────────────────┘
                    ↓
┌──────────────┬──────────────────────────────┐
│  OpenCode    │    Database Layer            │
│   (opencode) │    (database)                │
│              │                               │
│ - Client     │ - ConnectionManager          │
│ - Session    │ - MetadataAdapter            │
└──────────────┴──────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Model Layer (model)                 │
│  - DatabaseConfig                            │
│  - OptimizationRequest/Response              │
│  - TableMetadata                             │
└─────────────────────────────────────────────┘
```

### 核心组件职责

#### 1. UI 层 (ui)
- **OptimizeSqlAction**: 处理编辑器右键菜单的 "Optimize SQL" 操作
- **OptimizationToolWindowFactory**: 创建和管理右侧工具窗口
- **OptimizationPanel**: 显示优化进度、元数据和建议
- **DiffViewer**: 显示原始 SQL 和优化后 SQL 的对比视图

#### 2. 服务层 (service)
- **SqlOptimizerService**: 协调整个优化流程的核心服务
- **PromptBuilder**: 构建 AI 请求的 prompt

#### 3. 数据库层 (database)
- **DatabaseConnectionManager**: 管理数据库连接池（HikariCP）
- **MetadataAdapter**: 元数据提取接口
- **MySQLMetadataAdapter / PostgreSQLMetadataAdapter**: 具体数据库实现

#### 4. OpenCode 集成层 (opencode)
- **OpenCodeClient**: 与 OpenCode AI 服务通信
- **SessionManager**: 管理 AI 会话状态

#### 5. 模型层 (model)
- 数据传输对象（DTO）和领域模型

---

## 开发流程

### 1. 运行开发环境

```bash
# 启动沙箱 IDE（首次运行会下载 IntelliJ 平台，需等待）
./gradlew runIde
```

这会启动一个带插件的 IntelliJ IDEA 实例：
- 沙箱位置：`build/idea-sandbox/`
- 日志位置：`build/idea-sandbox/system/log/idea.log`
- 插件会自动加载，可直接测试

### 2. 开发新功能的一般步骤

#### Step 1: 理解需求
确定功能属于哪个层级（UI、Service、Database、Model）

#### Step 2: 创建/修改模型（如需要）
在 `model/` 包中创建或修改数据类

#### Step 3: 实现业务逻辑
在 `service/` 或相应包中实现功能

#### Step 4: 更新 UI（如需要）
在 `ui/` 包中添加或修改界面组件

#### Step 5: 注册扩展
在 `plugin.xml` 中注册新的扩展点

#### Step 6: 测试
```bash
# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew test --tests cn.mklaus.sqlagent.service.YourTest
```

### 3. 代码提交规范

```bash
# 1. 查看修改
git status
git diff

# 2. 添加文件
git add <files>

# 3. 提交（使用规范的提交信息）
git commit -m "feat: 添加对 Oracle 数据库的支持"
# 或
git commit -m "fix: 修复元数据提取时的空指针异常"

# 提交类型：
# feat: 新功能
# fix: 修复 bug
# docs: 文档更新
# refactor: 重构
# test: 测试相关
# chore: 构建/工具相关
```

---

## 核心功能详解

### SQL 优化流程

```
用户选中 SQL → 右键 "Optimize SQL"
     ↓
检查数据库配置
     ↓
后台任务启动
     ↓
提取表名
     ↓
连接数据库 → 提取元数据（行数、列、索引）
     ↓
获取执行计划
     ↓
构建 OptimizationRequest
     ↓
调用 OpenCode AI
     ↓
显示优化结果
     ↓
用户选择应用 → 替换原始 SQL
```

### 关键代码路径

#### 1. 用户操作入口
`OptimizeSqlAction.actionPerformed()` (OptimizeSqlAction.java:41)

#### 2. 核心优化服务
`SqlOptimizerService.optimize()` (SqlOptimizerService.java:29)

#### 3. 元数据提取
`MetadataAdapter.extractTableMetadata()` (各个适配器实现)

#### 4. AI 通信
`OpenCodeClient.optimize()` (OpenCodeClient.java)

### 配置存储

数据库配置通过 IntelliJ 的持久化组件存储：

```java
// 读取配置
DatabaseConfig config = DatabaseConfigStore.getInstance().getConfig();

// 配置存储位置（在沙箱中）
// build/idea-sandbox/options/other.xml
```

---

## 调试技巧

### 1. 使用调试模式运行

```bash
# 以调试模式运行（监听 5005 端口）
./gradlew runIde --debug-jvm
```

然后在 IntelliJ IDEA 中：
1. Run → Edit Configurations
2. 添加 "Remote JVM Debug"
3. 端口设置为 5005
4. 启动调试会话

### 2. 查看日志

```bash
# 实时查看日志
tail -f build/idea-sandbox/system/log/idea.log
```

在代码中使用 Logger：

```java
private static final Logger LOG = Logger.getInstance(YourClass.class);

LOG.info("Info message");
LOG.warn("Warning message");
LOG.error("Error message", exception);
```

### 3. 常见调试场景

#### 场景 1：插件未加载
检查：
- `plugin.xml` 中的 `id`、`version` 是否正确
- `sinceBuild` 和 `untilBuild` 是否匹配 IDE 版本
- 查看日志中的错误信息

#### 场景 2：数据库连接失败
检查：
- 配置中的连接参数是否正确
- 数据库是否可访问
- 网络连接是否正常
- 驱动版本是否兼容

#### 场景 3：AI 调用失败
检查：
- OpenCode 服务器是否运行（默认 http://localhost:4096）
- 请求格式是否正确
- 查看响应内容

---

## 测试指南

### 单元测试

项目使用 JUnit 4 和 Mockito。

```java
// 示例：src/test/java/cn/mklaus/sqlagent/service/YourTest.java
public class YourTest {
    @Test
    public void testSomething() {
        // Given
        String input = "test";

        // When
        String result = methodUnderTest(input);

        // Then
        assertEquals("expected", result);
    }
}
```

### 运行测试

```bash
# 所有测试
./gradlew test

# 特定测试类
./gradlew test --tests SqlOptimizerServiceTest

# 带详细输出
./gradlew test --info

# 跳过测试构建
./gradlew build -x test
```

### 测试覆盖率报告

```bash
./gradlew test jacocoTestReport
# 报告位置：build/reports/jacoco/test/html/index.html
```

---

## 发布流程

### 1. 版本号管理

编辑 `build.gradle.kts`:
```kotlin
group = "cn.mklaus"
version = "1.0.0"  # 修改版本号
```

### 2. 构建分发包

```bash
./gradlew clean buildPlugin
```

输出：`build/distributions/sqlagent-1.0.0.zip`

### 3. 验证插件

```bash
# 验证插件结构和兼容性
./gradlew verifyPlugin
./gradlew verifyPluginStructure
```

### 4. 发布到 JetBrains Marketplace

```bash
# 需要先配置发布令牌
./gradlew publishPlugin
```

或手动上传：
1. 访问 https://plugins.jetbrains.com/
2. 登录并上传 ZIP 文件
3. 填写版本说明和变更日志

### 5. 签名插件（可选）

需要配置 Marketplace ZIP Signer：
```bash
./gradlew signPlugin
```

---

## 常见问题

### Q1: 运行 `runIde` 时报 "OutOfMemory"
**A**: 增加 Gradle 内存，编辑 `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

### Q2: 插件在沙箱中不显示
**A**:
1. 清理并重新构建：`./gradlew clean build`
2. 删除沙箱：`rm -rf build/idea-sandbox`
3. 重新运行：`./gradlew runIde`

### Q3: 依赖冲突
**A**: 检查 `build.gradle.kts` 中的依赖版本，IntelliJ 平台已包含许多库。

### Q4: 如何支持新的数据库类型？
**A**:
1. 在 `database/` 包创建新的 `XxxMetadataAdapter`
2. 实现 `MetadataAdapter` 接口
3. 在 `MetadataAdapterFactory` 中注册

```java
public class OracleMetadataAdapter implements MetadataAdapter {
    // 实现接口方法
}

// 在工厂类中添加
public static MetadataAdapter getAdapter(DatabaseConfig config) {
    switch (config.getType()) {
        case ORACLE:
            return new OracleMetadataAdapter();
        // ...
    }
}
```

### Q5: 如何添加新的 UI 操作？
**A**:
1. 创建继承 `AnAction` 的类
2. 在 `plugin.xml` 的 `<actions>` 中注册
3. 实现 `actionPerformed()` 和 `update()` 方法

---

## 参考资源

### 官方文档
- [IntelliJ Platform SDK DevGuide](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html)
- [plugin.xml 配置](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)

### 代码示例
- [IntelliJ Platform SDK Code Samples](https://github.com/JetBrains/intellij-sdk-code-samples)

### 社区
- [JetBrains Platform Slack](https://plugins.jetbrains.com/docs/intellij/slack.html)
- [Stack Overflow - intellij-idea-plugin](https://stackoverflow.com/questions/tagged/intellij-idea-plugin)

---

## 附录

### 快捷命令参考

```bash
# 开发
./gradlew runIde              # 启动沙箱 IDE
./gradlew buildPlugin         # 构建插件
./gradlew clean               # 清理构建

# 测试
./gradlew test                # 运行测试
./gradlew check               # 完整检查

# 验证
./gradlew verifyPlugin        # 验证插件
./gradlew verifyPluginStructure  # 验证结构

# 发布
./gradlew signPlugin          # 签名插件
./gradlew publishPlugin       # 发布到 Marketplace
```

### IDE 版本对照

| IntelliJ Version | Build Number | sinceBuild |
|-----------------|--------------|------------|
| 2024.3          | 243.x        | 243        |
| 2024.2          | 242.x        | 242        |
| 2024.1          | 241.x        | 241        |

当前项目配置：IntelliJ 2024.3.2 (Build 243.x)

---

**祝开发顺利！** 有问题随时查阅本文档或查看官方资源。
