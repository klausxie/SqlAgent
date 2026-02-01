plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cn.mklaus.sqlagent"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // MCP Java SDK - commented out until available in Maven Central
    // implementation("io.modelcontextprotocol.sdk:mcp:1.0.0")

    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Database Drivers
    implementation("com.mysql:mysql-connector-j:8.2.0")
    implementation("org.postgresql:postgresql:42.7.1")

    // Connection Pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // SQL Parsing
    implementation("com.github.jsqlparser:jsqlparser:4.7")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Testing
    testImplementation("junit:junit:4.13.2")
}

tasks {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    // Create a simple JAR without dependencies bundled
    jar {
        manifest {
            attributes("Main-Class" to "cn.mklaus.sqlagent.mcp.Main")
        }
    }

    // Create fat JAR with all dependencies
    shadowJar {
        manifest {
            attributes("Main-Class" to "cn.mklaus.sqlagent.mcp.Main")
        }
        archiveFileName.set("sqlagent-mcp-server.jar")
    }
}
