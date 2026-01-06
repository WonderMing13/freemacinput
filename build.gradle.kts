plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.wonder.freemacinput"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("252")
            untilBuild.set("253.*")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // JUnit 测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.platform:junit-platform-console-standalone:1.10.0")

    intellijPlatform {
        intellijIdeaUltimate("2025.2.5", useInstaller = false)
    }
}

tasks {
    compileJava {
        options.release.set(17)
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjvm-default=all", "-Xskip-metadata-version-check")
        }
    }
    compileTestJava {
        options.release.set(17)
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjvm-default=all", "-Xskip-metadata-version-check")
        }
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    clean {
        delete(".gradle", "build")
    }
}

// 创建独立的纯逻辑测试任务
tasks.register<JavaExec>("runUnitTests") {
    group = "verification"
    description = "运行纯逻辑单元测试"
    mainClass.set("org.junit.platform.console.ConsoleLauncher")

    classpath = sourceSets["test"].runtimeClasspath

    args = listOf(
        "--select-class=com.wonder.freemacinput.freemacinput.core.ContextDetectorTest",
        "--select-class=com.wonder.freemacinput.freemacinput.core.InputMethodManagerTest",
        "--select-class=com.wonder.freemacinput.freemacinput.core.ContextDetectorLogicTest"
    )

    doFirst {
        println("运行纯逻辑单元测试...")
    }
}

// 禁用默认的 test 任务（因为它需要 IntelliJ 平台）
tasks.named("test") {
    enabled = false
}
