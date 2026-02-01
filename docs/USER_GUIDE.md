# SqlAgent 用户指南

本指南将帮助你安装、配置和使用 SqlAgent IntelliJ IDEA 插件。

---

## 📋 目录

1. [系统要求](#系统要求)
2. [安装步骤](#安装步骤)
3. [配置 OpenCode](#配置-opencode)
4. [配置数据库连接](#配置数据库连接)
5. [使用插件](#使用插件)
6. [常见问题排查](#常见问题排查)
7. [高级配置](#高级配置)

---

## 系统要求

### 必需软件

| 软件 | 最低版本 | 推荐版本 | 用途 |
|------|---------|---------|------|
| IntelliJ IDEA | 2024.3 | 2024.3.2+ | IDE |
| OpenCode | latest | latest | AI 助手服务器 |
| Python | 3.12+ | 3.12.2+ | MCP 服务器运行环境 |
| uv | latest | latest | Python 包管理器（可选） |

### 支持的数据库

- MySQL 5.7+
- PostgreSQL 12+

---

## 安装步骤

### 步骤 1: 安装 OpenCode

OpenCode 是 SqlAgent 的后端服务，提供 AI 能力和 MCP 工具集成。

#### macOS (推荐)

```bash
# 使用 Homebrew 安装
brew install opencode

# 启动 OpenCode 服务
opencode server

# 验证安装（浏览器打开）
open http://localhost:4096
```

#### Linux

```bash
# 下载最新版本
wget https://github.com/your-org/opencode/releases/latest/download/opencode-linux-amd64.tar.gz

# 解压
tar -xzf opencode-linux-amd64.tar.gz

# 启动服务
./opencode server
```

#### 验证安装

访问 `http://localhost:4096`，如果看到 OpenCode 界面，说明安装成功。

---

### 步骤 2: 安装 SqlAgent 插件

#### 方式 1: 从磁盘安装（开发版本）

```bash
# 克隆仓库
git clone https://github.com/your-org/sqlagent.git
cd sqlagent

# 构建插件
./gradlew buildPlugin

# 生成的插件位于: build/distributions/sqlagent-1.0-SNAPSHOT.zip
```

然后在 IntelliJ IDEA 中：
1. 打开 **Settings/Preferences** → **Plugins**
2. 点击 ⚙️ 图标 → **Install Plugin from Disk...**
3. 选择 `build/distributions/sqlagent-1.0-SNAPSHOT.zip`
4. 重启 IDEA

#### 方式 2: 从 JetBrains Marketplace 安装（正式版本）

1. 打开 **Settings/Preferences** → **Plugins**
2. 搜索 "SqlAgent"
3. 点击 **Install**
4. 重启 IDEA

---

## 配置 OpenCode

### 创建 OpenCode 配置文件

OpenCode 的配置文件位于 `~/.opencode/opencode.json`。

如果不存在，请创建该文件：

```bash
mkdir -p ~/.opencode
touch ~/.opencode/opencode.json
```

### 配置 MCP 服务器

在 `~/.opencode/opencode.json` 中添加 database-tools MCP 服务器配置：

#### MySQL 配置示例

```json
{
  "mcpServers": {
    "database-tools": {
      "command": "uv",
      "args": [
        "--directory",
        "/Users/klaus/opencode/database_tools_mcp",
        "run",
        "python",
        "/Users/klaus/opencode/database_tools_mcp/main.py"
      ],
      "env": {
        "DB_TYPE": "mysql",
        "DB_HOST": "localhost",
        "DB_PORT": "3306",
        "DB_USER": "your_username",
        "DB_PASSWORD": "your_password",
        "DB_NAME": "your_database"
      }
    }
  }
}
```

#### PostgreSQL 配置示例

```json
{
  "mcpServers": {
    "database-tools": {
      "command": "uv",
      "args": [
        "--directory",
        "/Users/klaus/opencode/database_tools_mcp",
        "run",
        "python",
        "/Users/klaus/opencode/database_tools_mcp/main.py"
      ],
      "env": {
        "DB_TYPE": "postgresql",
        "DB_HOST": "localhost",
        "DB_PORT": "5432",
        "DB_USER": "your_username",
        "DB_PASSWORD": "your_password",
        "DB_NAME": "your_database"
      }
    }
  }
}
```

#### 不使用 uv 的配置（可选）

如果你没有安装 uv，可以直接使用 Python：

```json
{
  "mcpServers": {
    "database-tools": {
      "command": "python",
      "args": [
        "/Users/klaus/opencode/database_tools_mcp/main.py"
      ],
      "env": {
        "DB_TYPE": "mysql",
        "DB_HOST": "localhost",
        "DB_PORT": "3306",
        "DB_USER": "your_username",
        "DB_PASSWORD": "your_password",
        "DB_NAME": "your_database"
      }
    }
  }
}
```

**注意**：请将路径 `/Users/klaus/opencode/database_tools_mcp` 替换为你实际的 MCP 服务器路径。

---

## 配置数据库连接

### MySQL

1. 确保数据库服务正在运行：

```bash
# macOS
brew services start mysql

# Linux
sudo systemctl start mysql
```

2. 创建数据库用户（如果需要）：

```sql
CREATE USER 'sqlagent'@'localhost' IDENTIFIED BY 'your_password';
GRANT SELECT, SHOW VIEW ON your_database.* TO 'sqlagent'@'localhost';
FLUSH PRIVILEGES;
```

3. 更新 `~/.opencode/opencode.json` 中的数据库凭据。

### PostgreSQL

1. 确保数据库服务正在运行：

```bash
# macOS
brew services start postgresql

# Linux
sudo systemctl start postgresql
```

2. 创建数据库用户（如果需要）：

```sql
CREATE USER sqlagent WITH PASSWORD 'your_password';
GRANT CONNECT ON DATABASE your_database TO sqlagent;
GRANT USAGE ON SCHEMA public TO sqlagent;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sqlagent;
```

3. 更新 `~/.opencode/opencode.json` 中的数据库凭据。

---

## 使用插件

### 方式 1: 从编辑器优化 SQL

1. 在代码编辑器中选中要优化的 SQL 语句

2. 右键点击选中的文本，选择 **Optimize SQL**（或使用快捷键）

3. 插件会：
   - 发送 SQL 到 OpenCode
   - OpenCode 使用 MCP 工具获取表元数据和执行计划
   - AI 分析并返回优化建议

4. 查看 **SQL Agent** 工具窗口中的优化结果

5. 点击 **Apply** 应用优化后的 SQL

### 方式 2: 从 MyBatis Mapper XML 优化

1. 打开 MyBatis Mapper XML 文件

2. 在左侧行号区域，你会看到绿色的 SQL 图标（💡）

3. 点击图标，插件会自动：
   - 提取 SQL 语句
   - 发送优化请求
   - 显示优化建议

4. 查看差异并选择是否应用

### 查看优化结果

优化结果包括：

- **优化后的 SQL**: 性能更好的 SQL 语句
- **改进说明**: 具体的优化建议和原因
- **性能对比**: 执行计划对比（如果有）
- **索引建议**: 推荐添加的索引（如果有）

---

## 常见问题排查

### 问题 1: "Cannot connect to OpenCode server"

**症状**：点击优化时显示无法连接到 OpenCode

**解决方案**：

1. 检查 OpenCode 是否正在运行：

```bash
# macOS/Linux
ps aux | grep opencode

# 查看端口占用
lsof -i :4096
```

2. 启动 OpenCode：

```bash
opencode server
```

3. 验证访问：打开浏览器访问 `http://localhost:4096`

---

### 问题 2: "Read timed out"

**症状**：优化请求超时

**可能原因**：

- MCP 服务器未启动或配置错误
- 数据库连接失败
- SQL 过于复杂，AI 处理时间长

**解决方案**：

1. 检查 MCP 服务器配置：

```bash
# 查看 OpenCode 日志
tail -f ~/.opencode/logs/server.log
```

2. 测试数据库连接：

```bash
# MySQL
mysql -h localhost -u your_username -p your_database

# PostgreSQL
psql -h localhost -U your_username -d your_database
```

3. 尝试简化 SQL，分段优化

---

### 问题 3: "No Database Configuration"

**症状**：提示缺少数据库配置

**解决方案**：

这个错误在新版本中不应该出现。如果看到此错误：

1. 确认你使用的是最新版本的插件
2. 检查 `~/.opencode/opencode.json` 是否存在且格式正确
3. 重启 OpenCode 服务

---

### 问题 4: MCP 工具调用失败

**症状**：OpenCode 日志显示 "database-tools" 调用失败

**解决方案**：

1. 检查 MCP 服务器路径是否正确：

```bash
# 确认文件存在
ls /Users/klaus/opencode/database_tools_mcp/main.py
```

2. 检查配置文件格式：

```bash
# 验证 JSON 格式
cat ~/.opencode/opencode.json | python -m json.tool
```

3. 手动测试 MCP 服务器：

```bash
cd /Users/klaus/opencode/database_tools_mcp
uv run python main.py
```

4. 检查 Python 环境和依赖：

```bash
cd /Users/klaus/opencode/database_tools_mcp
uv sync
```

---

### 问题 5: 优化结果不准确

**症状**：AI 返回的优化建议不适用或错误

**解决方案**：

1. 确保数据库元数据正确：
   - 检查表结构和索引是否正确
   - 确认数据行数统计准确

2. 提供更多上下文：
   - 在 SQL 注释中说明业务逻辑
   - 使用表别名时保持一致性

3. 多次尝试：
   - AI 的回答可能有随机性
   - 调整优化目标或重新提问

---

## 高级配置

### 自定义 OpenCode 服务器地址

默认情况下，插件连接到 `http://localhost:4096`。

如果你的 OpenCode 运行在其他地址，可以在插件设置中修改：

1. 打开 **Settings/Preferences** → **Tools** → **SqlAgent**
2. 修改 **OpenCode Server URL**

### 调整超时时间

如果经常遇到超时，可以在插件设置中调整：

1. 打开 **Settings/Preferences** → **Tools** → **SqlAgent**
2. 修改 **Request Timeout (seconds)**

默认值为 300 秒（5 分钟）。

### 启用详细日志

用于调试问题：

1. 打开 **Help** → **Show Log in Explorer/Finder**
2. 查看日志文件搜索 "SqlAgent"

或通过 IntelliJ IDEA 的 **Internal Log** 查看实时日志。

---

## 下一步

- 📖 阅读 [开发指南](./DEVELOPMENT_GUIDE.md) 了解如何贡献代码
- 🐛 [报告问题](https://github.com/your-org/sqlagent/issues)
- 💬 [加入讨论](https://github.com/your-org/sqlagent/discussions)

---

## 需要帮助？

如果以上步骤无法解决你的问题，请：

1. 查看 [GitHub Issues](https://github.com/your-org/sqlagent/issues)
2. 创建新的 Issue，附上：
   - 操作系统和 IDEA 版本
   - 完整的错误信息
   - `~/.opencode/logs/server.log` 相关日志
   - IntelliJ IDEA 日志（**Help** → **Show Log in Explorer**）
