plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gradle.node)
}

node {
    version.set("20.11.0")
    download.set(true)
    nodeProjectDir.set(rootProject.file("web-build"))
}

android {
    namespace = "com.defname.localshare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.defname.localshare"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.2.2"

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
        buildConfig = true
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

    sourceSets {
        getByName("main") {
            kotlin.srcDir(layout.buildDirectory.dir("generated/source/iconmap/main/kotlin"))
            assets.srcDir(layout.buildDirectory.dir("generated/assets/iconmap/main"))
            assets.srcDir(layout.buildDirectory.dir("generated/assets/main"))
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
    // implementation(libs.ktor.server.sse)
    // implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.zxing.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.androidx.compose)

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

    val outputDir = layout.buildDirectory.dir("generated/source/iconmap/main/kotlin").get().asFile
    val assetsDir = layout.buildDirectory.dir("generated/assets/iconmap/main").get().asFile
    
    commandLine(
        "/usr/bin/python3", 
        "${rootProject.projectDir}/buildScripts/generate_icon_mapping.py",
        outputDir.absolutePath,
        assetsDir.absolutePath
    )

    outputs.dir(outputDir)
    outputs.dir(assetsDir)

    doFirst {
        println("Generating IconMap to: $outputDir")
        println("Generating Icons to: $assetsDir")
        outputDir.mkdirs()
        assetsDir.mkdirs()
    }
}


val webBuild = tasks.register<com.github.gradle.node.npm.task.NpmTask>("web-build") {
    group = "web-build"
    description = "Generate tailwind css classes."

    args.set(listOf("run", "build"))

    val assetsDir = layout.buildDirectory.dir("generated/assets/main/web/css")
    outputs.dir(assetsDir)

    dependsOn("npmInstall")

    doFirst {
        val dir = assetsDir.get().asFile
        println("Generating tailwindcss to: $dir")
        dir.mkdirs()
    }
}

// add task to Android build process
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateIconMapping)
}

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Assets")) {
        dependsOn(generateIconMapping)
        dependsOn(webBuild)
    }
}

android.applicationVariants.all {
    val variantName = name.replaceFirstChar { it.uppercase() }

    tasks.named("process${variantName}Resources") {
        dependsOn(webBuild)
    }
}

