plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven("https://maven.enginehub.org/repo/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")  // Vault用
    maven ("https://nexus.scarsz.me/content/groups/public/" )
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0")

    // WorldGuard / WorldEdit
    // isTransitive = false で推移皁E��存を全て無効化し、strictly制紁E�E競合を回避する、E    // コンパイルに忁E��なBukkitアダプターとコアAPIので4JARのみを個別持E��する、E    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.14")   { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10")   { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.10")     { isTransitive = false }

    // Citizens & Vault
    // VaultAPI は古ぁEBukkit に依存してぁE��ため、推移皁E��存を無効化して競合を回避
    compileOnly("net.citizensnpcs:citizens-main:2.0.36-SNAPSHOT") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }

    // Database & Cache
    implementation("com.h2database:h2:2.2.224")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // AI Chatbot - Deep Java Library + ONNX Runtime
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.31.1")
    implementation("ai.djl:api:0.31.1")

    // HTTP Client for Google AI Studio API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("io.github.classgraph:classgraph:4.8.184")

    compileOnly("com.discordsrv:discordsrv:1.28.0")
}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")

        // ONNX Runtime native libraries — keep only Linux, drop Windows/macOS
        exclude("ai/onnxruntime/native/win-*/**")
        exclude("ai/onnxruntime/native/osx-*/**")

        relocate("com.zaxxer.hikari", "com.ruskserver.deepwither_V2.libs.hikari")
        relocate("org.h2", "com.ruskserver.deepwither_V2.libs.h2")
        relocate("com.github.benmanes.caffeine", "com.ruskserver.deepwither_V2.libs.caffeine")
        relocate("com.fasterxml.jackson", "com.ruskserver.deepwither_V2.libs.jackson")
        relocate("io.github.classgraph", "com.ruskserver.deepwither_V2.libs.classgraph")
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
