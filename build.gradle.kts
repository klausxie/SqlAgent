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
    // IntelliJ Platform - Target 2024.3 (latest stable)
    intellijPlatform {
        create("IC", "2024.3.2")
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
            sinceBuild = "243"
            untilBuild = "243.*"
        }
    }
}

tasks {
    // Set the JVM compatibility versions (Java 17 for IntelliJ 2024.3)
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    // Configure plugin.xml
    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("243.*")
    }
}
