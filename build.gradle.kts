import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val ktor_version: String by project
val kotlin_version: String = extra["kotlin.version"] as String
val logback_version: String by project

val exposed_version: String by project
val h2_version: String by project
val realmVersion: String by project

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id("io.realm.kotlin") version "1.10.0"
}

group = "com.programmersbox"
version = "1.0-SNAPSHOT"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://repo.spring.io/milestone")
    maven {
        name = "Sonatype Snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.components.splitPane)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    val precompose = "1.5.4"
    implementation("moe.tlaster:precompose:$precompose")
    implementation("moe.tlaster:precompose-viewmodel:$precompose")
    implementation("media.kamel:kamel-image:0.7.2")
    implementation("com.darkrockstudios:mpfilepicker:2.1.0")

    //DiscordBot stuff
    implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cio-jvm:$ktor_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    val kord = "0.10.0"
    implementation(platform("dev.kord:kord-bom:$kord"))
    implementation("dev.kord:kord-core")
    implementation("dev.kord:kord-common")
    implementation("io.realm.kotlin:library-base:$realmVersion")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.8-SNAPSHOT")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    val datastore = "1.1.0-alpha04"
    implementation("androidx.datastore:datastore-core:$datastore")
    implementation("androidx.datastore:datastore-preferences-core:$datastore")

    implementation("com.hexadevlabs:gpt4all-java-binding:1.1.5")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DiscordBotUI"
            packageVersion = "1.0.0"
        }
    }
}
