# SqlAgent

> AI-Powered SQL optimization assistant for IntelliJ IDEA

SqlAgent helps you write better, faster SQL queries by leveraging OpenCode AI to analyze performance, suggest optimizations, and explain execution plans.

## ✨ Features

- **🎯 One-Click Optimization** - Right-click any SQL statement to optimize with AI
- **📱 MyBatis Integration** - Click gutter icons in mapper XML files for instant optimization
- **🔍 Smart Analysis** - Automatic metadata retrieval and execution plan analysis
- **📊 Side-by-Side Diff** - Visual comparison of original and optimized SQL
- **🗄️ Multi-Database Support** - MySQL, PostgreSQL (extensible via MCP)

## 🚀 Quick Start

### Prerequisites

- **IntelliJ IDEA** 2024.3+
- **OpenCode** - AI assistant server
- **Python** 3.12+ (for MCP tools)

### 1. Install OpenCode

```bash
# macOS
brew install opencode
opencode server

# Verify installation
open http://localhost:4096
```

### 2. Configure Database Tools MCP

Create `~/.opencode/opencode.json`:

```json
{
  "mcpServers": {
    "database-tools": {
      "command": "uv",
      "args": [
        "--directory",
        "/path/to/database_tools_mcp",
        "run",
        "python",
        "/path/to/database_tools_mcp/main.py"
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

### 3. Install Plugin

**From Disk (Development)**

```bash
git clone https://github.com/your-org/sqlagent.git
cd sqlagent
./gradlew buildPlugin
# Install: IDEA → Settings → Plugins → ⚙️ → Install Plugin from Disk
```

**From Marketplace (Coming Soon)**

Search "SqlAgent" in IDEA → Settings → Plugins → Marketplace

### 4. Optimize Your SQL

**Option 1: Editor**

1. Select SQL in your editor
2. Right-click → **Optimize SQL with AI**
3. View suggestions in the **SQL Agent** tool window

**Option 2: MyBatis Mapper**

1. Open a MyBatis mapper XML file
2. Click the 💡 icon in the gutter
3. View optimization suggestions

## 📖 Documentation

- **[User Guide](docs/USER_GUIDE.md)** - Detailed setup and troubleshooting
- **[Development Guide](docs/DEVELOPMENT_GUIDE.md)** - Contributing and development

## 🛠️ Development

```bash
# Run IDE with plugin
./gradlew runIde

# Run tests
./gradlew test

# Build plugin
./gradlew buildPlugin

# Run E2E tests (requires OpenCode running)
./gradlew test --tests E2ETest
```

## 🏗️ Architecture

```
┌─────────────────────┐
│ IntelliJ IDEA       │
│  - Plugin UI        │
│  - SQL Selection    │
└─────────┬───────────┘
          │ HTTP
          ▼
┌─────────────────────┐
│ OpenCode Server     │
│  - sql-optimizer    │
│    Skill            │
└─────────┬───────────┘
          │ MCP
          ▼
┌─────────────────────┐
│ database-tools MCP  │
│  - Table metadata   │
│  - Execution plans  │
│  - SQL parsing      │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Database            │
│  (MySQL/PostgreSQL) │
└─────────────────────┘
```

## 📝 License

[Apache License 2.0](LICENSE)

## 🤝 Contributing

Contributions welcome! Please see [DEVELOPMENT_GUIDE.md](docs/DEVELOPMENT_GUIDE.md).

## 🙏 Acknowledgments

- [OpenCode](https://github.com/anthropics/opencode) - AI assistant platform
- [IntelliJ Platform](https://plugins.jetbrains.com/docs/intellij/welcome.html) - Plugin SDK
- [FastMCP](https://github.com/jlowin/fastmcp) - MCP server framework

## 📧 Support

- 🐛 [Report Issues](https://github.com/your-org/sqlagent/issues)
- 💬 [Discussions](https://github.com/your-org/sqlagent/discussions)
- 📖 [Documentation](docs/USER_GUIDE.md)
