plugins {
    java
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.karatelabs:karate-junit5:1.5.2")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("karate.output.dir", layout.buildDirectory.dir("karate-reports").get().asFile.absolutePath)

    listOf("GATEWAY_URL", "ORG", "SERVICE", "API").forEach { key ->
        System.getenv(key)?.let { environment(key, it) }
    }

    outputs.dir(layout.buildDirectory.dir("karate-reports"))
}
