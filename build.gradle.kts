import java.net.URI
import java.security.MessageDigest

plugins {
    java
}

group = "com.mira"
version = "0.1.6"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val miraCoreVersion = "0.1.0"
val miraCoreSha256 = "a96f1b4cd663666f7c82200b06ebf5d91751d2648a3d9bb3641e0e51bb730c9a"
val miraCoreJar = layout.projectDirectory.file("libs/MiraCore-$miraCoreVersion.jar").asFile

val downloadMiraCore by tasks.registering {
    doLast {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(file.readBytes()).joinToString("") { byte -> "%02x".format(byte) }
        }

        if (miraCoreJar.exists() && sha256(miraCoreJar) == miraCoreSha256) {
            return@doLast
        }

        miraCoreJar.parentFile.mkdirs()
        val url = "https://github.com/FiveSOCE/MIra-core/releases/download/v$miraCoreVersion/MiraCore-$miraCoreVersion.jar"
        URI(url).toURL().openStream().use { input ->
            miraCoreJar.outputStream().use { output -> input.copyTo(output) }
        }

        check(sha256(miraCoreJar) == miraCoreSha256) {
            "Downloaded MiraCore JAR failed SHA-256 verification"
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files(miraCoreJar))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(downloadMiraCore)
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}
