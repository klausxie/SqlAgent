plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "cn.mklaus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // IntelliJ Platform - Target 2024.1
    intellijPlatform {
        create("IC", "2024.1.7")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        instrumentationTools()

        // Required plugins for database support
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.yaml")
    }

    // HTTP client for LLM API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Database drivers
    implementation("com.mysql:mysql-connector-j:8.2.0")
    implementation("org.postgresql:postgresql:42.7.1")

    // Connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // SQL parsing and formatting
    implementation("com.github.jsqlparser:jsqlparser:4.7")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "241.*"
        }
    }
}

tasks {
    // Set the JVM compatibility versions (Java 17 for IntelliJ 2024.1)
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    // Copy MCP server JAR from mcp-server module to resources
    register("copyMcpServer", Copy::class) {
        group = "build"
        description = "Copy MCP server JAR to plugin resources"

        dependsOn(":mcp-server:shadowJar")

        from(file("mcp-server/build/libs/sqlagent-mcp-server.jar"))
        into(file("src/main/resources/mcp"))

        doLast {
            println("Copied MCP server JAR to resources")
        }
    }

    // Configure plugin.xml
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("241.*")

        dependsOn("copyMcpServer")
    }

    // Set executable permission on bundled binaries (Unix only)
    register("chmodBinaries") {
        group = "build"
        description = "Set executable permission on bundled OpenCode binaries"

        onlyIf {
            !System.getProperty("os.name").lowercase().contains("windows")
        }

        doLast {
            val binDir = file("src/main/resources/bin")
            if (binDir.exists()) {
                binDir.listFiles()?.filter { it.isDirectory }?.forEach { platformDir ->
                    platformDir.listFiles()?.filter { it.name == "opencode" }?.forEach { binary ->
                        binary.setExecutable(true)
                        println("Made executable: ${binary.name}")
                    }
                }
            }
        }
    }

    // Ensure binaries are executable before building
    named("build") {
        dependsOn("chmodBinaries")
        dependsOn("copyMcpServer")
    }

    named("prepareSandbox") {
        dependsOn("chmodBinaries")
        dependsOn("copyMcpServer")
    }
}
