plugins {
    alias(libs.plugins.android.library)
    
}

android {
    namespace = "com.hifzcompanion.notifications"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-ui"))
    implementation(libs.hilt.android)
}



