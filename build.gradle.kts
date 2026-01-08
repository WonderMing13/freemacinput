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
    
    // 配置runIde任务
    runIde {
        // 设置沙箱目录
        sandboxDirectory.set(file("$buildDir/idea-sandbox"))
        
        // 核心JVM参数：解决编辑器只读和文件写入问题
        jvmArgs("-Didea.test.mode=false")
        jvmArgs("-Didea.openapi.editor.EditorSettingsExternalizable.READ_ONLY=false")
        jvmArgs("-Didea.config.read.only=false")
        jvmArgs("-Didea.workspace.read.only=false")
        // 禁用文件系统只读处理
        jvmArgs("-Didea.filesystem.readonly.handling.mode=disabled")
        // 禁用备份文件创建，解决macOS权限问题
        jvmArgs("-Didea.configuration.store.no.backup=true")
        // 项目和文档级别禁用只读
        jvmArgs("-Didea.openapi.project.Project.ReadOnly=false")
        jvmArgs("-Didea.openapi.editor.Document.ReadOnly=false")
        // 禁用MacOS的com.apple.provenance属性限制
        jvmArgs("-Didea.mac.provenance.disabled=true")
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
