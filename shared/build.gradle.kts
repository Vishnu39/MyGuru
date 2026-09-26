import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
       namespace = "com.vish.myguru.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            // 1. ANDROID ENGINE GOES HERE
            val ktorVersion = "3.5.1"
            implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
        }
        iosMain.dependencies {
            val ktorVersion = "3.5.1"
            implementation("io.ktor:ktor-client-darwin:${ktorVersion}")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Add Ktor
            val ktorVersion = "3.5.1"
            implementation("io.ktor:ktor-client-core:${ktorVersion}")
            implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            // ADD THIS: The core library that contains 'kotlinx.serialization.json.Json'
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            // Add these explicit icon dependencies
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            // Adds coroutines support across all KMP targets
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            val koinVersion = "4.2.2"

            // Koin Core
            implementation("io.insert-koin:koin-core:$koinVersion")

            // Koin Compose & ViewModel integration
            implementation("io.insert-koin:koin-compose:$koinVersion")
            implementation("io.insert-koin:koin-compose-viewmodel:$koinVersion")
            // Room KMP
            implementation("androidx.room:room-runtime:2.8.4")
            implementation("androidx.sqlite:sqlite-bundled:2.7.0")

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
    arg("room.generateKotlin", "true")
}
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    // This generates the actual database implementation from your annotations
    add("kspCommonMainMetadata", libs.room.compiler)
    // Generates target-specific Room implementations
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}