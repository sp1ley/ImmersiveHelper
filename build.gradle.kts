import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.fabricmc.fabric-loom")
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricApiVersion = providers.gradleProperty("fabric_api_version").get()
val geckolibVersion = providers.gradleProperty("geckolib_version").get()
val modVersion = providers.gradleProperty("mod_version").get()
val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val artifactVersion = "$modVersion+$minecraftVersion"

version = artifactVersion
group = providers.gradleProperty("maven_group").get()

base {
    archivesName.set(archivesBaseName)
}

repositories {
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        content {
            includeGroup("com.geckolib")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("immersive_helper") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("com.geckolib:geckolib-fabric-$minecraftVersion:$geckolibVersion")
}

tasks.processResources {
    inputs.property("version", artifactVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to artifactVersion)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Jar>().configureEach {
    destinationDirectory.set(layout.projectDirectory.dir("output"))

    from("LICENSE") {
        rename { "${it}_$archivesBaseName" }
    }
}

tasks.named<Delete>("clean") {
    delete(layout.projectDirectory.dir("output"))
}
