import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
}

group = "com.chattriggers"
version = "1.0.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0")
    implementation("io.javalin:javalin:4.1.1")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("io.insert-koin:koin-core:3.1.4")
    implementation("mysql:mysql-connector-java:8.0.25")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.sendgrid:sendgrid-java:4.8.0")
    implementation("club.minnced:discord-webhooks:0.7.2")
    implementation("com.google.code.gson:gson:2.8.9")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register<Jar>("uberJar") {
    appendix = "uber"

    manifest {
        attributes["Main-Class"] = "com.chattriggers.website.WebsiteKt"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
