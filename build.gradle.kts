import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.41"
}

group = "com.chattriggers"
version = "1.0.0"

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
    implementation("com.overzealous:remark:1.1.0")
    implementation("club.minnced:discord-webhooks:0.5.4")
    implementation("com.google.code.gson:gson:2.8.8")
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
