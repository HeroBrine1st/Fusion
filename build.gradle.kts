import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

@Suppress("SpellCheckingInspection")
plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("jvm") version "1.6.10"
    application
}

group = "ru.herobrine1st.fusion"
version = "1.0-SNAPSHOT"
val mainClassName = "ru.herobrine1st.fusion.Fusion"

project.setProperty("mainClassName", mainClassName)

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven("https://jitpack.io/")
}

application {
    mainClass.set(this@Build_gradle.mainClassName)
}
@Suppress("SpellCheckingInspection")
dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // JDA
    val jdaVersion = "5.0.0-alpha.9"
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("com.github.minndevelopment:jda-ktx:9f01b74f4874b765feb1a33a694ee76d755d08fe")

    // slf4j
    val slf4jVersion = "1.7.36"
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("org.slf4j:log4j-over-slf4j:$slf4jVersion")


    // Hibernate ORM
    val hibernateVersion = "5.6.1.Final"
    implementation("org.hibernate:hibernate-core:$hibernateVersion")
    implementation("org.hibernate:hibernate-hikaricp:$hibernateVersion")
    implementation("mysql:mysql-connector-java:8.0.26")

    // Jackson
    val jacksonVersion = "2.13.1"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Other libraries
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.guava:guava:31.1-jre")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
    }
}

tasks {
    javadoc {
        options.encoding = "UTF-8"
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    compileTestJava {
        options.encoding = "UTF-8"
    }
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("full")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}