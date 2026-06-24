plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"

dependencies {
    compileOnly(paperApi)
    compileOnly("io.github.isquyet:entropy-core-api:1.0-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }
}
