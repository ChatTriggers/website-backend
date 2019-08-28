import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.41"
}

group = "com.chattriggers"
version = "0.0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.javalin:javalin:3.3.0")
    implementation("com.zaxxer:HikariCP:3.3.1")
    implementation("org.koin:koin-core:2.0.1")
    implementation("mysql:mysql-connector-java:8.0.17")
    implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("org.jetbrains.exposed:exposed:0.16.3")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9")
    implementation("com.sendgrid:sendgrid-java:4.4.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}