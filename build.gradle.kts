import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    application
}

group = "ru.herobrine1st.fusion"
version = "1.0-SNAPSHOT"
val mainClassNameFromFuckingRoot = "ru.herobrine1st.fusion.Fusion"

project.setProperty("mainClassName", mainClassNameFromFuckingRoot)

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
}

application {
    // mainClassName is fucking reserved and deprecated
    mainClass.set(mainClassNameFromFuckingRoot)
}

dependencies {
    implementation("net.dv8tion:JDA:4.3.0_339")
    implementation("com.github.HeroBrine1st:Fusion-framework:master-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("org.slf4j:slf4j-simple:1.7.32")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
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
//        mergeServiceFiles()
    }
}
