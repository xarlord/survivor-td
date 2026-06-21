plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.github.quillraven.fleks")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.survivortd.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.survivortd.game"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
        }
        create("prod") {
            dimension = "environment"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // === GAME ENGINE ===
    
    // Fleks ECS
    implementation("io.github.quillraven.fleks:fleks:1.5.0")

    // dyn4j — Pure Java 2D Physics (v5.0.2 = first Android-compatible version)
    implementation("org.dyn4j:dyn4j:5.0.2")

    // === JETPACK COMPOSE ===
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Coroutines for game loop
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Audio: Media3 ExoPlayer for BGM
    implementation("androidx.media3:media3-exoplayer:1.5.1")

    // DataStore for save games
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // JSON for level data
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // === TESTING ===

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")

    // Robolectric for JVM-based Android tests
    testImplementation("org.robolectric:robolectric:4.14.1")

    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Debugging
    debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
