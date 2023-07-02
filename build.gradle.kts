import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
}

group = "com.chattriggers"
version = "1.0.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("club.minnced:discord-webhooks:0.8.0")
    implementation("io.javalin:javalin:4.5.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.insert-koin:koin-core:3.1.6")
    implementation("mysql:mysql-connector-java:8.0.27")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.1")
    implementation("com.sendgrid:sendgrid-java:4.4.1")
    implementation("com.google.code.gson:gson:2.9.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register<Jar>("uberJar") {
    appendix = "uber"

    manifest {
        attributes["Main-Class"] = "com.chattriggers.website.WebsiteKt"
    }

    duplicatesStrategy = DuplicatesStrategy.WARN

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
