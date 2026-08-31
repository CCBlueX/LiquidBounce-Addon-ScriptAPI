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
    // Lets you test against a locally built client (`./gradlew publishToMavenLocal` in LiquidBounce).
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
    accessWidenerPath = file("src/main/resources/example-addon.accesswidener")
}

// Two things to leave alone here:
//
// 1. There is no `mappings(...)` line. LiquidBounce declares none either, and Loom defaults to
//    Mojang official mappings for this Minecraft version. A different mapping set produces an
//    add-on that compiles and then fails on every Minecraft call.
// 2. Dependencies use plain `implementation`, not `modImplementation`. This Loom version has no
//    remapping step - the development and production namespaces are both Mojang official - so the
//    `mod*` configurations do not exist. LiquidBounce's own build does the same.
dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)

    // The client itself; there is no separate API artifact.
    implementation(libs.liquidbounce)
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
