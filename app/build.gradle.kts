plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("android")
}

android {
    namespace = "id.emiyasyahriel.taikoboard"
    compileSdk = 33

    defaultConfig {
        applicationId = "id.emiyasyahriel.taikoboard"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug"){
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

}

dependencies {
}