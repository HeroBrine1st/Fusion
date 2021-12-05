import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    application
    kotlin("jvm") version "1.5.10"
}

group = "ru.herobrine1st.fusion"
version = "1.0-SNAPSHOT"
val mainClassName = "ru.herobrine1st.fusion.Fusion"

project.setProperty("mainClassName", mainClassName)

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
}

application {
    mainClass.set(this@Build_gradle.mainClassName)
}

dependencies {
    implementation("net.dv8tion:JDA:4.4.0_351")
    implementation("com.github.HeroBrine1st:Fusion-framework:master-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("org.hibernate:hibernate-core:5.6.1.Final")
    implementation("org.hibernate:hibernate-hikaricp:5.6.1.Final")
    implementation("mysql:mysql-connector-java:8.0.26")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
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
