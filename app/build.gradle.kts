plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.defname.localshare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.defname.localshare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true

            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            // Exclude the file causing the conflict
            excludes += "/META-INF/INDEX.LIST"

            // It is very common when using Ktor and Netty to also need these:
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/okio.kotlin_module"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.partialcontent)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.zxing.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val generateIconMapping = tasks.register<Exec>("generateIconMapping") {
    group = "build setup"
    description = "Generate Kotlin file for the icon mapping."

    // path to the script
    commandLine("/usr/bin/python3", "${rootProject.projectDir}/buildScripts/generate_icon_mapping.py")

    // specify working directory
    // workingDir = File("${rootProject.projectDir}    // specify working directory
    // workingDir = File("${rootProject.projectDir}")

    // error handling
    doFirst {
        println("Start generating icon mapping...")
    }
}

// add task to Android build process
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    dependsOn(generateIconMapping)
}
