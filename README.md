# SqlAgent

> AI-Powered SQL optimization assistant for IntelliJ IDEA

SqlAgent helps you write better, faster SQL queries by leveraging OpenCode AI to analyze performance, suggest optimizations, and explain execution plans.

## ✨ Features

- **🎯 One-Click Optimization** - Right-click any SQL statement to optimize with AI
- **📱 MyBatis Integration** - Click gutter icons in mapper XML files for instant optimization
- **🔍 Smart Analysis** - Automatic metadata retrieval and execution plan analysis
- **📊 Side-by-Side Diff** - Visual comparison of original and optimized SQL
- **🗄️ Multi-Database Support** - MySQL, PostgreSQL (extensible via MCP)
- **📦 OpenCode Bundled** - Works out of the box, no separate installation needed!

## 📦 OpenCode Integration

**Great news!** This plugin includes OpenCode executable, so you don't need to install it separately.

### Supported Platforms

- **macOS**: Intel (x86-64) and Apple Silicon (ARM64)
- **Linux**: x86-64
- **Windows**: x86-64

### Automatic Detection

The plugin automatically detects and uses the appropriate OpenCode executable:

1. Custom path (if specified in settings)
2. **Bundled OpenCode executable** (included with plugin)
3. System-installed OpenCode (in PATH)
4. Common installation paths

### MCP Server Configuration

While the plugin bundles OpenCode, you still need to configure MCP servers for database tools.

See the configuration section below for details.

## 🚀 Quick Start

### Prerequisites

- **IntelliJ IDEA** 2024.3+
- **Python** 3.12+ (for MCP tools)
- **OpenCode** is bundled with the plugin!

### 1. Configure Database Tools MCP

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

### 2. Install Plugin

**From Disk (Development)**

```bash
git clone https://github.com/your-org/sqlagent.git
cd sqlagent
./gradlew buildPlugin
# Install: IDEA → Settings → Plugins → ⚙️ → Install Plugin from Disk
```

**From Marketplace (Coming Soon)**

Search "SqlAgent" in IDEA → Settings → Plugins → Marketplace

### 3. Optimize Your SQL

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
