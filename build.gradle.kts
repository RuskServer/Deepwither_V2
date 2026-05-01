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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0")

    // WorldGuard / WorldEdit
    // isTransitive = false で推移的依存を全て無効化し、strictly制約の競合を回避する。
    // コンパイルに必要なBukkitアダプターとコアAPIので4JARのみを個別指定する。
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.14")   { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10")   { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.10")     { isTransitive = false }

    // Database & Cache
    implementation("com.h2database:h2:2.2.224")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("") // -all を取り除き、通常のjarを上書き（FatJar化）する
        relocate("com.zaxxer.hikari", "com.ruskserver.deepwither_V2.libs.hikari")
        relocate("org.h2", "com.ruskserver.deepwither_V2.libs.h2")
        relocate("com.github.benmanes.caffeine", "com.ruskserver.deepwither_V2.libs.caffeine")
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
