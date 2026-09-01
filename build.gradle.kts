import org.gradle.api.artifacts.ModuleDependency

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
}

base {
    archivesName = project.property("archives_base_name") as String
    version = project.property("mod_version") as String
    group = project.property("maven_group") as String
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "CCBlueX Releases"
        url = uri("https://maven.ccbluex.net/releases")
    }
    maven {
        name = "CCBlueX Snapshots"
        url = uri("https://maven.ccbluex.net/snapshots")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

loom {
    accessWidenerPath = file("src/main/resources/liquidbounce-scriptapi.accesswidener")
}

/**
 * Nests a dependency and everything it pulls in as jar-in-jar, the way the client does it.
 * `include(...)` on its own only takes the named artifact, and GraalVM's dependency graph is deep.
 */
val jij: Configuration = configurations.create("jij").apply {
    // Supplied by Minecraft or fabric-language-kotlin at runtime; bundling them again would
    // shadow the game's own copies.
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "it.unimi.dsi", module = "fastutil")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "com.google.code.gson", module = "gson")
    exclude(group = "org.apache.logging.log4j", module = "log4j-core")
    exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    exclude(group = "org.slf4j", module = "slf4j-api")
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)
    implementation(libs.liquidbounce)

    // ScriptAPI
    jij(libs.polyglot)
    jij(libs.polyglot.js)
    jij(libs.polyglot.tools)

    // Used by ScriptAsyncUtil.request; the client bundles it too, but an add-on cannot rely on
    // another mod's nested jars being on its classpath.
    compileOnly(libs.okhttp)

    // Inline-only extensions the client compiles against as `compileOnlyApi`, so they are not in
    // its POM and have to be declared again here.
    compileOnly(libs.fastutil4k.extensionsOnly)

    testImplementation(kotlin("test"))
}

// Every resolved jij artifact has to reach four places: `compileOnly` to build against, `include`
// to nest in the jar, `testImplementation` because ScriptCommandBuilderTest starts a real polyglot
// Context, and `runtimeOnly` for `runClient` - nested jars are only unpacked from a built mod jar,
// so without it the development run has no GraalVM on its classpath.
run {
    val resolved = jij.incoming.resolutionResult.allDependencies.map { dep ->
        dependencies.create(dep.requested.displayName) {
            (this as? ModuleDependency)?.isTransitive = false
        }
    }

    listOf("compileOnly", "include", "runtimeOnly", "testImplementation").forEach { name ->
        configurations.named(name).configure {
            withDependencies { addAll(resolved) }
        }
    }
}

tasks.processResources {
    val modVersion = providers.gradleProperty("mod_version")
    val minecraftVersion = libs.versions.minecraft
    val loaderVersion = libs.versions.fabric.loader
    val fabricKotlinVersion = libs.versions.fabric.kotlin

    inputs.property("version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)
    inputs.property("fabric_kotlin_version", fabricKotlinVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion.get(),
                "minecraft_version" to minecraftVersion.get(),
                "loader_version" to loaderVersion.get(),
                "fabric_kotlin_version" to fabricKotlinVersion.get(),
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.jdk.get().toInt()
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt())
    }
}

kotlin {
    compilerOptions {
        jvmToolchain(libs.versions.jdk.get().toInt())
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
