plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "dev.compan.compandroid"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
