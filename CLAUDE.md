# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Type

IntelliJ IDEA plugin (JetBrains Platform Plugin) for macOS input enhancement.

- **Language:** Kotlin 2.0.0 / Java 21
- **Build System:** Gradle 8.14.3
- **Plugin ID:** `com.wonder.freemacinput.FreeMacInput`
- **Target IDE:** IntelliJ IDEA Ultimate 2025.2.5 (build 252)

## Common Commands

```bash
# Run IDE with plugin installed for development
./gradlew runIde

# Build the plugin (assembles and tests)
./gradlew build

# Build plugin ZIP for distribution
./gradlew buildPlugin

# Validate plugin configuration
./gradlew verifyPlugin

# Clean build artifacts
./gradlew clean
```

## Architecture

```
src/main/kotlin/com/wonder/freemacinput/freemacinput/   # Plugin source code
src/main/resources/META-INF/
    plugin.xml                                          # Plugin descriptor
    pluginIcon.svg                                      # Plugin icon
build.gradle.kts                                        # Build configuration
```

The plugin uses the IntelliJ Platform SDK via the `org.jetbrains.intellij` Gradle plugin. Key dependencies:
- `com.intellij.modules.platform` (core platform)
- `Git4Idea` (Git integration)

## Development Workflow

1. Run `./gradlew runIde` to launch IntelliJ IDEA with the plugin installed in a sandbox environment
2. Make code changes in `src/main/kotlin/com.wonder.freemacinput/`
3. The plugin automatically reloads in the running IDE instance
4. Logs are written to `build/idea-sandbox/system/log/idea.log`

## Code Locations

- Plugin entry point: `src/main/kotlin/com/wonder/freemacinput/freemacinput/`
- Plugin descriptor: `src/main/resources/META-INF/plugin.xml`
- Run configuration: `.run/Run IDE with Plugin.run.xml`
