@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val secretProperties = Properties()
listOf(rootProject.file("secrets.properties"), rootProject.file("local.properties"))
    .filter { it.isFile }
    .forEach { file ->
        file.inputStream().use(secretProperties::load)
    }

fun secretValue(name: String): String {
    return secretProperties.getProperty(name)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(name).orEmpty()
}

fun kotlinString(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}

fun normalizedEndpoint(value: String): String {
    val host = normalizedHost(value)
    return if (host.isBlank()) "" else "https://$host/api/v1"
}

fun normalizedHost(value: String): String {
    val compact = value.trim().replace("\\s+".toRegex(), "")
    if (compact.isBlank()) return ""
    return compact
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .trim('.')
}

fun normalizedTokensUrl(value: String): String {
    val host = normalizedHost(value)
    return if (host.isBlank()) "" else "https://$host/account/tokens"
}

val generatedSecretsDir = layout.buildDirectory.dir("generated/doorDuckSecrets/commonMain/kotlin")
val generateDoorDuckSecrets by tasks.registering {
    inputs.files(rootProject.file("secrets.properties"), rootProject.file("local.properties"))
    outputs.dir(generatedSecretsDir)

    doLast {
        val outputFile = generatedSecretsDir
            .get()
            .file("io/github/vgy789/doorDuck/model/DoorDuckSecrets.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package io.github.vgy789.doorDuck.model

            internal object DoorDuckSecrets {
                const val baseEndpoint = "${kotlinString(normalizedEndpoint(secretValue("CORE_PROGRAM_URL")))}"
                const val rocketTokensUrl = "${kotlinString(normalizedTokensUrl(secretValue("CORE_PROGRAM_URL")))}"
                const val intensiveMskEndpoint = "${kotlinString(normalizedEndpoint(secretValue("INTENSIVE_MSK_URL")))}"
                const val intensiveNskEndpoint = "${kotlinString(normalizedEndpoint(secretValue("INTENSIVE_NSK_URL")))}"
                const val intensiveKznEndpoint = "${kotlinString(normalizedEndpoint(secretValue("INTENSIVE_KZN_URL")))}"
                const val donatePhoneValue = "${kotlinString(secretValue("DONATE_PHONE_VALUE"))}"
                const val donateCardValue = "${kotlinString(secretValue("DONATE_CARD_VALUE"))}"
            }
            """.trimIndent(),
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_3_21`,
                reason = "Work around stale Kotlin/Native cache failures while building Compose iOS frameworks in Xcode",
                issueUrl = URI("https://kotlinlang.org/docs/whatsnew2320.html")
            )
        }
    }

    androidLibrary {
        namespace = "io.github.vgy789.doorDuck.shared"
        compileSdk = 36
        minSdk = 24

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedSecretsDir)
        }
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateDoorDuckSecrets)
}
