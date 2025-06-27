plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "fr.maner"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.6.1") {
        exclude(module = "opus-java") // required for encoding audio into opus, not needed if audio is already provided in opus encoding
        exclude(module = "tink") // required for encrypting and decrypting audio
    }
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("org.slf4j:slf4j-simple:2.0.17")

    // Database
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("com.zaxxer:HikariCP:6.3.0")
}

application {
    mainClass.set("fr.maner.aystonediscord.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("aystone-discord")
}
