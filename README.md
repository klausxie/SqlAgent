# SqlAgent

> AI-Powered SQL optimization assistant for IntelliJ IDEA

SqlAgent helps you write better, faster SQL queries by leveraging OpenCode AI to analyze performance, suggest optimizations, and explain execution plans.

## ✨ Features

- **🎯 One-Click Optimization** - Right-click any SQL statement to optimize with AI
- **📱 MyBatis Integration** - Click gutter icons in mapper XML files for instant optimization
- **🔍 Smart Analysis** - Automatic metadata retrieval and execution plan analysis
- **📊 Side-by-Side Diff** - Visual comparison of original and optimized SQL
- **🗄️ Multi-Database Support** - MySQL, PostgreSQL
- **📦 Zero External Dependencies** - OpenCode and MCP server bundled!

## 🏗️ Architecture

```
┌─────────────────────┐
│ IntelliJ IDEA       │
│  - Plugin UI        │
│  - Database Config  │
│  - Java MCP Server  │
└─────────┬───────────┘
          │ HTTP
          ▼
┌─────────────────────┐
│ OpenCode Server     │
│  (Bundled)          │
│  - sql-optimizer    │
│    Skill            │
└─────────┬───────────┘
          │ MCP (via STDIO)
          ▼
┌─────────────────────┐
│ Java MCP Server     │
│  (Bundled)          │
│  - Table metadata   │
│  - Execution plans  │
│  - SQL parsing      │
└─────────┬───────────┘
          │ JDBC
          ▼
┌─────────────────────┐
│ Database            │
│  (MySQL/PostgreSQL) │
└─────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **IntelliJ IDEA** 2024.3+

That's it! No Python, no external dependencies needed.

### 1. Install Plugin

**From Disk (Development)**

```bash
git clone https://github.com/your-org/sqlagent.git
cd sqlagent
./gradlew buildPlugin
# Install: IDEA → Settings → Plugins → ⚙️ → Install Plugin from Disk
```

**From Marketplace (Coming Soon)**

Search "SqlAgent" in IDEA → Settings → Plugins → Marketplace

### 2. Configure Database Connection

1. Open **Settings → Tools → SQL Agent**
2. Scroll to **Database Configuration** section
3. Fill in your database details:
   - **Database Type**: MySQL or PostgreSQL
   - **Host**: e.g., localhost
   - **Port**: 3306 (MySQL) or 5432 (PostgreSQL)
   - **Database**: Your database name
   - **Username**: Your database username
   - **Password**: Your database password
4. Click **OK** to save

The plugin will automatically:
- Extract the Java MCP server
- Start the MCP server with your database configuration
- Configure OpenCode to use the MCP server

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

# Build plugin (includes MCP server)
./gradlew buildPlugin

# Run E2E tests (requires OpenCode running)
./gradlew test --tests E2ETest
```

## 📝 License

[Apache License 2.0](LICENSE)

## 🤝 Contributing

Contributions welcome! Please see [DEVELOPMENT_GUIDE.md](docs/DEVELOPMENT_GUIDE.md).

## 🙏 Acknowledgments

- [OpenCode](https://github.com/anthropics/opencode) - AI assistant platform
- [IntelliJ Platform](https://plugins.jetbrains.com/docs/intellij/welcome.html) - Plugin SDK
- [Model Context Protocol](https://modelcontextprotocol.io/) - Standard for LLM tool integration

## 📧 Support

- 🐛 [Report Issues](https://github.com/your-org/sqlagent/issues)
- 💬 [Discussions](https://github.com/your-org/sqlagent/discussions)
- 📖 [Documentation](docs/USER_GUIDE.md)
