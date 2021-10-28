plugins {
    application
}

group = "ru.herobrine1st.fusion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url="https://m2.dv8tion.net/releases")
    maven(url="https://jitpack.io")
}

application {
    mainClass.set("ru.herobrine1st.fusion.Fusion")
}

dependencies {
    implementation("net.dv8tion:JDA:4.3.0_277")
    implementation("com.github.HeroBrine1st:Fusion-framework:master-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("org.slf4j:slf4j-simple:1.7.32")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
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
}
