plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.easecation.bedrockloader"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
}

// 在 KotlinCompile 任务中精确引入主项目的共享源文件
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    source(fileTree("${rootProject.projectDir}/src/main/kotlin") {
        include(
            "net/easecation/bedrockloader/sync/server/**/*.kt",
            "net/easecation/bedrockloader/sync/common/**/*.kt",
            "net/easecation/bedrockloader/loader/BedrockPackRegistry.kt",
            "net/easecation/bedrockloader/loader/PackStructureType.kt",
            "net/easecation/bedrockloader/bedrock/pack/PackManifest.kt",
            "net/easecation/bedrockloader/bedrock/pack/SemVersion.kt",
        )
    })
}

application {
    mainClass.set("net.easecation.bedrockloader.standalone.StandaloneMainKt")
}

dependencies {
    // HTTP Server
    implementation("io.javalin:javalin:6.3.0")

    // YAML Configuration
    implementation("org.yaml:snakeyaml:2.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Kotlin stdlib
    implementation(kotlin("stdlib"))
}

tasks.shadowJar {
    archiveBaseName.set("bedrock-pack-server")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "net.easecation.bedrockloader.standalone.StandaloneMainKt"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
