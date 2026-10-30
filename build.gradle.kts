import org.gradle.api.artifacts.ModuleDependency

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
}

base {
    archivesName = project.property("archives_base_name") as String
    // The Minecraft version the add-on is built for goes into its version, e.g. 1.0.0+26.3
    version = "${project.property("mod_version")}+${libs.versions.minecraft.get()}"
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

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "liquidbounce-scriptapi-gametest"
        enableGameTests = false
    }
}

loom.runs.named("clientGameTest") {
    // Otherwise the client downloads and starts a browser for its web UI, which no test touches.
    environmentVars.put("LB_BROWSER_SKIP", "true")
    environmentVars.put("LB_INTEROP_SKIP", "true")
    systemProperties.put("scriptapi.gametest.examples", file("examples").absolutePath)
    providers.gradleProperty("gametest.only").orNull?.let { systemProperties.put("scriptapi.gametest.only", it) }
}

// The startup and marketplace cases need their scripts in place before the client starts.
val seedGameTestScripts = tasks.register<Sync>("seedGameTestScripts") {
    mustRunAfter("deleteGameTestRunDir")
    from("src/gametest/LiquidBounce")
    into(layout.buildDirectory.dir("run/clientGameTest/LiquidBounce"))
}

tasks.matching { it.name == "runClientGameTest" }.configureEach {
    dependsOn(seedGameTestScripts)
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

    implementation(libs.liquidbounce) {
        // -Pliquidbounce=<version>, used by the definitions workflow for a fresh client release
        providers.gradleProperty("liquidbounce").orNull?.let { version { strictly(it) } }
    }

    // ScriptAPI
    jij(libs.polyglot)
    jij(libs.polyglot.js)
    jij(libs.polyglot.tools)

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

val clientVersions = configurations.compileClasspath.map { classpath ->
    classpath.incoming.resolutionResult.allComponents
        .mapNotNull { it.moduleVersion }
        .associate { "${it.group}:${it.name}" to it.version }
}

tasks.processResources {
    val modVersion = providers.gradleProperty("mod_version").zip(libs.versions.minecraft) { version, minecraft ->
        "$version+$minecraft"
    }
    val minecraftVersion = libs.versions.minecraft
    val loaderVersion = clientVersions.map { it.getValue("net.fabricmc:fabric-loader") }
    val fabricKotlinVersion = clientVersions.map { it.getValue("net.fabricmc:fabric-language-kotlin") }

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
        // LiquidBounce is compiled with preview features, which marks its classes as pre-release
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
