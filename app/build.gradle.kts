plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Backend URL: -PbackendUrl=... or BACKEND_URL env var, else gradle.properties.
val backendUrl: String = (System.getenv("BACKEND_URL")?.takeIf { it.isNotBlank() }
    ?: (project.findProperty("backendUrl") as String?).orEmpty())
    .trim().trimEnd('/')

// Optional release keystore. Without one, the release APK is signed with the
// debug key, which installs fine for a demo but can't go to the Play Store.
val keystorePath: String? = System.getenv("UPISHOP_KEYSTORE_PATH")
val hasReleaseKeystore = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "com.demo.upishop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.demo.upishop"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("UPISHOP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPISHOP_KEY_ALIAS")
                keyPassword = System.getenv("UPISHOP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Adyen Android Components 5.20.0: the UPI Component plus the Compose wrapper.
    // The UPI module brings the await/redirect action handlers with it.
    implementation("com.adyen.checkout:upi:5.20.0")
    implementation("com.adyen.checkout:components-compose:5.20.0")

    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // The Adyen views are Material/AppCompat views, so the Activity and theme use these.
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
