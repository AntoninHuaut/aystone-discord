plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "fr.maner"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.6.1") {
        exclude(module = "opus-java") // required for encoding audio into opus, not needed if audio is already provided in opus encoding
        exclude(module = "tink") // required for encrypting and decrypting audio
    }
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
}

application {
    mainClass.set("fr.maner.aystonediscord.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("aystone-discord")
}