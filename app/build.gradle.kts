plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.startuplab"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.startuplab"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    // Kotlin の jvmTarget は targetCompatibility の値（17）が使われる
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
