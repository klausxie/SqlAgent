# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SqlAgent is an IntelliJ IDEA plugin that provides AI-powered SQL assistance. The plugin integrates with LLM APIs to help users work with SQL queries and databases directly within the IDE.

**Build System**: Gradle with IntelliJ Platform Gradle Plugin (version 2.5.0)
**Target Platform**: IntelliJ IDEA Community Edition 2024.1.7 (build 241.14494.240)
**Language**: Java 17
**Group ID**: cn.mklaus
**Package Structure**: `cn.mklaus.sqlagent`

## Essential Commands

### Development
```bash
# Run IDE with plugin loaded for testing/debugging
./gradlew runIde

# Build the plugin (creates distributable ZIP)
./gradlew buildPlugin

# Clean build artifacts
./gradlew clean

# Build without running tests
./gradlew build -x test
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests com.example.YourTestClass

# Run tests with detailed output
./gradlew test --info
```

### Verification
```bash
# Run all checks (tests + verification tasks)
./gradlew check

# Verify plugin compatibility with IDE versions
./gradlew verifyPlugin

# Verify plugin structure and configuration
./gradlew verifyPluginStructure
```

### Plugin Distribution
```bash
# Build plugin ZIP archive (output: build/distributions/sqlagent-1.0-SNAPSHOT.zip)
./gradlew buildPlugin

# Prepare sandbox environment (for debugging)
./gradlew prepareSandbox

# Sign plugin (requires Marketplace ZIP Signer configuration)
./gradlew signPlugin

# Publish plugin to JetBrains Marketplace (requires tokens)
./gradlew publishPlugin
```

## Architecture

### Core Dependencies

The plugin uses several key libraries:
- **OkHttp 4.12.0**: HTTP client for LLM API calls
- **Gson 2.10.1**: JSON parsing and serialization
- **JSqlParser 4.7**: SQL parsing and formatting
- **HikariCP 5.1.0**: Database connection pooling
- **Database Drivers**: MySQL (8.2.0), PostgreSQL (42.7.1)

### Plugin Structure

```
src/main/
├── java/cn/mklaus/sqlagent/    # Main plugin code
└── resources/
    └── META-INF/
        ├── plugin.xml          # Plugin descriptor
        └── pluginIcon.svg      # Plugin icon
```

### IntelliJ Platform Integration

The plugin requires two bundled plugins:
- `com.intellij.java` - Java core support
- `org.jetbrains.plugins.yaml` - YAML file support

### Version Compatibility
- **sinceBuild**: "241" (IntelliJ 2024.1)
- **untilBuild**: "241.*" (2024.1.x series)

## Development Notes

### Running the Plugin
Use `./gradlew runIde` to launch a sandboxed IntelliJ IDEA instance with the plugin loaded. The sandbox is located at `build/idea-sandbox/`. Log files are written to `build/idea-sandbox/system/log/idea.log`.

### Plugin Configuration
The `plugin.xml` file (`src/main/resources/META-INF/plugin.xml`) is the main plugin descriptor. Key extension points and registrations go in the `<extensions>` section.

### Build Artifacts
- **Main JAR**: `build/libs/sqlagent-1.0-SNAPSHOT.jar`
- **Instrumented JAR**: `build/libs/sqlagent-1.0-SNAPSHOT-instrumented.jar` (with runtime instrumentation)
- **Distribution ZIP**: `build/distributions/sqlagent-1.0-SNAPSHOT.zip`

### Gradle Configuration
- **Configuration Cache**: Enabled for faster builds
- **Build Cache**: Enabled for incremental build optimization
- **Kotlin stdlib**: Opted out (Java-only project)

### Testing
Test files should be placed in `src/test/java/cn/mklaus/sqlagent/` following the same package structure. The project uses JUnit 4 and Mockito for testing.

## Common Patterns

### Adding Plugin Extensions
Register components in `plugin.xml`:
```xml
<extensions defaultExtensionNs="com.intellij">
    <applicationService serviceImplementation="cn.mklaus.sqlagent.YourService"/>
    <projectComponent serviceImplementation="cn.mklaus.sqlagent.YourComponent"/>
</extensions>
```

### Accessing IntelliJ APIs
Most functionality requires access to:
- `Project` - Current project context
- `Application` - Application-level services
- `ActionManager` - For registering actions
- `ToolWindowFactory` - For creating tool windows

### Working with Databases
Use the bundled database integration APIs along with HikariCP for connection pooling. SQL can be parsed and manipulated using JSqlParser.
