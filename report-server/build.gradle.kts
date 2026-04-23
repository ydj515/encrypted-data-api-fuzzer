plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "report-server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<JavaExec>("publishKarate") {
    group = "reporting"
    description = "Publish Karate reports into REPORT_DATA_DIR without starting the web server"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.reportserver.ReportServerApplication")
    args("publish-karate")
}

tasks.register<JavaExec>("publishCats") {
    group = "reporting"
    description = "Publish CATS reports into REPORT_DATA_DIR without starting the web server"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.reportserver.ReportServerApplication")
    args("publish-cats")
}
