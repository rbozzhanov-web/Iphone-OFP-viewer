plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.rbozzhanov.ofpviewer"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.rbozzhanov.ofpviewer"
        minSdk = 26
        targetSdk = 35
        // CI sets these from the run number and the release tag; a local build is 1 / 1.0.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    // One key for every build, so each APK installs over the one before it. A
    // fresh debug key per machine would make Android refuse every update with
    // "App not installed". It lives in the repository because this app is
    // sideloaded, not published: it proves only that a build came from this
    // project's sources, which are public anyway. A Play Store release would
    // need a private key kept out of the repository instead.
    signingConfigs {
        create("sideload") {
            storeFile = file("sideload.keystore")
            storePassword = "ofpviewer-sideload"
            keyAlias = "ofpviewer"
            keyPassword = "ofpviewer-sideload"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // The reader itself is the web app's own index.html, copied in at build time
    // from the repository root — never a copy kept here to drift.
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("reader"))
}

val copyReader by tasks.registering(Copy::class) {
    from(rootProject.file("../index.html"), rootProject.file("../icon-192.png"))
    into(layout.buildDirectory.dir("reader/web"))
}
tasks.named("preBuild") { dependsOn(copyReader) }

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
