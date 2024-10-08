import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

@Suppress("SpellCheckingInspection")
plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("jvm") version "1.6.10"
    application
    id("app.cash.sqldelight") version "2.0.0-alpha05"
}

group = "ru.herobrine1st.fusion"
version = "1.0-SNAPSHOT"
val mainClassName = "ru.herobrine1st.fusion.FusionKt"

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
    val jdaVersion = "5.0.0-beta.9"
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("club.minnced:jda-ktx:0.11.0-beta.19")
    // slf4j
    val slf4jVersion = "1.7.36"
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("org.slf4j:log4j-over-slf4j:$slf4jVersion")

    // SQLDelight
    implementation("app.cash.sqldelight:runtime:2.0.0-alpha05")
    implementation("app.cash.sqldelight:jdbc-driver:2.0.0-alpha05")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.0-alpha05")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("mysql:mysql-connector-java:8.0.32")

    // Jackson
    val jacksonVersion = "2.14.2"
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

@Suppress("SpellCheckingInspection")
sqldelight {
    databases {
        create("Database") {
            packageName.set("ru.herobrinr1st.fusion.database")
            dialect("app.cash.sqldelight:mysql-dialect:2.0.0-alpha05")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
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